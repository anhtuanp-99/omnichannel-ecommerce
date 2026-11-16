/**
 * Điểm khởi chạy chính của Catalog Service.
 * Cấu hình quét các repository MongoDB (loại trừ repository của Elasticsearch),
 * kích hoạt Eureka Discovery Client, Spring Cache (Redis) và GlobalExceptionHandler dùng chung.
 */
package com.omni.catalog;

import com.omni.catalog.search.ProductSearchRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableMongoRepositories(
        basePackages = "com.omni.catalog",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = ProductSearchRepository.class
        )
)
@Import(com.omni.common.web.GlobalExceptionHandler.class)
public class CatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}