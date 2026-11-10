package com.omni.orderinventory.inventory.dto;

import java.util.UUID;

public record StockResponse(
        UUID id, UUID skuId, UUID warehouseId,
        long onHand, long reserved, long available, long version
) {}