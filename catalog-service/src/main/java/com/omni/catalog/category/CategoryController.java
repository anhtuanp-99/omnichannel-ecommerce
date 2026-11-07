package com.omni.catalog.category;

import com.omni.catalog.category.dto.CategoryRequest;
import com.omni.catalog.category.dto.CategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) { this.service = service; }

    @GetMapping
    public List<CategoryResponse> list() { return service.listAll(); }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable String id) { return service.getById(id); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public CategoryResponse create(@Valid @RequestBody CategoryRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public CategoryResponse update(@PathVariable String id, @Valid @RequestBody CategoryRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}