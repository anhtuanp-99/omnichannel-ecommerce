package com.omni.orderinventory.inventory;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;

import java.util.UUID;

public class InsufficientStockException extends BusinessException {
    public InsufficientStockException(UUID skuId, int requested) {
        super(ErrorCode.INVENTORY_INSUFFICIENT,
                "Không đủ tồn kho cho SKU " + skuId + " (cần " + requested + ")");
    }
}