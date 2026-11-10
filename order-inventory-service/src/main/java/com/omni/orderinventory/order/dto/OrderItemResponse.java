package com.omni.orderinventory.order.dto;

import java.util.Map;
import java.util.UUID;

public record OrderItemResponse(
        UUID id, UUID skuId, String skuCode, String productName,
        Map<String, String> attributes, long unitPrice, int quantity, long lineTotal
) {}