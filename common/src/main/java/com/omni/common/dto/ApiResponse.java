package com.omni.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Định dạng phản hồi chuẩn cho toàn bộ API trong hệ sinh thái Omni-Ecom.
 *
 * @param <T> Kiểu dữ liệu của payload trả về
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private String status;        // SUCCESS | ERROR
    private String message;       // Thông điệp phản hồi (nếu có)
    private T data;               // Payload dữ liệu
    private String errorCode;     // Mã lỗi nghiệp vụ khi xảy ra lỗi (VD: VALIDATION_ERROR, AUTHENTICATION_REQUIRED)

    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Tạo response thành công kèm dữ liệu payload.
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Tạo response thành công kèm message và dữ liệu payload.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Tạo response lỗi với mã lỗi và thông báo.
     */
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .status("ERROR")
                .errorCode(errorCode)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}