package com.omni.catalog.brand.dto;

import java.time.Instant;

public record BrandResponse(
        String id,
        String name,
        String slug,
        String logoUrl,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}