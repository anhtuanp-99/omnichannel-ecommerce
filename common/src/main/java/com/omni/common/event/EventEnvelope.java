package com.omni.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope chuẩn cho MỌI event (SRS COM-07).
 * Payload type được generic hóa để service tự định nghĩa schema riêng.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope<T>(
        String eventId,
        String eventType,
        String eventVersion,
        Instant occurredAt,
        String correlationId,
        String producer,
        T payload
) {
    /** Factory cho event version 1. */
    public static <T> EventEnvelope<T> of(String eventType, String producer,
                                          String correlationId, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID().toString(),
                eventType,
                "1",
                Instant.now(),
                correlationId,
                producer,
                payload
        );
    }
}