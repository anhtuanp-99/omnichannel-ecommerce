package com.omni.catalog.image;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/catalog/admin/images")
public class ImageController {

    private final ImageUploadService service;

    public ImageController(ImageUploadService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CATALOG_STAFF')")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        String url = service.save(file);
        return Map.of("url", url);
    }
}