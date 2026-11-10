package com.omni.orderinventory.inventory;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.orderinventory.common.OrderInventoryConstants;
import com.omni.orderinventory.inventory.dto.ReservationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lõi xử lý tồn kho - Chống oversell (bán lố) bằng lệnh UPDATE điều kiện (atomic).
 * TẤT CẢ các method đều idempotent (chạy nhiều lần không bị sai kết quả).
 */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final StockRepository stockRepo;
    private final StockReservationRepository reservationRepo;
    private final StockMovementRepository movementRepo;
    private final int ttlMinutes;

    public ReservationService(StockRepository stockRepo,
                              StockReservationRepository reservationRepo,
                              StockMovementRepository movementRepo,
                              @Value("${app.inventory.reservation.ttl-minutes:15}") int ttlMinutes) {
        this.stockRepo = stockRepo;
        this.reservationRepo = reservationRepo;
        this.movementRepo = movementRepo;
        this.ttlMinutes = ttlMinutes;
    }

    /**
     * Giữ hàng cho toàn bộ item (Atomic - được tất cả hoặc không gì cả).
     * Nếu bất kỳ SKU nào thiếu hàng -> Ném lỗi -> @Transactional rollback toàn bộ.
     */
    @Transactional
    public ReservationResult reserveAll(UUID orderId, List<ItemDraft> items) {
        Instant expiresAt = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);
        List<UUID> reservationIds = new ArrayList<>(items.size());

        for (ItemDraft item : items) {
            // Cập nhật kho (Atomic) - Nếu available < quantity thì affected = 0
            int affected = stockRepo.tryReserve(
                    item.skuId(), OrderInventoryConstants.DEFAULT_WAREHOUSE_ID, item.quantity());
            if (affected == 0) {
                throw new InsufficientStockException(item.skuId(), item.quantity());
            }

            // Ghi nhận lượt giữ hàng
            StockReservation res = new StockReservation();
            res.setOrderId(orderId);
            res.setSkuId(item.skuId());
            res.setWarehouseId(OrderInventoryConstants.DEFAULT_WAREHOUSE_ID);
            res.setQuantity(item.quantity());
            res.setStatus(ReservationStatus.PENDING);
            res.setExpiresAt(expiresAt);
            reservationRepo.save(res);

            // Ghi log biến động kho
            StockMovement mv = new StockMovement();
            mv.setSkuId(item.skuId());
            mv.setWarehouseId(OrderInventoryConstants.DEFAULT_WAREHOUSE_ID);
            mv.setQuantity(item.quantity());
            mv.setType(StockMovementType.RESERVE);
            mv.setReferenceType("ORDER");
            mv.setReferenceId(orderId.toString());
            mv.setActor("order-service");
            movementRepo.save(mv);

            reservationIds.add(res.getId());
        }
        return new ReservationResult(reservationIds, expiresAt);
    }

    /**
     * Chốt kho (PENDING -> COMMITTED). Gọi khi thanh toán thành công.
     */
    @Transactional
    public void commitReservation(UUID reservationId) {
        StockReservation res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (res.getStatus() == ReservationStatus.COMMITTED) return;  // Đã xử lý rồi -> Bỏ qua
        if (res.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT,
                    "Lượt giữ hàng ở trạng thái " + res.getStatus() + ", không thể chốt (commit)");
        }

        // Cập nhật trạng thái an toàn (chống race condition)
        int updated = reservationRepo.conditionalStatusUpdate(
                reservationId, ReservationStatus.PENDING, ReservationStatus.COMMITTED);
        if (updated == 0) return;

        int stockUpdated = stockRepo.tryCommit(
                res.getSkuId(), res.getWarehouseId(), res.getQuantity());
        if (stockUpdated == 0) {
            log.error("Chốt kho thất bại - Dữ liệu không nhất quán, reservationId={}", reservationId);
            throw new IllegalStateException("Dữ liệu tồn kho không nhất quán");
        }

        // Ghi log biến động kho
        StockMovement mv = new StockMovement();
        mv.setSkuId(res.getSkuId());
        mv.setWarehouseId(res.getWarehouseId());
        mv.setQuantity(res.getQuantity());
        mv.setType(StockMovementType.COMMIT);
        mv.setReferenceType("ORDER");
        mv.setReferenceId(res.getOrderId().toString());
        mv.setActor("order-service");
        movementRepo.save(mv);
    }

    /**
     * Nhả kho mặc định (PENDING -> RELEASED). Dùng khi khách chủ động hủy đơn.
     */
    @Transactional
    public void releaseReservation(UUID reservationId) {
        releaseReservation(reservationId, ReservationStatus.RELEASED);
    }

    /**
     * Nhả kho có chỉ định trạng thái đích.
     * Dùng khi khách hủy (RELEASED) hoặc hết hạn (EXPIRED).
     */
    @Transactional
    public void releaseReservation(UUID reservationId, ReservationStatus finalStatus) {
        StockReservation res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        // Idempotent: Đã nhả hoặc đã hết hạn thì bỏ qua
        if (res.getStatus() == ReservationStatus.RELEASED || res.getStatus() == ReservationStatus.EXPIRED) {
            return;
        }

        if (res.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT,
                    "Lượt giữ hàng ở trạng thái " + res.getStatus() + ", không thể nhả kho");
        }

        // Cập nhật trạng thái an toàn (chống race condition)
        int updated = reservationRepo.conditionalStatusUpdate(
                reservationId, ReservationStatus.PENDING, finalStatus);
        if (updated == 0) return;  // Process khác đã xử lý xong

        int stockUpdated = stockRepo.tryRelease(
                res.getSkuId(), res.getWarehouseId(), res.getQuantity());
        if (stockUpdated == 0) {
            log.error("Nhả kho thất bại - Dữ liệu không nhất quán, reservationId={}", reservationId);
            throw new IllegalStateException("Dữ liệu tồn kho không nhất quán");
        }

        // Ghi log biến động kho
        StockMovement mv = new StockMovement();
        mv.setSkuId(res.getSkuId());
        mv.setWarehouseId(res.getWarehouseId());
        mv.setQuantity(res.getQuantity());
        mv.setType(StockMovementType.RELEASE);
        mv.setReferenceType("ORDER");
        mv.setReferenceId(res.getOrderId().toString());
        mv.setActor("order-service");
        movementRepo.save(mv);
    }

    /** Item trong 1 order draft — chỉ giữ field cần để reserve. */
    public record ItemDraft(UUID skuId, int quantity) {}
}