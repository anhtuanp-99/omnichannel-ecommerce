/**
 * Cấu hình kích hoạt Spring Data Elasticsearch Repositories có điều kiện.
 * Chỉ nạp bean ProductSearchRepository khi cờ app.search.use-elasticsearch=true,
 * giúp ứng dụng khởi động độc lập hoàn toàn khi tắt Elasticsearch.
 */
package com.omni.catalog.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(name = "app.search.use-elasticsearch", havingValue = "true", matchIfMissing = true)
@EnableElasticsearchRepositories(basePackages = "com.omni.catalog.search")
public class ElasticsearchConfig {
}