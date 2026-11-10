package com.omni.orderinventory.cart.dto;

import java.util.UUID;

public record CartItemResponse(UUID id, UUID skuId, int quantity) {}