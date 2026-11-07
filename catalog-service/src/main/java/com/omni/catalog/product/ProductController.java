package com.omni.catalog.product;

import com.omni.catalog.product.dto.ProductRequest;
import com.omni.catalog.product.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable String id) { return service.getById(id); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public ProductResponse create(@Valid @RequestBody ProductRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public ProductResponse update(@PathVariable String id, @Valid @RequestBody ProductRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}