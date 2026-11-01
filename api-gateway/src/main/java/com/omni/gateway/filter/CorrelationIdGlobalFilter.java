package com.omni.gateway.filter;

import com.omni.common.constant.HttpHeaders;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Đảm bảo mọi request qua Gateway có correlationId:
 * - Nếu client đã gửi header -> giữ nguyên.
 * - Nếu chưa có -> tự sinh UUID.
 * - Đẩy header sang downstream microservice và trả về client response header.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String cid = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CORRELATION_ID);
        if (cid == null || cid.isBlank()) {
            cid = UUID.randomUUID().toString();
        }
        final String finalCid = cid;

        // Bổ sung header vào request gửi tới downstream service
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(h -> h.set(HttpHeaders.CORRELATION_ID, finalCid)))
                .build();

        // Gắn header vào response trả về cho client kiểm tra
        mutated.getResponse().getHeaders().set(HttpHeaders.CORRELATION_ID, finalCid);

        MDC.put("correlationId", finalCid);
        return chain.filter(mutated)
                .doFinally(sig -> MDC.clear());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}