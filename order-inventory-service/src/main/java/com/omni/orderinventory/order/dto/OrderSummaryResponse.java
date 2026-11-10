package com.omni.orderinventory.order.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id, String code, String status, long totalAmount, String currency,
        Instant createdAt
) {}