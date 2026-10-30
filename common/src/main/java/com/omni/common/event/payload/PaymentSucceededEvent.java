package com.omni.common.event.payload;

import java.math.BigInteger;

public record PaymentSucceededEvent(
        String paymentId,
        String orderId,
        BigInteger amount,
        String currency,
        String providerRef
) {}