package com.omni.catalog.search;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * ES document — flatten product + SKU để search.
 * Custom analyzer "vi_folding" (lowercase + asciifolding): search "ao thun" match "Áo Thun".
 */
@Document(indexName = "products", createIndex = false)
@Setting(settingPath = "es/product-settings.json")
@Getter @Setter
public class ProductSearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "vi_folding")
    private String name;

    @Field(type = FieldType.Text, analyzer = "vi_folding")
    private String description;

    @Field(type = FieldType.Keyword)
    private String categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String brandId;

    @Field(type = FieldType.Keyword)
    private String brandName;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Long)
    private long minPrice;

    @Field(type = FieldType.Long)
    private long maxPrice;

    @Field(type = FieldType.Keyword)
    private String currency;

    @Field(type = FieldType.Keyword)
    private List<String> skuCodes;

    @Field(type = FieldType.Keyword)
    private String primaryImageUrl;

    @Field(type = FieldType.Object)
    private Map<String, String> attributes;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant updatedAt;
}