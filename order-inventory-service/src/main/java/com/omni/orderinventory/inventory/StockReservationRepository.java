package com.omni.orderinventory.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findByOrderId(UUID orderId);

    /** Job quét reservation hết hạn — chỉ lấy PENDING. */
    @Query("""
            SELECT r FROM StockReservation r
            WHERE r.status = com.omni.orderinventory.inventory.ReservationStatus.PENDING
              AND r.expiresAt < :now
            """)
    List<StockReservation> findExpired(@Param("now") Instant now);

    /**
     * Conditional UPDATE — đảm bảo idempotent dù chạy 2 instance cùng lúc.
     * Trả về rowcount: 0 nếu đã được xử lý trước đó.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StockReservation r
            SET r.status = :newStatus
            WHERE r.id = :id
              AND r.status = :expectedStatus
            """)
    int conditionalStatusUpdate(@Param("id") UUID id,
                                @Param("expectedStatus") ReservationStatus expectedStatus,
                                @Param("newStatus") ReservationStatus newStatus);
}