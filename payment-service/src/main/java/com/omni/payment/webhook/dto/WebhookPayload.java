package com.omni.payment.webhook.dto;

import java.time.Instant;

public record WebhookPayload(
        String eventId,
        String provider,
        String providerTxnId,
        String paymentId,
        String orderId,
        long amount,
        String currency,
        String status,
        String failureCategory,
        Instant occurredAt
) {}