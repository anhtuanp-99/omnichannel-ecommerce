package com.omni.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Format lỗi chuẩn (SRS COM-04, Phụ lục A).
 * details chỉ chứa thông tin an toàn — KHÔNG stack trace, SQL, secret.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        String correlationId,
        Instant timestamp,
        Map<String, Object> details
) {
    public static ErrorResponse of(ErrorCode code, String correlationId) {
        return new ErrorResponse(
                code.name(), code.defaultMessage(),
                correlationId, Instant.now(), null);
    }

    public static ErrorResponse of(ErrorCode code, String message, String correlationId) {
        return new ErrorResponse(code.name(), message, correlationId, Instant.now(), null);
    }

    public static ErrorResponse of(ErrorCode code, String message,
                                   String correlationId, Map<String, Object> details) {
        return new ErrorResponse(code.name(), message, correlationId, Instant.now(), details);
    }
}