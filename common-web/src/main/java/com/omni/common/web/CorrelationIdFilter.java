package com.omni.common.web;

import com.omni.common.constant.HttpHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Đảm bảo mọi request có correlationId (SRS FR-GW-007, COM-05).
 * - Nếu client/gateway đã set → dùng lại (propagation).
 * - Nếu chưa → sinh UUID mới.
 * - Đặt vào MDC để log pattern %X{correlationId} hiển thị.
 * - Trả về trong response header để client trace.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cid = request.getHeader(HttpHeaders.CORRELATION_ID);
        if (cid == null || cid.isBlank()) {
            cid = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", cid);
        response.setHeader(HttpHeaders.CORRELATION_ID, cid);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}