package com.omni.orderinventory.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Ledger bất biến — append-only. Không update/delete. */
@Entity
@Table(name = "stock_movements")
@Getter @Setter @NoArgsConstructor
public class StockMovement {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "sku_id", nullable = false, columnDefinition = "uuid")
    private UUID skuId;

    @Column(name = "warehouse_id", nullable = false, columnDefinition = "uuid")
    private UUID warehouseId;

    @Column(nullable = false)
    private long quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockMovementType type;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(length = 100)
    private String actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (occurredAt == null) occurredAt = Instant.now();
    }
}