/**
 * Dịch vụ tái lập chỉ mục (Reindex) toàn bộ dữ liệu sản phẩm từ nguồn dữ liệu gốc (MongoDB)
 * sang chỉ mục tìm kiếm dẫn xuất (Elasticsearch) theo yêu cầu DR-013.
 */
package com.omni.catalog.search;

import com.omni.catalog.product.ProductRepository;
import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

@Service
public class ReindexService {

    private static final Logger log = LoggerFactory.getLogger(ReindexService.class);

    private final ElasticsearchOperations ops;
    private final ProductRepository productRepo;
    private final ProductIndexer indexer;
    private final boolean useElastic;

    public ReindexService(
            @Autowired(required = false) ElasticsearchOperations ops,
            ProductRepository productRepo,
            ProductIndexer indexer,
            @Value("${app.search.use-elasticsearch:true}") boolean useElastic) {
        this.ops = ops;
        this.productRepo = productRepo;
        this.indexer = indexer;
        this.useElastic = useElastic;
    }

    /**
     * Xóa index cũ, tạo lại mapping mới và đẩy toàn bộ sản phẩm từ MongoDB sang Elasticsearch.
     */
    public long reindexAll() {
        if (!useElastic || ops == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Tính năng Elasticsearch đang tắt (app.search.use-elasticsearch=false)");
        }

        try {
            IndexOperations io = ops.indexOps(ProductSearchDocument.class);
            if (io.exists()) {
                io.delete();
            }
            io.createWithMapping();

            var products = productRepo.findAll();
            if (!products.isEmpty()) {
                indexer.indexAll(products);
            }
            return products.size();
        } catch (DataAccessException e) {
            log.error("Không thể kết nối tới Elasticsearch để reindex: {}", e.getMessage());
            throw new BusinessException(ErrorCode.UPSTREAM_UNAVAILABLE,
                    "Elasticsearch hiện không khả dụng, vui lòng kiểm tra lại service tìm kiếm");
        }
    }
}