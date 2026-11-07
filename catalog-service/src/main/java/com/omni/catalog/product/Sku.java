package com.omni.catalog.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/** SKU embedded trong Product — không tách collection riêng (1 document = 1 product + N SKU). */
@Getter @Setter @NoArgsConstructor
public class Sku {

    private String id;              // UUID
    private String code;            // unique toàn hệ thống
    private Map<String, String> attributes;  // {size: "M", color: "red"}
    private long price;             // đồng
    private String currency;        // VND
    private String status;          // ACTIVE, INACTIVE
}