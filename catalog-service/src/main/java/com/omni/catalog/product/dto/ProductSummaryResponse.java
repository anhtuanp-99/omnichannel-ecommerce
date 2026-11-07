package com.omni.catalog.product.dto;
import java.util.List;
public record ProductSummaryResponse(
        String id, String name, String categoryId, String brandId,
        long minPrice, long maxPrice, String currency,
        String primaryImageUrl, List<String> skuCodes
) {}