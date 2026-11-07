// config/StartupConfig.java
package com.omni.catalog.config;

import com.omni.catalog.search.ProductIndexer;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupConfig {
    private final ProductIndexer indexer;
    public StartupConfig(ProductIndexer indexer) { this.indexer = indexer; }

    @PostConstruct
    void init() {
        // Lazy — fail không crash app nếu ES down
        indexer.ensureIndex();
    }
}