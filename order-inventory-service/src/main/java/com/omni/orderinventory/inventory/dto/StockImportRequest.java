package com.omni.orderinventory.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockImportRequest(
        @NotNull UUID skuId,
        @Min(1) long quantity,
        String reason
) {}