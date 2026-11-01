package com.omni.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Trích xuất định danh để tính rate limit
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var remote = exchange.getRequest().getRemoteAddress();
            String ip = (remote != null && remote.getAddress() != null)
                    ? remote.getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }
}