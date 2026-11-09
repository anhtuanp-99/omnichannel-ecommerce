package com.omni.orderinventory.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    /**
     * Idempotent consumer — INSERT nếu chưa có, trả 0 nếu đã xử lý.
     * Dùng native query để có ON CONFLICT DO NOTHING.
     */
    @Modifying
    @Query(value = """
            INSERT INTO processed_events (event_id, event_type, processed_at)
            VALUES (:eventId, :eventType, now())
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int markProcessed(@Param("eventId") UUID eventId, @Param("eventType") String eventType);
}