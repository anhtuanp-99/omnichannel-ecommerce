package com.omni.catalog.brand;

import com.omni.catalog.brand.dto.BrandRequest;
import com.omni.catalog.brand.dto.BrandResponse;
import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class BrandService {

    private final BrandRepository repo;

    public BrandService(BrandRepository repo) {
        this.repo = repo;
    }

    public List<BrandResponse> listAll() {
        return repo.findAll().stream().map(this::toResp).toList();
    }

    public BrandResponse getById(String id) {
        return repo.findById(id).map(this::toResp)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Brand không tồn tại"));
    }

    public BrandResponse create(BrandRequest req) {
        if (repo.existsBySlug(req.slug())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Slug brand đã tồn tại");
        }
        Brand b = new Brand();
        b.setName(req.name());
        b.setSlug(req.slug());
        b.setLogoUrl(req.logoUrl());
        b.setStatus(req.status());
        b.setCreatedAt(Instant.now());
        b.setUpdatedAt(b.getCreatedAt());
        return toResp(repo.save(b));
    }

    public BrandResponse update(String id, BrandRequest req) {
        Brand b = repo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Brand không tồn tại"));
        b.setName(req.name());
        b.setSlug(req.slug());
        b.setLogoUrl(req.logoUrl());
        b.setStatus(req.status());
        b.setUpdatedAt(Instant.now());
        return toResp(repo.save(b));
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Brand không tồn tại");
        }
        repo.deleteById(id);
    }

    private BrandResponse toResp(Brand b) {
        return new BrandResponse(b.getId(), b.getName(), b.getSlug(), b.getLogoUrl(),
                b.getStatus(), b.getCreatedAt(), b.getUpdatedAt());
    }
}