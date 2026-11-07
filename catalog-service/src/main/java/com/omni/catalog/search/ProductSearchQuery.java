package com.omni.catalog.search;

/** Query object dùng chung cho cả ES và Mongo fallback. */
public record ProductSearchQuery(
        String keyword,
        String categoryId,
        String brandId,
        Long priceMin,
        Long priceMax,
        int page,       // 0-based
        int size,
        String sort     // relevance | price_asc | price_desc | created_desc | updated_desc
) {
    public int safePage() { return Math.max(0, page); }
    public int safeSize() { return Math.min(Math.max(1, size), 100); }
}