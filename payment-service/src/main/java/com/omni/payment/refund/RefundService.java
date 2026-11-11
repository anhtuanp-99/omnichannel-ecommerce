package com.omni.payment.refund;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.payment.outbox.OutboxEvent;
import com.omni.payment.outbox.OutboxEventRepository;
import com.omni.payment.payment.Payment;
import com.omni.payment.payment.PaymentRepository;
import com.omni.payment.payment.PaymentStatus;
import com.omni.payment.provider.PaymentProvider;
import com.omni.payment.provider.RefundResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final PaymentRepository paymentRepo;
    private final PaymentRefundRepository refundRepo;
    private final OutboxEventRepository outboxRepo;
    private final PaymentProvider provider;

    public RefundService(PaymentRepository paymentRepo, PaymentRefundRepository refundRepo,
                         OutboxEventRepository outboxRepo, PaymentProvider provider) {
        this.paymentRepo = paymentRepo;
        this.refundRepo = refundRepo;
        this.outboxRepo = outboxRepo;
        this.provider = provider;
    }

    @Transactional
    public PaymentRefund refundFullForOrder(UUID orderId, String reason, UUID actor) {
        Payment p = paymentRepo.findByOrderId(orderId).orElseThrow(() ->
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Payment cho đơn hàng: " + orderId));

        if (p.getStatus() != PaymentStatus.SUCCEEDED && p.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT,
                    "Payment ở trạng thái không thể hoàn tiền: " + p.getStatus());
        }

        long remaining = p.getAmount() - p.getRefundedAmount();
        if (remaining <= 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "Đơn hàng đã được hoàn tiền toàn bộ từ trước");
        }

        return doRefund(p, remaining, reason, actor);
    }

    @Transactional
    public PaymentRefund refund(UUID paymentId, long amount, String reason, UUID actor) {
        Payment p = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (p.getStatus() != PaymentStatus.SUCCEEDED && p.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT,
                    "Payment ở trạng thái không thể hoàn tiền: " + p.getStatus());
        }

        long remaining = p.getAmount() - p.getRefundedAmount();
        if (amount <= 0 || amount > remaining) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Số tiền hoàn không hợp lệ (còn lại: " + remaining + ")");
        }

        return doRefund(p, amount, reason, actor);
    }

    private PaymentRefund doRefund(Payment p, long amount, String reason, UUID actor) {
        RefundResult result = provider.refund(p.getProviderRef(), amount, reason);

        PaymentRefund r = new PaymentRefund();
        r.setPaymentId(p.getId());
        r.setAmount(amount);
        r.setReason(reason);
        r.setRequestedBy(actor);

        if (result.success()) {
            r.setStatus(RefundStatus.SUCCEEDED);
            r.setProviderRef(result.providerRef());
            r.setCompletedAt(Instant.now());

            p.setRefundedAmount(p.getRefundedAmount() + amount);
            p.setStatus(p.getRefundedAmount() == p.getAmount()
                    ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
            paymentRepo.save(p);

            emitRefundCompleted(r, p);
            log.info("Hoàn tiền THÀNH CÔNG paymentId={} amount={} status={}", p.getId(), amount, p.getStatus());
        } else {
            r.setStatus(RefundStatus.FAILED);
            log.error("Hoàn tiền THẤT BẠI paymentId={} lý do: {}", p.getId(), result.failureReason());
        }

        return refundRepo.save(r);
    }

    private void emitRefundCompleted(PaymentRefund r, Payment p) {
        OutboxEvent e = new OutboxEvent();
        e.setAggregateType("Refund");
        e.setAggregateId(r.getId());
        e.setEventType("RefundCompleted");
        e.setPayload(Map.of(
                "refundId", r.getId().toString(),
                "paymentId", p.getId().toString(),
                "orderId", p.getOrderId().toString(),
                "amount", r.getAmount()
        ));
        outboxRepo.save(e);
    }
}