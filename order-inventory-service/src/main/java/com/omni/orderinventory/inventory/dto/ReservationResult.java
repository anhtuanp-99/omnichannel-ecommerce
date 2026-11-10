package com.omni.orderinventory.inventory.dto;

import java.util.List;
import java.util.UUID;

public record ReservationResult(List<UUID> reservationIds, java.time.Instant expiresAt) {}