package com.omni.common.error;

import org.springframework.http.HttpStatus;

/** Mã lỗi chuẩn — mapping sang HTTP status và message mặc định (SRS Phụ lục B). */
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "Thiếu hoặc token không hợp lệ"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Không có quyền truy cập"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),
    STATE_CONFLICT(HttpStatus.CONFLICT, "Xung đột trạng thái"),
    INVENTORY_INSUFFICIENT(HttpStatus.CONFLICT, "Không đủ tồn kho"),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, "Idempotency-Key đã dùng với payload khác"),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "Vi phạm quy tắc nghiệp vụ"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Vượt giới hạn yêu cầu"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống"),
    UPSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ phụ thuộc không khả dụng"),
    UPSTREAM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "Dịch vụ phụ thuộc timeout");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus httpStatus() { return httpStatus; }
    public String defaultMessage() { return defaultMessage; }
}