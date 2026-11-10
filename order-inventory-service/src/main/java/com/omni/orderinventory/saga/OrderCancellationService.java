package com.omni.orderinventory.saga;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.orderinventory.inventory.ReservationService;
import com.omni.orderinventory.inventory.ReservationStatus;
import com.omni.orderinventory.inventory.StockReservationRepository;
import com.omni.orderinventory.order.*;
import com.omni.orderinventory.outbox.OutboxEvent;
import com.omni.orderinventory.outbox.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** Service xử lý hủy đơn hàng tập trung (Saga Pattern). */
@Service
public class OrderCancellationService {

    private static final Logger log = LoggerFactory.getLogger(OrderCancellationService.class);

    private final OrderRepository orderRepo;
    private final OrderStatusHistoryRepository historyRepo;
    private final StockReservationRepository reservationRepo;
    private final ReservationService reservationService;
    private final OutboxEventRepository outboxRepo;

    public OrderCancellationService(OrderRepository orderRepo,
                                    OrderStatusHistoryRepository historyRepo,
                                    StockReservationRepository reservationRepo,
                                    ReservationService reservationService,
                                    OutboxEventRepository outboxRepo) {
        this.orderRepo = orderRepo;
        this.historyRepo = historyRepo;
        this.reservationRepo = reservationRepo;
        this.reservationService = reservationService;
        this.outboxRepo = outboxRepo;
    }

    /** Hủy đơn hàng mặc định (do khách hàng hoặc nhân viên thao tác) */
    @Transactional
    public void cancel(UUID actorId, UUID orderId, String reason, boolean fromCustomer) {
        cancel(actorId, orderId, reason, fromCustomer, false);
    }

    /** Hủy đơn hàng có phân biệt việc tự động hết hạn (isExpiry = true) */
    @Transactional
    public void cancel(UUID actorId, UUID orderId, String reason, boolean fromCustomer, boolean isExpiry) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (fromCustomer && !order.getCustomerId().equals(actorId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.info("Order={} đã CANCELLED từ trước, bỏ qua", orderId);
            return;
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "Không thể hủy đơn hàng đã hoàn thành");
        }

        OrderStatus oldStatus = order.getStatus();
        OrderStateMachine.assertCanTransition(oldStatus, OrderStatus.CANCELLED);

        // Đơn đã xác nhận hoặc đang vận chuyển thì cần kích hoạt luồng hoàn tiền
        boolean refundRequired = (oldStatus == OrderStatus.CONFIRMED || oldStatus == OrderStatus.SHIPPING);

        // Với đơn PENDING: giải phóng tồn kho đã giữ
        if (oldStatus == OrderStatus.PENDING) {
            var reservations = reservationRepo.findByOrderId(orderId);
            for (var r : reservations) {
                if (r.getStatus() == ReservationStatus.PENDING) {
                    // Nếu là hết hạn, cập nhật status thành EXPIRED. Nếu hủy tay, cập nhật thành RELEASED.
                    ReservationStatus targetStatus = isExpiry ? ReservationStatus.EXPIRED : ReservationStatus.RELEASED;

                    // Lưu ý: Bạn cần đảm bảo hàm releaseReservation trong ReservationService
                    // nhận thêm tham số (UUID id, ReservationStatus finalStatus).
                    // (Hoặc nếu bạn không muốn sửa hàm đó, hãy giữ nguyên và nó sẽ set RELEASED, test sẽ fail).
                    reservationService.releaseReservation(r.getId(), targetStatus);
                }
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        orderRepo.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(OrderStatus.CANCELLED);
        history.setReason(reason);

        // Phân biệt actor là ai
        if (isExpiry) {
            history.setActor("system");
        } else {
            history.setActor(fromCustomer ? "customer:" + actorId : "staff:" + actorId);
        }
        historyRepo.save(history);

        // Bắn Outbox event để các service khác (Payment) xử lý hoàn tiền
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("Order");
        event.setAggregateId(orderId);
        event.setEventType("OrderCancelled");
        event.setPayload(Map.of(
                "orderId", orderId.toString(),
                "reason", reason,
                "refundRequired", refundRequired
        ));
        outboxRepo.save(event);

        log.info("Đã hủy Order={} (oldStatus={}, refundRequired={}, isExpiry={})", orderId, oldStatus, refundRequired, isExpiry);
    }
}