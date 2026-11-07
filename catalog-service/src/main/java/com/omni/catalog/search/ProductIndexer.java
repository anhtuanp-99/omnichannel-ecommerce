package com.omni.catalog.search;

import com.omni.catalog.product.Product;
import com.omni.catalog.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chuyển Product (Mongo) → ProductSearchDocument (ES).
 * - index(product): upsert 1 doc
 * - deleteById: xóa ES doc
 * - ensureIndex(): tạo index với custom analyzer nếu chưa có
 */
@Service
public class ProductIndexer {

    private static final Logger log = LoggerFactory.getLogger(ProductIndexer.class);

    private final ElasticsearchOperations ops;
    private final ProductRepository productRepo;
    private final boolean useElastic;

    public ProductIndexer(ElasticsearchOperations ops, ProductRepository productRepo,
                          @Value("${app.search.use-elasticsearch:true}") boolean useElastic) {
        this.ops = ops;
        this.productRepo = productRepo;
        this.useElastic = useElastic;
    }

    /** Tạo ES index với settings từ JSON. Gọi ở startup — lazy, không block nếu ES down. */
    public void ensureIndex() {
        if (!useElastic) return;
        try {
            IndexOperations io = ops.indexOps(ProductSearchDocument.class);
            if (!io.exists()) {
                io.createWithMapping();
                log.info("ES index 'products' created with custom analyzer");
            }
        } catch (Exception e) {
            log.warn("Không tạo được ES index (ES down?): {}", e.getMessage());
        }
    }

    public void index(Product p) {
        if (!useElastic) return;
        ops.save(toDoc(p));
    }

    public void deleteById(String id) {
        if (!useElastic) return;
        try {
            ops.delete(id, ProductSearchDocument.class);
        } catch (Exception e) {
            log.warn("ES delete failed id={}: {}", id, e.getMessage());
        }
    }

    public void indexAll(List<Product> products) {
        if (!useElastic) return;
        ops.save(products.stream().map(this::toDoc).toList());
    }

    public ProductSearchDocument toDoc(Product p) {
        ProductSearchDocument d = new ProductSearchDocument();
        d.setId(p.getId());
        d.setName(p.getName());
        d.setDescription(p.getDescription());
        d.setCategoryId(p.getCategoryId());
        d.setBrandId(p.getBrandId());
        d.setStatus(p.getStatus());
        d.setAttributes(p.getAttributes());
        d.setCreatedAt(p.getCreatedAt());
        d.setUpdatedAt(p.getUpdatedAt());

        // Tính min/max price từ SKU active
        var activeSkus = p.getSkus().stream().filter(s -> "ACTIVE".equals(s.getStatus())).toList();
        d.setMinPrice(activeSkus.stream().mapToLong(s -> s.getPrice()).min().orElse(0));
        d.setMaxPrice(activeSkus.stream().mapToLong(s -> s.getPrice()).max().orElse(0));
        d.setCurrency(activeSkus.isEmpty() ? "VND" : activeSkus.get(0).getCurrency());
        d.setSkuCodes(activeSkus.stream().map(s -> s.getCode()).toList());
        d.setPrimaryImageUrl(p.getImages().stream().filter(Product.Image::isPrimary)
                .map(Product.Image::url).findFirst().orElse(null));
        return d;
    }
}