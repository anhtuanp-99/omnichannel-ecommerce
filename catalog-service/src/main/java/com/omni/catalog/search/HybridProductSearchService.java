package com.omni.catalog.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Wrapper: try ES → catch exception → fallback Mongo.
 *
 * - Chỉ 1 bean @Primary được inject vào controller.
 * - Nếu config app.search.use-elasticsearch=false → Elastic bean không được tạo → chỉ Mongo.
 */
@Service
@Primary
public class HybridProductSearchService implements ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridProductSearchService.class);

    private final ProductSearchService elastic;
    private final ProductSearchService mongo;
    private final boolean useElastic;

    public HybridProductSearchService(
            // required = false: ES bean có thể vắng mặt nếu bị tắt qua cấu hình
            @Autowired(required = false)
            @Qualifier("elasticProductSearchService")
            ProductSearchService elastic,

            // Mongo bean LUÔN LUÔN có mặt để làm lá chắn fallback cuối cùng
            @Qualifier("mongoProductSearchService")
            ProductSearchService mongo,

            @Value("${app.search.use-elasticsearch:true}") boolean useElastic) {
        this.elastic = elastic;
        this.mongo = mongo;
        this.useElastic = useElastic;
    }

    @Override
    public SearchResult search(ProductSearchQuery query) {
        // Ưu tiên số 1: Elasticsearch (nếu được bật và bean khả dụng)
        if (useElastic && elastic != null) {
            try {
                return elastic.search(query);
            } catch (Exception e) {
                // Khi ES sập hoặc timeout, hệ thống tự động giáng cấp (graceful degradation) sang Mongo
                log.warn("ES search failed, fallback Mongo: {}", e.getMessage());
            }
        }

        // Phương án dự phòng an toàn: Tìm trực tiếp trong Mongo
        return mongo.search(query);
    }
}