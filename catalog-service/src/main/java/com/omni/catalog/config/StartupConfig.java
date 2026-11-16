/**
 * Cấu hình tác vụ chạy ngay sau khi ApplicationContext khởi tạo hoàn tất.
 * Chỉ kiểm tra và khởi tạo Elasticsearch index khi cờ app.search.use-elasticsearch=true.
 */
package com.omni.catalog.config;

import com.omni.catalog.search.ProductIndexer;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.search.use-elasticsearch", havingValue = "true", matchIfMissing = true)
public class StartupConfig {

    private final ProductIndexer indexer;

    public StartupConfig(ProductIndexer indexer) {
        this.indexer = indexer;
    }

    @PostConstruct
    void init() {
        // Kiểm tra và tạo index nếu chưa có; tự nuốt exception nếu ES đang down để không làm sập app
        indexer.ensureIndex();
    }
}