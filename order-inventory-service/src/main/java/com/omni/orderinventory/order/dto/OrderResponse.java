package com.omni.orderinventory.order.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OrderResponse(
        UUID id, String code, UUID customerId, String status,
        long subtotal, long discount, long tax, long shippingFee, long totalAmount, String currency,
        Map<String, Object> shippingAddress, Map<String, Object> contact,
        List<OrderItemResponse> items,
        Instant createdAt, Instant updatedAt
) {}