package com.omni.orderinventory.saga;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omni.common.constant.KafkaTopics;
import com.omni.common.event.EventEnvelope;
import com.omni.common.event.payload.PaymentFailedEvent;
import com.omni.common.event.payload.PaymentSucceededEvent;
import com.omni.orderinventory.inventory.ReservationService;
import com.omni.orderinventory.inventory.ReservationStatus;
import com.omni.orderinventory.inventory.StockReservation;
import com.omni.orderinventory.inventory.StockReservationRepository;
import com.omni.orderinventory.messaging.ProcessedEventRepository;
import com.omni.orderinventory.order.*;
import com.omni.orderinventory.outbox.OutboxEvent;
import com.omni.orderinventory.outbox.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Consumer xử lý sự kiện kết quả thanh toán từ Payment service (Saga Pattern). */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final ReservationService reservationService;
    private final StockReservationRepository reservationRepo;
    private final OrderRepository orderRepo;
    private final OrderStatusHistoryRepository historyRepo;
    private final OutboxEventRepository outboxRepo;
    private final ProcessedEventRepository processedRepo;
    private final ObjectMapper om;

    public PaymentEventConsumer(ReservationService reservationService,
                                StockReservationRepository reservationRepo,
                                OrderRepository orderRepo,
                                OrderStatusHistoryRepository historyRepo,
                                OutboxEventRepository outboxRepo,
                                ProcessedEventRepository processedRepo,
                                ObjectMapper om) {
        this.reservationService = reservationService;
        this.reservationRepo = reservationRepo;
        this.orderRepo = orderRepo;
        this.historyRepo = historyRepo;
        this.outboxRepo = outboxRepo;
        this.processedRepo = processedRepo;
        this.om = om;
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_EVENTS,
            groupId = "order-inventory",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onPaymentEvent(String message, Acknowledgment ack) {
        try {
            EventEnvelope<?> envelope = om.readValue(message, new TypeReference<EventEnvelope<?>>() {});
            UUID eventId = UUID.fromString(envelope.eventId());

            // 1. Lọc trùng lặp (Idempotency)
            int inserted = processedRepo.markProcessed(eventId, envelope.eventType());
            if (inserted == 0) {
                log.info("Bỏ qua event trùng lặp eventId={} type={}", eventId, envelope.eventType());
                ack.acknowledge();
                return;
            }

            // 2. Phân loại sự kiện
            switch (envelope.eventType()) {
                case "PaymentSucceeded" -> handleSucceeded(envelope, ack);
                case "PaymentFailed" -> handleFailed(envelope, ack);
                default -> {
                    log.warn("Bỏ qua loại event không hỗ trợ: {}", envelope.eventType());
                    ack.acknowledge();
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý payment event: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi xử lý message Kafka", e);
        }
    }

    private void handleSucceeded(EventEnvelope<?> envelope, Acknowledgment ack) {
        PaymentSucceededEvent payload = om.convertValue(envelope.payload(), PaymentSucceededEvent.class);
        UUID orderId = UUID.fromString(payload.orderId());

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy orderId: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("PaymentSucceeded cho đơn đã CANCELLED id={} -> yêu cầu refund", orderId);
            emitCancelledWithRefund(orderId, "Thanh toán thành công sau khi đơn đã bị hủy do quá hạn");
            ack.acknowledge();
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Order={} đang ở status={}, bỏ qua commit", orderId, order.getStatus());
            ack.acknowledge();
            return;
        }

        List<StockReservation> reservations = reservationRepo.findByOrderId(orderId);
        for (var r : reservations) {
            if (r.getStatus() == ReservationStatus.PENDING) {
                reservationService.commitReservation(r.getId());
            } else if (r.getStatus() != ReservationStatus.COMMITTED) {
                log.error("Order={} có reservation={} ở status={} -> yêu cầu refund", orderId, r.getId(), r.getStatus());
                emitCancelledWithRefund(orderId, "Hàng giữ đã bị giải phóng trước khi nhận xác nhận thanh toán");
                ack.acknowledge();
                return;
            }
        }

        OrderStatus oldStatus = order.getStatus();
        OrderStateMachine.assertCanTransition(oldStatus, OrderStatus.CONFIRMED);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepo.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(OrderStatus.CONFIRMED);
        history.setReason("Payment succeeded: " + payload.paymentId());
        history.setActor("system");
        historyRepo.save(history);

        // Bắn event Outbox OrderConfirmed với đầy đủ thông tin cho Notification
        emitEvent(orderId, "OrderConfirmed", Map.of(
                "orderId", orderId.toString(),
                "orderCode", order.getCode(),
                "customerId", order.getCustomerId().toString(),
                "totalAmount", order.getTotalAmount(),
                "currency", order.getCurrency(),
                "confirmedAt", Instant.now().toString()
        ));

        log.info("Order={} đã CONFIRMED", orderId);
        ack.acknowledge();
    }

    private void handleFailed(EventEnvelope<?> envelope, Acknowledgment ack) {
        PaymentFailedEvent payload = om.convertValue(envelope.payload(), PaymentFailedEvent.class);
        UUID orderId = UUID.fromString(payload.orderId());

        Order order = orderRepo.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            ack.acknowledge();
            return;
        }

        for (var r : reservationRepo.findByOrderId(orderId)) {
            if (r.getStatus() == ReservationStatus.PENDING) {
                reservationService.releaseReservation(r.getId(), ReservationStatus.RELEASED);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason("Payment failed: " + payload.failureCategory());
        orderRepo.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOldStatus(OrderStatus.PENDING);
        history.setNewStatus(OrderStatus.CANCELLED);
        history.setReason("Payment failed: " + payload.failureCategory());
        history.setActor("system");
        historyRepo.save(history);

        // Bắn event Outbox OrderCancelled với đầy đủ thông tin cho Notification
        emitEvent(orderId, "OrderCancelled", Map.of(
                "orderId", orderId.toString(),
                "orderCode", order.getCode(),
                "customerId", order.getCustomerId().toString(),
                "reason", "payment failed: " + payload.failureCategory(),
                "refundRequired", false,
                "cancelledAt", Instant.now().toString()
        ));

        log.info("Order={} đã CANCELLED do thanh toán thất bại", orderId);
        ack.acknowledge();
    }

    private void emitCancelledWithRefund(UUID orderId, String reason) {
        Order order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return;

        if (order.getStatus() != OrderStatus.CANCELLED) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelReason(reason);
            orderRepo.save(order);
        }

        emitEvent(orderId, "OrderCancelled", Map.of(
                "orderId", orderId.toString(),
                "orderCode", order.getCode(),
                "customerId", order.getCustomerId().toString(),
                "reason", reason,
                "refundRequired", true,
                "cancelledAt", Instant.now().toString()
        ));
    }

    private void emitEvent(UUID orderId, String type, Map<String, Object> payload) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("Order");
        event.setAggregateId(orderId);
        event.setEventType(type);
        event.setPayload(payload);
        outboxRepo.save(event);
    }
}