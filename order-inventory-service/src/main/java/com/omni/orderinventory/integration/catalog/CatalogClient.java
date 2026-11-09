package com.omni.orderinventory.integration.catalog;

import com.omni.orderinventory.integration.catalog.dto.CatalogProductDto;
import com.omni.orderinventory.integration.catalog.dto.CatalogSkuDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Feign client kết nối Catalog-service qua Eureka LoadBalancer. */
@FeignClient(name = "catalog-service", path = "/api/v1/catalog/products")
public interface CatalogClient {

    /** Lấy thông tin Product (Dùng khi cần validate product cha) */
    @GetMapping("/{id}")
    CatalogProductDto getProduct(@PathVariable("id") String id);

    /** Lấy thông tin chi tiết SKU (Dùng khi thêm vào giỏ hàng) */
    @GetMapping("/skus/{skuId}")
    CatalogSkuDto getSku(@PathVariable("skuId") String skuId);
}