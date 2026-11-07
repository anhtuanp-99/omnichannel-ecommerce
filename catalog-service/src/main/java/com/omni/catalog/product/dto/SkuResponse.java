package com.omni.catalog.product.dto;
import java.util.Map;
public record SkuResponse(String id, String code, Map<String, String> attributes,
                          long price, String currency, String status) {}