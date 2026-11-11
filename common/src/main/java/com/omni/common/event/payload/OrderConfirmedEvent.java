package com.omni.common.event.payload;

import java.math.BigInteger;
import java.time.Instant;

/** Payload sự kiện OrderConfirmed mang đầy đủ thông tin gửi thông báo. */
public record OrderConfirmedEvent(
        String orderId,
        String orderCode,
        String customerId,
        BigInteger totalAmount,
        String currency,
        Instant confirmedAt
) {}