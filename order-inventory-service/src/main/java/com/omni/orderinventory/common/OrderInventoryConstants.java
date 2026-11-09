package com.omni.orderinventory.common;

import java.util.UUID;

/** Hằng số dùng chung — tránh magic value rải rác. */
public final class OrderInventoryConstants {

    /** v1.0: một kho mặc định. Seed UUID cố định để đồng bộ với seed catalog. */
    public static final UUID DEFAULT_WAREHOUSE_ID =
            UUID.fromString("00000000-0000-0000-0000-00000000ffff");

    public static final int DEFAULT_RESERVATION_TTL_MINUTES = 15;
    public static final int MAX_OUTBOX_RETRY = 5;
    public static final int IDEMPOTENCY_TTL_HOURS = 24;

    private OrderInventoryConstants() {}
}