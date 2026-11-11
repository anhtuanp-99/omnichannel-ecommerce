package com.omni.common.event.payload;

import java.time.Instant;

/** Payload sự kiện OrderCancelled mang thông tin lý do hủy và trạng thái hoàn tiền. */
public record OrderCancelledEvent(
        String orderId,
        String orderCode,
        String customerId,
        String reason,
        boolean refundRequired,
        Instant cancelledAt
) {}