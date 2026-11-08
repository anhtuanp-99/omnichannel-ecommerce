package com.omni.catalog.product;

import com.omni.catalog.product.dto.ProductRequest;
import com.omni.catalog.product.dto.ProductResponse;
import com.omni.catalog.product.dto.SkuLookupResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /** API: Xem chi tiết sản phẩm */
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable String id) {
        return service.getById(id);
    }

    /** API: Xem chi tiết SKU (Dùng nội bộ cho Feign từ Order-service) */
    @GetMapping("/skus/{skuId}")
    public SkuLookupResponse getSku(@PathVariable String skuId) {
        return service.getSkuById(skuId);
    }

    /** API: Thêm mới sản phẩm */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public ProductResponse create(@Valid @RequestBody ProductRequest req) {
        return service.create(req);
    }

    /** API: Cập nhật sản phẩm */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public ProductResponse update(@PathVariable String id, @Valid @RequestBody ProductRequest req) {
        return service.update(id, req);
    }

    /** API: Xóa sản phẩm */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}