package com.omni.common.event.payload;

import java.math.BigInteger;

public record RefundCompletedEvent(
        String refundId,
        String paymentId,
        String orderId,
        BigInteger amount
) {}