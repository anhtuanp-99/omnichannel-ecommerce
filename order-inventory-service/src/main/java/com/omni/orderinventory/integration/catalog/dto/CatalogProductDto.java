package com.omni.orderinventory.integration.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/** Response từ catalog-service GET /products/{id} — chỉ giữ field cần. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogProductDto(
        String id,
        String name,
        String categoryId,
        String brandId,
        String status,
        List<Image> images,
        List<Sku> skus
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Image(String url, boolean isPrimary) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sku(
            String id,
            String code,
            Map<String, String> attributes,
            long price,
            String currency,
            String status
    ) {}
}