package com.omni.catalog.search;

import com.omni.catalog.product.dto.ProductSummaryResponse;

import java.util.List;

public record SearchResult(
        List<ProductSummaryResponse> items,
        long total,
        int page,
        int size,
        String source    // "elasticsearch" | "mongo" — để debug
) {}