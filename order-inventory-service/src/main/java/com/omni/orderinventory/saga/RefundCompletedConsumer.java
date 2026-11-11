package com.omni.orderinventory.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omni.common.constant.KafkaTopics;
import com.omni.common.event.payload.RefundCompletedEvent;
import com.omni.orderinventory.common.OrderInventoryConstants;
import com.omni.orderinventory.inventory.StockMovement;
import com.omni.orderinventory.inventory.StockMovementRepository;
import com.omni.orderinventory.inventory.StockMovementType;
import com.omni.orderinventory.inventory.StockRepository;
import com.omni.orderinventory.messaging.ProcessedEventRepository;
import com.omni.orderinventory.order.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Consumer xử lý sự kiện RefundCompleted để hoàn trả tồn kho vật lý (Saga Compensation). */
@Component
public class RefundCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RefundCompletedConsumer.class);

    private final OrderRepository orderRepo;
    private final StockRepository stockRepo;
    private final StockMovementRepository movementRepo;
    private final ProcessedEventRepository processedRepo;
    private final ObjectMapper om;

    public RefundCompletedConsumer(OrderRepository orderRepo,
                                   StockRepository stockRepo,
                                   StockMovementRepository movementRepo,
                                   ProcessedEventRepository processedRepo,
                                   ObjectMapper om) {
        this.orderRepo = orderRepo;
        this.stockRepo = stockRepo;
        this.movementRepo = movementRepo;
        this.processedRepo = processedRepo;
        this.om = om;
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_EVENTS,
            groupId = "order-inventory",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onRefundCompleted(String rawMessage, Acknowledgment ack) {
        try {
            JsonNode root = om.readTree(rawMessage);
            String eventType = root.path("eventType").asText();

            // Chỉ xử lý sự kiện RefundCompleted, bỏ qua các event khác của payment.events
            if (!"RefundCompleted".equals(eventType)) {
                ack.acknowledge();
                return;
            }

            UUID eventId = UUID.fromString(root.path("eventId").asText());

            // Lọc trùng bản tin Idempotent
            if (processedRepo.markProcessed(eventId, eventType) == 0) {
                log.debug("Event RefundCompleted {} đã được xử lý từ trước", eventId);
                ack.acknowledge();
                return;
            }

            RefundCompletedEvent payload = om.treeToValue(root.path("payload"), RefundCompletedEvent.class);
            UUID orderId = UUID.fromString(payload.orderId());

            // Khôi phục hàng vào kho từ danh sách order items
            orderRepo.findById(orderId).ifPresent(order -> {
                order.getItems().forEach(item -> {
                    var stockOpt = stockRepo.findBySkuIdAndWarehouseId(item.getSkuId(), OrderInventoryConstants.DEFAULT_WAREHOUSE_ID);
                    if (stockOpt.isPresent()) {
                        var stock = stockOpt.get();
                        stock.setOnHand(stock.getOnHand() + item.getQuantity());
                        stockRepo.save(stock);

                        StockMovement mv = new StockMovement();
                        mv.setSkuId(item.getSkuId());
                        mv.setWarehouseId(OrderInventoryConstants.DEFAULT_WAREHOUSE_ID);
                        mv.setQuantity(item.getQuantity());
                        mv.setType(StockMovementType.IMPORT);
                        mv.setReferenceType("REFUND");
                        mv.setReferenceId(payload.refundId());
                        mv.setActor("refund-consumer");
                        movementRepo.save(mv);
                    }
                });
                log.info("Đã hoàn kho thành công cho orderId={} sau khi hoàn tiền", orderId);
            });

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Lỗi khi xử lý RefundCompleted event: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}