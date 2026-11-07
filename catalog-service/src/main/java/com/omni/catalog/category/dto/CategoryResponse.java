package com.omni.catalog.category.dto;

import java.time.Instant;

public record CategoryResponse(
        String id, String name, String slug, String parentId,
        String status, Instant createdAt, Instant updatedAt
) {}