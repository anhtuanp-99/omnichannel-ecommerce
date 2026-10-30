package com.omni.common.event.payload;

import java.time.Instant;

public record OrderConfirmedEvent(String orderId, Instant confirmedAt) {}