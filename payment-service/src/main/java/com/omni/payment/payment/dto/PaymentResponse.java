package com.omni.payment.payment.dto;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id, UUID orderId, long amount, long refundedAmount, String currency,
        String provider, String status, String failureCategory,
        Instant createdAt, Instant updatedAt
) {}