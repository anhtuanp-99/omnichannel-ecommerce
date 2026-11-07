package com.omni.catalog.search.dto;

import com.omni.catalog.product.dto.ProductSummaryResponse;
import com.omni.catalog.search.SearchResult;

import java.util.List;

public record SearchResponse(
        List<ProductSummaryResponse> items,
        long total, int page, int size, int totalPages, String source
) {
    public static SearchResponse from(SearchResult r) {
        int totalPages = (int) Math.ceil((double) r.total() / Math.max(1, r.size()));
        return new SearchResponse(r.items(), r.total(), r.page(), r.size(), totalPages, r.source());
    }
}