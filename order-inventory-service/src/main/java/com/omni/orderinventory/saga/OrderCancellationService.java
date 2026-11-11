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

import java.time.Instant;
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

    @Transactional
    public void cancel(UUID actorId, UUID orderId, String reason, boolean fromCustomer) {
        cancel(actorId, orderId, reason, fromCustomer, false);
    }

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

        boolean refundRequired = (oldStatus == OrderStatus.CONFIRMED || oldStatus == OrderStatus.SHIPPING);

        if (oldStatus == OrderStatus.PENDING) {
            var reservations = reservationRepo.findByOrderId(orderId);
            for (var r : reservations) {
                if (r.getStatus() == ReservationStatus.PENDING) {
                    ReservationStatus targetStatus = isExpiry ? ReservationStatus.EXPIRED : ReservationStatus.RELEASED;
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

        if (isExpiry) {
            history.setActor("system");
        } else {
            history.setActor(fromCustomer ? "customer:" + actorId : "staff:" + actorId);
        }
        historyRepo.save(history);

        // Bắn Outbox event mang đầy đủ thông tin mã đơn và thời gian hủy
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("Order");
        event.setAggregateId(orderId);
        event.setEventType("OrderCancelled");
        event.setPayload(Map.of(
                "orderId", orderId.toString(),
                "orderCode", order.getCode(),
                "customerId", order.getCustomerId().toString(),
                "reason", reason,
                "refundRequired", refundRequired,
                "cancelledAt", Instant.now().toString()
        ));
        outboxRepo.save(event);

        log.info("Đã hủy Order={} (oldStatus={}, refundRequired={}, isExpiry={})", orderId, oldStatus, refundRequired, isExpiry);
    }
}