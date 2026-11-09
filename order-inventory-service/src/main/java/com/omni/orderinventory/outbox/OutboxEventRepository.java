package com.omni.orderinventory.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /** Lấy batch sự kiện PENDING để publish — scheduler gọi mỗi 2s. */
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.status = com.omni.orderinventory.outbox.OutboxStatus.PENDING
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findPending(Pageable pageable);
}