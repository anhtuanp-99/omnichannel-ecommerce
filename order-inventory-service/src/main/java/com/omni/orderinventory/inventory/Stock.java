package com.omni.orderinventory.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Stock per (SKU, warehouse). available = onHand - reserved (computed).
 * Surrogate id + UNIQUE(sku_id, warehouse_id) — cần UNIQUE cho atomic UPDATE ở Chat B.
 * @Version để support optimistic locking khi đọc-sửa-ghi (không dùng cho reserve atomic).
 */
@Entity
@Table(name = "stocks",
        uniqueConstraints = @UniqueConstraint(name = "uq_stocks_sku_wh",
                columnNames = {"sku_id", "warehouse_id"}))
@Getter @Setter @NoArgsConstructor
public class Stock {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "sku_id", nullable = false, columnDefinition = "uuid")
    private UUID skuId;

    @Column(name = "warehouse_id", nullable = false, columnDefinition = "uuid")
    private UUID warehouseId;

    @Column(name = "on_hand", nullable = false)
    private long onHand;

    @Column(nullable = false)
    private long reserved;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public long getAvailable() { return onHand - reserved; }
}