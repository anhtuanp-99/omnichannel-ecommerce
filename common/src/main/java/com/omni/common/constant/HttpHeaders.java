package com.omni.common.constant;

/** Header dùng xuyên service — tránh typo. */
public final class HttpHeaders {
    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String USER_ID = "X-User-Id";           // do Gateway set sau khi verify JWT
    public static final String USER_ROLES = "X-User-Roles";     // CSV

    private HttpHeaders() {}
}