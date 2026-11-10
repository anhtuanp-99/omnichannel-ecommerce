package com.omni.orderinventory.inventory.dto;

public record AdjustStockRequest(long delta, String reason) {}