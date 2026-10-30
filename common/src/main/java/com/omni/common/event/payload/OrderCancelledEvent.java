package com.omni.common.event.payload;

public record OrderCancelledEvent(
        String orderId,
        String reason,
        boolean refundRequired
) {}