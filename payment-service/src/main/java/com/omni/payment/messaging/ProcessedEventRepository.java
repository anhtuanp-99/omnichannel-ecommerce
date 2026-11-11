package com.omni.payment.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO processed_events (event_id, event_type, processed_at) VALUES (:eventId, :eventType, now()) ON CONFLICT (event_id) DO NOTHING", nativeQuery = true)
    int markProcessed(@Param("eventId") UUID eventId, @Param("eventType") String eventType);
}