package com.omni.orderinventory.messaging;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Bảng chống xử lý trùng — PK = eventId. Consumer dùng INSERT ON CONFLICT DO NOTHING. */
@Entity
@Table(name = "processed_events")
@Getter @Setter @NoArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", columnDefinition = "uuid")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    void prePersist() { if (processedAt == null) processedAt = Instant.now(); }
}