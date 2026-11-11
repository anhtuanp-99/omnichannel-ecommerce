package com.omni.payment.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omni.common.constant.KafkaTopics;
import com.omni.common.event.payload.OrderCancelledEvent;
import com.omni.common.event.payload.OrderCreatedEvent;
import com.omni.payment.common.PaymentConstants;
import com.omni.payment.messaging.ProcessedEventRepository;
import com.omni.payment.payment.Payment;
import com.omni.payment.payment.PaymentRepository;
import com.omni.payment.refund.RefundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Consumer lắng nghe sự kiện từ order.events để tự động tạo Payment Intent hoặc hoàn tiền. */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final PaymentRepository paymentRepo;
    private final RefundService refundService;
    private final ProcessedEventRepository processedRepo;
    private final ObjectMapper om;

    public OrderEventConsumer(PaymentRepository paymentRepo,
                              RefundService refundService,
                              ProcessedEventRepository processedRepo,
                              ObjectMapper om) {
        this.paymentRepo = paymentRepo;
        this.refundService = refundService;
        this.processedRepo = processedRepo;
        this.om = om;
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_EVENTS,
            groupId = "payment-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onOrderEvent(String rawMessage, Acknowledgment ack) {
        try {
            JsonNode root = om.readTree(rawMessage);
            String eventType = root.path("eventType").asText();
            UUID eventId = UUID.fromString(root.path("eventId").asText());

            if (processedRepo.markProcessed(eventId, eventType) == 0) {
                log.debug("Bỏ qua event trùng lặp eventId={} type={}", eventId, eventType);
                ack.acknowledge();
                return;
            }

            switch (eventType) {
                case "OrderCreated" -> handleCreated(root.path("payload"));
                case "OrderCancelled" -> handleCancelled(root.path("payload"));
                default -> log.debug("Bỏ qua sự kiện không quan tâm: {}", eventType);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Lỗi khi xử lý event từ order.events: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void handleCreated(JsonNode payloadNode) throws Exception {
        OrderCreatedEvent payload = om.treeToValue(payloadNode, OrderCreatedEvent.class);
        UUID orderId = UUID.fromString(payload.orderId());

        if (paymentRepo.findByOrderId(orderId).isPresent()) {
            log.debug("Payment đã tồn tại cho orderId={}", orderId);
            return;
        }

        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setAmount(payload.totalAmount().longValue());
        p.setCurrency(payload.currency());
        p.setProvider(PaymentConstants.PROVIDER_MOCK);
        paymentRepo.save(p);
        log.info("Đã tự động khởi tạo Payment Intent cho orderId={} amount={}", orderId, p.getAmount());
    }

    private void handleCancelled(JsonNode payloadNode) throws Exception {
        OrderCancelledEvent payload = om.treeToValue(payloadNode, OrderCancelledEvent.class);
        if (!payload.refundRequired()) {
            log.debug("Order={} đã hủy, không yêu cầu hoàn tiền", payload.orderId());
            return;
        }

        UUID orderId = UUID.fromString(payload.orderId());
        try {
            var refund = refundService.refundFullForOrder(orderId, payload.reason(),
                    UUID.fromString("00000000-0000-0000-0000-000000000000"));
            log.info("Đã hoàn tiền tự động refundId={} cho orderId={}", refund.getId(), orderId);
        } catch (Exception e) {
            log.warn("Hoàn tiền tự động cho orderId={} thất bại hoặc bỏ qua: {}", orderId, e.getMessage());
        }
    }
}