package com.omni.catalog.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 5000) String description,
        @NotBlank String categoryId,
        @NotBlank String brandId,
        @NotBlank String status,
        Map<String, String> attributes,
        List<ImageDto> images,
        @NotEmpty @Valid List<SkuDto> skus
) {
    public record ImageDto(@NotBlank String url, boolean isPrimary) {}
    public record SkuDto(
            String id,
            @NotBlank String code,
            Map<String, String> attributes,
            @Min(0) long price,
            @NotBlank String currency,
            @NotBlank String status
    ) {}
}