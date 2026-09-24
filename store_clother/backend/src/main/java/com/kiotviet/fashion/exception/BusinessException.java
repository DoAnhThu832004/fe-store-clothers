package com.kiotviet.fashion.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom exception cho business logic errors.
 *
 * 💡 Senior Note: Tách BusinessException khỏi RuntimeException thông thường
 * để GlobalExceptionHandler có thể phân biệt:
 * - BusinessException → HTTP 4xx (lỗi do client/business rule)
 * - Exception → HTTP 500 (lỗi server không mong đợi)
 * Điều này giúp monitoring/alerting phân loại đúng: 4xx không alert oncall,
 * 5xx mới alert team kỹ thuật.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
