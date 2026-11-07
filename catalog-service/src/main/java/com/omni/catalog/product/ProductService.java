package com.omni.catalog.product;

import com.omni.catalog.category.CategoryRepository;
import com.omni.catalog.brand.BrandRepository;
import com.omni.catalog.product.dto.*;
import com.omni.catalog.search.ProductIndexer;
import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final ProductIndexer indexer;

    public ProductService(ProductRepository productRepo, CategoryRepository categoryRepo,
                          BrandRepository brandRepo, ProductIndexer indexer) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.brandRepo = brandRepo;
        this.indexer = indexer;
    }

    @Cacheable(value = "product", key = "#id")
    public ProductResponse getById(String id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResp(p);
    }

    public ProductResponse create(ProductRequest req) {
        validateRefs(req);
        Product p = new Product();
        apply(p, req);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(p.getCreatedAt());
        Product saved = productRepo.save(p);
        safeIndex(saved);   // dual write — Mongo đã save, ES fail không rollback
        return toResp(saved);
    }

    @CacheEvict(value = "product", key = "#id")
    public ProductResponse update(String id, ProductRequest req) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        validateRefs(req);
        apply(p, req);
        p.setUpdatedAt(Instant.now());
        Product saved = productRepo.save(p);
        safeIndex(saved);
        return toResp(saved);
    }

    @CacheEvict(value = "product", key = "#id")
    public void delete(String id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        productRepo.delete(p);
        indexer.deleteById(id);
    }

    /**
     * Dual write: Mongo đã save → cố index ES. Nếu ES down → log + KHÔNG rollback.
     * Sẽ dùng reindex endpoint để rebuild sau.
     */
    private void safeIndex(Product p) {
        try {
            indexer.index(p);
        } catch (Exception e) {
            log.warn("ES index failed for productId={}, cần reindex sau: {}", p.getId(), e.getMessage());
        }
    }

    private void validateRefs(ProductRequest req) {
        if (!categoryRepo.existsById(req.categoryId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Category không tồn tại");
        }
        if (!brandRepo.existsById(req.brandId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Brand không tồn tại");
        }
        // Check trùng sku code trong request
        Set<String> codes = new HashSet<>();
        for (var s : req.skus()) {
            if (!codes.add(s.code())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trùng SKU code trong request: " + s.code());
            }
        }
    }

    private void apply(Product p, ProductRequest req) {
        p.setName(req.name());
        p.setDescription(req.description());
        p.setCategoryId(req.categoryId());
        p.setBrandId(req.brandId());
        p.setStatus(req.status());
        p.setAttributes(req.attributes() == null ? Map.of() : req.attributes());

        p.setImages(req.images() == null ? List.of()
                : req.images().stream()
                  .map(i -> new Product.Image(i.url(), i.isPrimary()))
                  .toList());

        // SKU: giữ id cũ nếu có, sinh UUID nếu chưa
        List<Sku> skus = new ArrayList<>();
        for (var s : req.skus()) {
            Sku sku = new Sku();
            sku.setId(s.id() != null && !s.id().isBlank() ? s.id() : UUID.randomUUID().toString());
            sku.setCode(s.code());
            sku.setAttributes(s.attributes() == null ? Map.of() : s.attributes());
            sku.setPrice(s.price());
            sku.setCurrency(s.currency());
            sku.setStatus(s.status());
            skus.add(sku);
        }
        p.setSkus(skus);
    }

    private ProductResponse toResp(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getCategoryId(), p.getBrandId(), p.getStatus(),
                p.getImages().stream().map(i -> new ProductResponse.ImageDto(i.url(), i.isPrimary())).toList(),
                p.getAttributes(),
                p.getSkus().stream().map(s -> new SkuResponse(s.getId(), s.getCode(),
                        s.getAttributes(), s.getPrice(), s.getCurrency(), s.getStatus())).toList(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}