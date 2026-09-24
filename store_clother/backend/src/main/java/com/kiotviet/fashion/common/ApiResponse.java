package com.kiotviet.fashion.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wrapper response chuẩn cho toàn bộ API.
 *
 * 💡 Senior Note: @JsonInclude(NON_NULL) để không serialize các field null
 * vào JSON response, giữ payload gọn gàng. VD: khi data = null (lỗi),
 * JSON chỉ có {status, message, timestamp} không có "data": null.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cấu trúc response chuẩn của toàn bộ API")
public class ApiResponse<T> {

    @Schema(description = "HTTP status code", example = "200")
    private int status;

    @Schema(description = "Thông điệp mô tả kết quả", example = "Thành công")
    private String message;

    @Schema(description = "Dữ liệu trả về (null nếu lỗi)")
    private T data;

    @Schema(description = "Thời điểm xử lý request")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ===================== FACTORY METHODS =====================

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message("Thành công")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .status(201)
                .message("Tạo mới thành công")
                .data(data)
                .build();
    }

    public static ApiResponse<Void> noContent(String message) {
        return ApiResponse.<Void>builder()
                .status(200)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .build();
    }
}
