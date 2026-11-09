package com.omni.orderinventory.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor
public class OrderItem {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "sku_id", nullable = false, columnDefinition = "uuid")
    private UUID skuId;

    @Column(name = "sku_code", nullable = false, length = 100)
    private String skuCode;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> attributesSnapshot;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false)
    private long lineTotal;

    @PrePersist
    void prePersist() { if (id == null) id = UUID.randomUUID(); }
}