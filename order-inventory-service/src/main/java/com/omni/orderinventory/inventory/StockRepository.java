package com.omni.orderinventory.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findBySkuIdAndWarehouseId(UUID skuId, UUID warehouseId);

    /**
     * Atomic reserve — UPDATE có điều kiện. Trả về 0 nếu available < qty.
     * ĐÂY LÀ CƠ CHẾ CHỐNG OVERSELL DUY NHẤT — không dùng Redis lock.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE stocks
            SET reserved = reserved + :qty,
                version = version + 1,
                updated_at = now()
            WHERE sku_id = :skuId
              AND warehouse_id = :warehouseId
              AND (on_hand - reserved) >= :qty
            """, nativeQuery = true)
    int tryReserve(@Param("skuId") UUID skuId,
                   @Param("warehouseId") UUID warehouseId,
                   @Param("qty") int qty);

    /**
     * Atomic commit — chuyển từ reserved sang đã trừ on_hand.
     * Điều kiện reserved >= qty để chống race nếu có job khác giải phóng nhầm.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE stocks
            SET on_hand = on_hand - :qty,
                reserved = reserved - :qty,
                version = version + 1,
                updated_at = now()
            WHERE sku_id = :skuId
              AND warehouse_id = :warehouseId
              AND reserved >= :qty
            """, nativeQuery = true)
    int tryCommit(@Param("skuId") UUID skuId,
                  @Param("warehouseId") UUID warehouseId,
                  @Param("qty") int qty);

    /** Atomic release — chỉ giảm reserved (on_hand không đổi). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE stocks
            SET reserved = reserved - :qty,
                version = version + 1,
                updated_at = now()
            WHERE sku_id = :skuId
              AND warehouse_id = :warehouseId
              AND reserved >= :qty
            """, nativeQuery = true)
    int tryRelease(@Param("skuId") UUID skuId,
                   @Param("warehouseId") UUID warehouseId,
                   @Param("qty") int qty);
}