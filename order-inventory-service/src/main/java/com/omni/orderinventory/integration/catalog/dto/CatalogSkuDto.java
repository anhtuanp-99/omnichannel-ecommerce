package com.omni.orderinventory.integration.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSkuDto(
        String id,
        String code,
        String productName,
        String productStatus,
        Map<String, String> attributes,
        long price,
        String currency,
        String skuStatus
) {}