package com.omni.catalog.product.dto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
public record ProductResponse(
        String id, String name, String description,
        String categoryId, String brandId, String status,
        List<ImageDto> images, Map<String, String> attributes,
        List<SkuResponse> skus,
        Instant createdAt, Instant updatedAt
) {
    public record ImageDto(String url, boolean isPrimary) {}
}
