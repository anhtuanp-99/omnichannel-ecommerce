package com.omni.payment.refund;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
    List<PaymentRefund> findByPaymentId(UUID paymentId);
}