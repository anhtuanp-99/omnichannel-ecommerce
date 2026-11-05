package com.omni.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omni.common.error.ErrorCode;
import com.omni.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
            throws IOException {
        res.setStatus(401);
        res.setContentType("application/json;charset=UTF-8");
        var cid = (String) MDC.get("correlationId");
        mapper.writeValue(res.getWriter(),
                ErrorResponse.of(ErrorCode.AUTHENTICATION_REQUIRED, cid == null ? "" : cid));
    }
}