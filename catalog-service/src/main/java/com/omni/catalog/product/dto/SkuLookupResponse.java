package com.omni.catalog.product.dto;

import java.util.Map;

/** Snapshot SKU và Product cha để Order-service lưu thông tin đơn hàng. */
public record SkuLookupResponse(
        String id,
        String code,
        String productName,
        String productStatus,
        Map<String, String> attributes,
        long price,
        String currency,
        String skuStatus
) {}