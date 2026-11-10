package com.omni.orderinventory.order;

import com.omni.common.constant.HttpHeaders;
import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.orderinventory.order.dto.*;
import com.omni.orderinventory.saga.OrderCancellationService;
import com.omni.orderinventory.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;
    private final OrderCancellationService cancellationService;

    public OrderController(OrderService orderService, OrderCancellationService cancellationService) {
        this.orderService = orderService;
        this.cancellationService = cancellationService;
    }

    /** Tạo đơn hàng mới với Idempotency-Key. */
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            CurrentUser user,
            @RequestHeader(HttpHeaders.IDEMPOTENCY_KEY) String idemKey,
            @Valid @RequestBody CreateOrderRequest req) {
        if (idemKey == null || idemKey.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thiếu Idempotency-Key trên Header");
        }
        return ResponseEntity.accepted().body(orderService.createOrder(user.userId(), idemKey, req));
    }

    /** Lấy chi tiết đơn hàng. */
    @GetMapping("/{id}")
    public OrderResponse get(CurrentUser user, @PathVariable UUID id) {
        return orderService.getOrder(user.userId(), id);
    }

    /** Danh sách đơn hàng cá nhân có phân trang. */
    @GetMapping
    public List<OrderSummaryResponse> list(CurrentUser user,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return orderService.listMyOrders(user.userId(), page, size);
    }

    /** Hủy đơn hàng — hỗ trợ cả PENDING và CONFIRMED (kích hoạt refund). */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(CurrentUser user,
                                       @PathVariable UUID id,
                                       @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Khách hàng tự hủy";
        cancellationService.cancel(user.userId(), id, reason, true);
        return ResponseEntity.noContent().build();
    }
}