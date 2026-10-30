package com.omni.common.web;

import com.omni.common.constant.HttpHeaders;
import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý lỗi tập trung cho mọi service servlet.
 * - BusinessException → map ErrorCode → HTTP status
 * - Validation error → 400 với field errors
 * - Exception khác → 500 nhưng KHÔNG leak stack trace (NFR-SEC)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        ErrorCode code = ex.getCode();
        return ResponseEntity.status(code.httpStatus())
                .body(ErrorResponse.of(code, ex.getMessage(), cid(req)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(f -> details.put(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Dữ liệu không hợp lệ",
                        cid(req), details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex, HttpServletRequest req) {
        // Log full stack trace SERVER-SIDE, nhưng KHÔNG trả về client
        log.error("Unhandled exception, cid={}", cid(req), ex);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR,
                        "Đã xảy ra lỗi, vui lòng thử lại sau", cid(req)));
    }

    private String cid(HttpServletRequest req) {
        Object mdc = org.slf4j.MDC.get("correlationId");
        if (mdc != null) return mdc.toString();
        String h = req.getHeader(HttpHeaders.CORRELATION_ID);
        return h != null ? h : "";
    }
}