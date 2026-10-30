package com.omni.common.event.payload;

import java.math.BigInteger;
import java.time.Instant;

public record OrderCreatedEvent(
        String orderId,
        String customerId,
        BigInteger totalAmount,     // đơn vị đồng
        String currency,            // VND
        Instant reservationExpiresAt
) {}