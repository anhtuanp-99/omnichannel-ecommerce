package com.omni.orderinventory.order.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID addressId,
        String note
) {}