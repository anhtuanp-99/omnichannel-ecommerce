package com.omni.orderinventory.order;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;

import java.util.Map;
import java.util.Set;

/** Validate transition theo SRS 7.1. Illegal transition → STATE_CONFLICT. */
public final class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.PENDING,    Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED,  Set.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
            OrderStatus.SHIPPING,   Set.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED),
            OrderStatus.COMPLETED,  Set.of(),
            OrderStatus.CANCELLED,  Set.of()
    );

    private OrderStateMachine() {}

    public static void assertCanTransition(OrderStatus from, OrderStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT,
                    "Không thể chuyển từ " + from + " sang " + to);
        }
    }
}