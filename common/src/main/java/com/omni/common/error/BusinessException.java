package com.omni.common.error;

/** Exception base cho lỗi nghiệp vụ — GlobalExceptionHandler sẽ bắt và map. */
public class BusinessException extends RuntimeException {
    private final ErrorCode code;

    public BusinessException(ErrorCode code) {
        super(code.defaultMessage());
        this.code = code;
    }

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() { return code; }
}