package com.omni.orderinventory.inventory;

import com.omni.orderinventory.saga.OrderCancellationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Quét các reservation PENDING đã hết hạn.
 * Chuyển giao xử lý cho OrderCancellationService để đảm bảo tính đồng bộ.
 */
@Component
public class ReservationExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final StockReservationRepository reservationRepo;
    private final OrderCancellationService cancellationService;

    public ReservationExpiryJob(StockReservationRepository reservationRepo,
                                OrderCancellationService cancellationService) {
        this.reservationRepo = reservationRepo;
        this.cancellationService = cancellationService;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 15_000)
    @Transactional
    public void sweepExpired() {
        var expired = reservationRepo.findExpired(Instant.now());
        if (expired.isEmpty()) return;

        log.info("Tìm thấy {} lượt giữ hàng đã hết hạn", expired.size());

        for (var r : expired) {
            try {
                // Gọi Service để hủy đơn hàng (truyền cờ isExpiry = true để đánh dấu là hết hạn)
                // Truyền actorId = null vì đây là hệ thống tự động hủy
                cancellationService.cancel(null, r.getOrderId(), "Hết hạn giữ hàng", false, true);
            } catch (Exception e) {
                log.error("Lỗi khi xử lý hết hạn cho reservationId={}", r.getId(), e);
            }
        }
    }
}