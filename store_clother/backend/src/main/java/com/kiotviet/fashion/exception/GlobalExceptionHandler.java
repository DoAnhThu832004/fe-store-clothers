package com.kiotviet.fashion.exception;

import com.kiotviet.fashion.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler — xử lý tập trung tất cả exception.
 *
 * 💡 Senior Note: @RestControllerAdvice = @ControllerAdvice + @ResponseBody.
 * Mọi exception từ Controller/Service đều được bắt ở đây và map thành ApiResponse chuẩn.
 * Không cần try-catch trong Controller hay Service (trừ business logic cần handle).
 * Đây là nguyên tắc "Centralized Error Handling" — giảm boilerplate, đảm bảo response chuẩn.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * BusinessException → 4xx (tuỳ HttpStatus trong exception).
     * VD: phone trùng → 409, không tìm thấy → 404.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("[BUSINESS] {} - code={}", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getStatus().value(), ex.getMessage()));
    }

    /**
     * ResourceNotFoundException → 404 NOT FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("[NOT FOUND] {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    /**
     * @Valid validation failure → 400 BAD REQUEST.
     * Trả map {field: message} để client biết chính xác field nào lỗi.
     *
     * 💡 Senior Note: Trả validation errors dưới dạng Map<field, message>
     * giúp frontend hiển thị lỗi ngay tại từng input field.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("[VALIDATION] {} errors: {}", errors.size(), errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .status(400)
                        .message("Dữ liệu đầu vào không hợp lệ")
                        .data(errors)
                        .build());
    }

    /**
     * Optimistic Lock failure → 409 CONFLICT.
     * Xảy ra khi 2 user cùng sửa 1 entity (Customer, Supplier, ProductVariant).
     *
     * 💡 Senior Note: KHÔNG để exception này hiện lên client như 500.
     * Client cần biết nguyên nhân (version cũ) để retry đúng cách:
     * GET lại resource → lấy version mới → PUT lại.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex
    ) {
        log.warn("[OPTIMISTIC LOCK] Xung đột version: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409,
                        "Dữ liệu đã bị thay đổi bởi người dùng khác. " +
                        "Vui lòng tải lại trang và thực hiện lại thao tác."));
    }

    /**
     * AccessDeniedException → 403 FORBIDDEN.
     * Spring Security throw khi @PreAuthorize không pass.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("[ACCESS DENIED] {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, "Bạn không có quyền thực hiện thao tác này"));
    }

    /**
     * Pessimistic Lock timeout → 503 SERVICE UNAVAILABLE.
     * Xảy ra khi cancelOrder chờ SELECT FOR UPDATE quá 3000ms.
     *
     * 💡 Senior Note: Trả 503 (Service Unavailable) thay vì 500.
     * 503 có ngữ nghĩa "tạm thời không xử lý được, thử lại sau".
     * Client có thể tự động retry sau vài giây.
     * 500 ngụ ý lỗi server vĩnh viễn → client không retry.
     */
    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleLockTimeout(
            org.springframework.dao.PessimisticLockingFailureException ex
    ) {
        log.warn("[LOCK TIMEOUT] Pessimistic lock timeout: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(503,
                        "Hệ thống đang bận xử lý yêu cầu khác. " +
                        "Vui lòng thử lại sau 5-10 giây."));
    }

    /**
     * Catch-all → 500 INTERNAL SERVER ERROR.
     * Log đầy đủ stack trace để debug, nhưng KHÔNG lộ stack trace cho client.
     *
     * 💡 Senior Note: Tuyệt đối không trả ex.getMessage() hay stack trace
     * cho 500 errors. Thông tin đó có thể lộ cấu trúc DB, thư viện, business logic
     * → security vulnerability. Chỉ log phía server và trả generic message cho client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("[INTERNAL ERROR] Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500,
                        "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau hoặc liên hệ quản trị viên."));
    }
}
