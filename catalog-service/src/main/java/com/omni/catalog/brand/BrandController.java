package com.omni.catalog.brand;

import com.omni.catalog.brand.dto.BrandRequest;
import com.omni.catalog.brand.dto.BrandResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/brands")
public class BrandController {

    private final BrandService service;

    public BrandController(BrandService service) {
        this.service = service;
    }

    @GetMapping
    public List<BrandResponse> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public BrandResponse get(@PathVariable String id) {
        return service.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public BrandResponse create(@Valid @RequestBody BrandRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public BrandResponse update(@PathVariable String id, @Valid @RequestBody BrandRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}