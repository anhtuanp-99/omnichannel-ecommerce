package com.omni.payment.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.payment.outbox.OutboxEvent;
import com.omni.payment.outbox.OutboxEventRepository;
import com.omni.payment.payment.Payment;
import com.omni.payment.payment.PaymentRepository;
import com.omni.payment.payment.PaymentStatus;
import com.omni.payment.webhook.dto.WebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookEventRepository webhookRepo;
    private final PaymentRepository paymentRepo;
    private final OutboxEventRepository outboxRepo;
    private final HmacService hmac;
    private final ObjectMapper om;

    public WebhookService(WebhookEventRepository webhookRepo, PaymentRepository paymentRepo,
                          OutboxEventRepository outboxRepo, HmacService hmac, ObjectMapper om) {
        this.webhookRepo = webhookRepo;
        this.paymentRepo = paymentRepo;
        this.outboxRepo = outboxRepo;
        this.hmac = hmac;
        this.om = om;
    }

    @Transactional
    public void handle(String rawBody, String signatureHeader) {
        // 1. Xác thực chữ ký HMAC
        if (!hmac.verify(rawBody, signatureHeader)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED, "Chữ ký HMAC không hợp lệ");
        }

        WebhookPayload payload;
        try {
            payload = om.readValue(rawBody, WebhookPayload.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Payload Webhook không đúng định dạng JSON");
        }

        UUID paymentId = UUID.fromString(payload.paymentId());

        // 2. Chống Replay Attack: kiểm tra providerTxnId
        if (webhookRepo.existsByProviderAndProviderTxnId(payload.provider(), payload.providerTxnId())) {
            log.info("Bỏ qua webhook trùng lặp providerTxnId={}", payload.providerTxnId());
            return;
        }

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Payment ID: " + paymentId));

        // 3. Khớp số tiền và đơn vị tiền tệ
        if (payment.getAmount() != payload.amount() || !payment.getCurrency().equals(payload.currency())) {
            log.error("Sai lệch số tiền thanh toán: mong đợi {} {}, nhận được {} {}",
                    payment.getAmount(), payment.getCurrency(), payload.amount(), payload.currency());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số tiền thanh toán không khớp với đơn hàng");
        }

        // 4. Lưu vết Webhook Event và bắt xung đột race condition
        try {
            WebhookEvent we = new WebhookEvent();
            we.setProvider(payload.provider());
            we.setProviderTxnId(payload.providerTxnId());
            we.setEventId(payload.eventId());
            we.setPaymentId(paymentId);
            we.setRawPayload(om.convertValue(payload, Map.class));
            we.setSignatureValid(true);
            webhookRepo.saveAndFlush(we);
        } catch (DataIntegrityViolationException e) {
            log.info("Webhook đang được xử lý đồng thời txnId={}", payload.providerTxnId());
            return;
        }

        // 5. Cập nhật trạng thái Payment và sinh Outbox Event
        switch (payload.status()) {
            case "SUCCEEDED" -> markSucceeded(payment, payload);
            case "FAILED" -> markFailed(payment, payload);
            case "EXPIRED" -> markExpired(payment);
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trạng thái không hợp lệ: " + payload.status());
        }
    }

    private void markSucceeded(Payment p, WebhookPayload payload) {
        if (p.getStatus() == PaymentStatus.SUCCEEDED) return;
        if (p.getStatus() != PaymentStatus.PENDING) {
            log.warn("Payment={} ở status={}, bỏ qua webhook SUCCEEDED", p.getId(), p.getStatus());
            return;
        }
        p.setStatus(PaymentStatus.SUCCEEDED);
        p.setProviderRef(payload.providerTxnId());
        paymentRepo.save(p);

        emitEvent(p, "PaymentSucceeded", Map.of(
                "paymentId", p.getId().toString(),
                "orderId", p.getOrderId().toString(),
                "amount", p.getAmount(),
                "currency", p.getCurrency(),
                "providerRef", payload.providerTxnId()
        ));
        log.info("Payment={} cập nhật SUCCEEDED", p.getId());
    }

    private void markFailed(Payment p, WebhookPayload payload) {
        if (p.getStatus() == PaymentStatus.FAILED || p.getStatus() != PaymentStatus.PENDING) return;
        p.setStatus(PaymentStatus.FAILED);
        p.setFailureCategory(payload.failureCategory() != null ? payload.failureCategory() : "PAYMENT_DECLINED");
        paymentRepo.save(p);

        emitEvent(p, "PaymentFailed", Map.of(
                "paymentId", p.getId().toString(),
                "orderId", p.getOrderId().toString(),
                "failureCategory", p.getFailureCategory()
        ));
        log.info("Payment={} cập nhật FAILED", p.getId());
    }

    private void markExpired(Payment p) {
        if (p.getStatus() != PaymentStatus.PENDING) return;
        p.setStatus(PaymentStatus.EXPIRED);
        paymentRepo.save(p);

        emitEvent(p, "PaymentFailed", Map.of(
                "paymentId", p.getId().toString(),
                "orderId", p.getOrderId().toString(),
                "failureCategory", "EXPIRED"
        ));
    }

    private void emitEvent(Payment p, String type, Map<String, Object> payload) {
        OutboxEvent e = new OutboxEvent();
        e.setAggregateType("Payment");
        e.setAggregateId(p.getId());
        e.setEventType(type);
        e.setPayload(payload);
        outboxRepo.save(e);
    }
}