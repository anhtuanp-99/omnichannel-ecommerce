package com.omni.catalog.search;

import com.omni.catalog.product.ProductRepository;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

@Service
public class ReindexService {

    private final ElasticsearchOperations ops;
    private final ProductRepository productRepo;
    private final ProductIndexer indexer;

    public ReindexService(ElasticsearchOperations ops, ProductRepository productRepo,
                          ProductIndexer indexer) {
        this.ops = ops;
        this.productRepo = productRepo;
        this.indexer = indexer;
    }

    /** Xóa index cũ + tạo lại + reindex toàn bộ từ Mongo. */
    public long reindexAll() {
        IndexOperations io = ops.indexOps(ProductSearchDocument.class);
        if (io.exists()) io.delete();
        io.createWithMapping();

        var products = productRepo.findAll();
        if (!products.isEmpty()) {
            indexer.indexAll(products);
        }
        return products.size();
    }
}