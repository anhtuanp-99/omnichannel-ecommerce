package com.omni.common.event.payload;

import java.time.Instant;

public record OrderStatusChangedEvent(
        String orderId,
        String oldStatus,
        String newStatus,
        Instant occurredAt
) {}