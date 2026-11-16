/**
 * Thành phần chịu trách nhiệm chuyển đổi thực thể Product (MongoDB) thành ProductSearchDocument
 * và đồng bộ dữ liệu (upsert, delete, batch index) sang Elasticsearch.
 * Hỗ trợ cơ chế bỏ qua an toàn khi Elasticsearch bị tắt qua cấu hình hoặc đang gặp sự cố.
 */
package com.omni.catalog.search;

import com.omni.catalog.product.Product;
import com.omni.catalog.product.Sku;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductIndexer {

    private static final Logger log = LoggerFactory.getLogger(ProductIndexer.class);

    private final ElasticsearchOperations ops;
    private final boolean useElastic;

    public ProductIndexer(
            @Autowired(required = false) ElasticsearchOperations ops,
            @Value("${app.search.use-elasticsearch:true}") boolean useElastic) {
        this.ops = ops;
        this.useElastic = useElastic;
    }

    /**
     * Tạo Elasticsearch index kèm custom settings/mapping nếu chưa tồn tại.
     */
    public void ensureIndex() {
        if (!isElasticReady()) return;
        try {
            IndexOperations io = ops.indexOps(ProductSearchDocument.class);
            if (!io.exists()) {
                io.createWithMapping();
                log.info("Elasticsearch index 'products' created with custom analyzer");
            }
        } catch (Exception e) {
            log.warn("Không thể khởi tạo Elasticsearch index (ES có thể đang tắt): {}", e.getMessage());
        }
    }

    /**
     * Đồng bộ (thêm mới hoặc cập nhật) 1 sản phẩm lên Elasticsearch.
     */
    public void index(Product p) {
        if (!isElasticReady()) return;
        ops.save(toDoc(p));
    }

    /**
     * Xóa document sản phẩm khỏi Elasticsearch theo ID.
     */
    public void deleteById(String id) {
        if (!isElasticReady()) return;
        try {
            ops.delete(id, ProductSearchDocument.class);
        } catch (Exception e) {
            log.warn("Xóa document trên Elasticsearch thất bại [id={}]: {}", id, e.getMessage());
        }
    }

    /**
     * Đồng bộ hàng loạt danh sách sản phẩm lên Elasticsearch.
     */
    public void indexAll(List<Product> products) {
        if (!isElasticReady() || products.isEmpty()) return;
        ops.save(products.stream().map(this::toDoc).toList());
    }

    /**
     * Chuyển đổi Product Entity sang ProductSearchDocument, tính toán sẵn khoảng giá từ các SKU ACTIVE.
     */
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

        List<Sku> activeSkus = p.getSkus().stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .toList();

        d.setMinPrice(activeSkus.stream().mapToLong(Sku::getPrice).min().orElse(0));
        d.setMaxPrice(activeSkus.stream().mapToLong(Sku::getPrice).max().orElse(0));
        d.setCurrency(activeSkus.isEmpty() ? "VND" : activeSkus.get(0).getCurrency());
        d.setSkuCodes(activeSkus.stream().map(Sku::getCode).toList());
        d.setPrimaryImageUrl(p.getImages().stream()
                .filter(Product.Image::isPrimary)
                .map(Product.Image::url)
                .findFirst()
                .orElse(null));
        return d;
    }

    private boolean isElasticReady() {
        return useElastic && ops != null;
    }
}