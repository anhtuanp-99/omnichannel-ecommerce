package com.omni.common.event.payload;

public record PaymentFailedEvent(
        String paymentId,
        String orderId,
        String failureCategory    // PAYMENT_DECLINED, EXPIRED, PROVIDER_ERROR...
) {}