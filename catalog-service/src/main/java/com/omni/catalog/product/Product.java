package com.omni.catalog.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
// SKU code unique toàn hệ thống (FR-CAT-006)
@CompoundIndex(name = "sku_code_unique", def = "{'skus.code': 1}", unique = true)
@Getter @Setter @NoArgsConstructor
public class Product {

    @Id
    private String id;

    private String name;
    private String description;

    @Indexed
    private String categoryId;

    @Indexed
    private String brandId;

    @Indexed
    private String status;   // ACTIVE, INACTIVE

    private List<Image> images = new ArrayList<>();
    private Map<String, String> attributes;
    private List<Sku> skus = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    public record Image(String url, boolean isPrimary) {}
}