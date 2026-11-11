package com.omni.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omni.common.constant.KafkaTopics;
import com.omni.common.event.EventEnvelope;
import com.omni.common.event.payload.OrderCancelledEvent;
import com.omni.common.event.payload.OrderConfirmedEvent;
import com.omni.notification.email.EmailSender;
import com.omni.notification.email.TemplateService;
import com.omni.notification.integration.identity.IdentityClient;
import com.omni.notification.integration.identity.dto.UserContactDto;
import com.omni.notification.log.NotificationLog;
import com.omni.notification.log.NotificationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private static final DateTimeFormatter VN_FMT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final IdentityClient identityClient;
    private final TemplateService templateService;
    private final EmailSender emailSender;
    private final NotificationLogService logService;
    private final ObjectMapper om;

    public OrderEventConsumer(IdentityClient identityClient, TemplateService templateService,
                              EmailSender emailSender, NotificationLogService logService,
                              ObjectMapper om) {
        this.identityClient = identityClient;
        this.templateService = templateService;
        this.emailSender = emailSender;
        this.logService = logService;
        this.om = om;
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_EVENTS,
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onEvent(String message, Acknowledgment ack) {
        try {
            EventEnvelope<?> envelope = om.readValue(message, new TypeReference<EventEnvelope<?>>() {});
            switch (envelope.eventType()) {
                case "OrderConfirmed" -> handleConfirmed(envelope);
                case "OrderCancelled" -> handleCancelled(envelope);
                default -> log.debug("Bỏ qua loại event không quan tâm: {}", envelope.eventType());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Lỗi khi xử lý event thông báo: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void handleConfirmed(EventEnvelope<?> envelope) throws Exception {
        OrderConfirmedEvent p = om.convertValue(envelope.payload(), OrderConfirmedEvent.class);

        var existing = logService.findByEventId(envelope.eventId());
        if (existing.isPresent() && "SENT".equals(existing.get().getStatus())) {
            log.debug("Bỏ qua event xác nhận trùng lặp eventId={}", envelope.eventId());
            return;
        }

        UserContactDto contact = identityClient.getContact(p.customerId());
        NotificationLog logEntry = existing.orElseGet(() ->
                logService.createQueued(envelope.eventId(), "OrderConfirmed", contact.email(), "order-confirmed"));

        try {
            Map<String, Object> model = new HashMap<>();
            model.put("customerName", contact.fullName());
            model.put("orderCode", p.orderCode());
            model.put("totalAmount", formatMoney(p.totalAmount().longValue()));
            model.put("currency", p.currency());
            model.put("confirmedAt", VN_FMT.format(p.confirmedAt()));

            String html = templateService.render("order-confirmed", model);
            String messageId = emailSender.sendHtml(contact.email(), "Đơn hàng " + p.orderCode() + " đã được xác nhận", html);

            logService.markSent(logEntry, messageId);
            log.info("Đã gửi email xác nhận cho đơn={}", p.orderCode());
        } catch (Exception e) {
            logService.markFailed(logEntry, e.getMessage());
            throw e;
        }
    }

    private void handleCancelled(EventEnvelope<?> envelope) throws Exception {
        OrderCancelledEvent p = om.convertValue(envelope.payload(), OrderCancelledEvent.class);

        var existing = logService.findByEventId(envelope.eventId());
        if (existing.isPresent() && "SENT".equals(existing.get().getStatus())) {
            log.debug("Bỏ qua event hủy trùng lặp eventId={}", envelope.eventId());
            return;
        }

        UserContactDto contact = identityClient.getContact(p.customerId());
        NotificationLog logEntry = existing.orElseGet(() ->
                logService.createQueued(envelope.eventId(), "OrderCancelled", contact.email(), "order-cancelled"));

        try {
            Map<String, Object> model = new HashMap<>();
            model.put("customerName", contact.fullName());
            model.put("orderCode", p.orderCode());
            model.put("reason", p.reason());
            model.put("refundRequired", p.refundRequired());
            model.put("refundMessage", p.refundRequired()
                    ? "Số tiền đã thanh toán sẽ được hoàn về phương thức ban đầu trong 3–5 ngày làm việc."
                    : "Đơn hàng chưa thanh toán nên không cần hoàn tiền.");
            model.put("cancelledAt", VN_FMT.format(p.cancelledAt()));

            String html = templateService.render("order-cancelled", model);
            String messageId = emailSender.sendHtml(contact.email(), "Đơn hàng " + p.orderCode() + " đã được hủy", html);

            logService.markSent(logEntry, messageId);
            log.info("Đã gửi email hủy cho đơn={}", p.orderCode());
        } catch (Exception e) {
            logService.markFailed(logEntry, e.getMessage());
            throw e;
        }
    }

    private String formatMoney(long vnd) {
        return String.format("%,d", vnd).replace(",", ".");
    }
}