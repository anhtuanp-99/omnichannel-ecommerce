package com.omni.catalog.product;

import com.omni.catalog.brand.BrandRepository;
import com.omni.catalog.category.CategoryRepository;
import com.omni.catalog.product.dto.SkuLookupResponse;
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

    /** Lấy chi tiết sản phẩm theo ID (có cache) */
    @Cacheable(value = "product", key = "#id")
    public ProductResponse getById(String id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResp(p);
    }

    /** Tìm thông tin chi tiết SKU phục vụ Feign call từ Order-service */
    public SkuLookupResponse getSkuById(String skuId) {
        Product product = productRepo.findBySkuId(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy SKU: " + skuId));

        Sku sku = product.getSkus().stream()
                .filter(s -> skuId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SKU không tồn tại"));

        return new SkuLookupResponse(
                sku.getId(),
                sku.getCode(),
                product.getName(),
                product.getStatus(),
                sku.getAttributes(),
                sku.getPrice(),
                sku.getCurrency(),
                sku.getStatus()
        );
    }

    /** Tạo mới sản phẩm và đồng bộ sang ES */
    public ProductResponse create(ProductRequest req) {
        validateRefs(req);
        Product p = new Product();
        apply(p, req);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(p.getCreatedAt());
        Product saved = productRepo.save(p);
        safeIndex(saved);
        return toResp(saved);
    }

    /** Cập nhật sản phẩm, xóa cache cũ và đồng bộ lại ES */
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

    /** Xóa sản phẩm khỏi DB, Cache và ES */
    @CacheEvict(value = "product", key = "#id")
    public void delete(String id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        productRepo.delete(p);
        indexer.deleteById(id);
    }

    /** Dual write: Mongo đã save → cố index ES. Nếu ES down → log + KHÔNG rollback. */
    private void safeIndex(Product p) {
        try {
            indexer.index(p);
        } catch (Exception e) {
            log.warn("ES index failed for productId={}, cần reindex sau: {}", p.getId(), e.getMessage());
        }
    }

    /** Validate các ràng buộc danh mục, thương hiệu và trùng lặp SKU */
    private void validateRefs(ProductRequest req) {
        if (!categoryRepo.existsById(req.categoryId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Category không tồn tại");
        }
        if (!brandRepo.existsById(req.brandId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Brand không tồn tại");
        }

        Set<String> codes = new HashSet<>();
        for (var s : req.skus()) {
            if (!codes.add(s.code())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trùng SKU code trong request: " + s.code());
            }
        }
    }

    /** Copy dữ liệu từ Request DTO sang Entity */
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

    /** Map Entity sang Response DTO */
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