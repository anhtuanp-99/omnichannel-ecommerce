package com.omni.orderinventory.integration.catalog;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.orderinventory.integration.catalog.dto.CatalogProductDto;
import com.omni.orderinventory.integration.catalog.dto.CatalogSkuDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Feign client kết nối Catalog-service qua Eureka LoadBalancer kèm Circuit Breaker. */
@FeignClient(name = "catalog-service", path = "/api/v1/catalog/products")
public interface CatalogClient {

    @GetMapping("/{id}")
    @CircuitBreaker(name = "catalogClient", fallbackMethod = "getProductFallback")
    CatalogProductDto getProduct(@PathVariable("id") String id);

    @GetMapping("/skus/{skuId}")
    @CircuitBreaker(name = "catalogClient", fallbackMethod = "getSkuFallback")
    CatalogSkuDto getSku(@PathVariable("skuId") String skuId);

    default CatalogProductDto getProductFallback(String id, Throwable t) {
        throw new BusinessException(ErrorCode.UPSTREAM_UNAVAILABLE, "Catalog service tạm thời không khả dụng");
    }

    default CatalogSkuDto getSkuFallback(String skuId, Throwable t) {
        throw new BusinessException(ErrorCode.UPSTREAM_UNAVAILABLE, "Catalog service tạm thời không khả dụng");
    }
}