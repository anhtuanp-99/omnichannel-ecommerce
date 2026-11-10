package com.omni.orderinventory.cart.dto;

import java.util.List;
import java.util.UUID;

public record CartResponse(UUID id, UUID customerId, List<CartItemResponse> items) {}