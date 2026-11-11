package com.omni.payment.payment;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.payment.payment.dto.PayResponse;
import com.omni.payment.payment.dto.PaymentResponse;
import com.omni.payment.provider.PaymentInitResult;
import com.omni.payment.provider.PaymentProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository repo;
    private final PaymentProvider provider;

    public PaymentService(PaymentRepository repo, PaymentProvider provider) {
        this.repo = repo;
        this.provider = provider;
    }

    public PaymentResponse get(UUID id) {
        Payment p = repo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResp(p);
    }

    public PaymentResponse getByOrderId(UUID orderId) {
        Payment p = repo.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResp(p);
    }

    @Transactional
    public PayResponse initiatePayment(UUID paymentId) {
        Payment p = repo.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (p.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT,
                    "Payment ở trạng thái " + p.getStatus() + ", không thể thực hiện thanh toán");
        }

        if (p.getProviderRef() != null) {
            PaymentInitResult r = provider.createPayment(p.getId().toString(), p.getAmount(), p.getCurrency());
            return new PayResponse(p.getId().toString(), r.paymentUrl());
        }

        PaymentInitResult result = provider.createPayment(p.getId().toString(), p.getAmount(), p.getCurrency());
        p.setProviderRef(result.providerRef());
        repo.save(p);

        return new PayResponse(p.getId().toString(), result.paymentUrl());
    }

    private PaymentResponse toResp(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(),
                p.getRefundedAmount(), p.getCurrency(), p.getProvider(), p.getStatus().name(),
                p.getFailureCategory(), p.getCreatedAt(), p.getUpdatedAt());
    }
}