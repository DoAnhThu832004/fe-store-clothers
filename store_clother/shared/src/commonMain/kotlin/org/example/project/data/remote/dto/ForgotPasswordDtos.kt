package org.example.project.data.remote.dto

/**
 * DTO cho request Quên Mật Khẩu.
 *
 * Backend: POST /api/v1/auth/forgot-password
 * Body: { "email": "...", "storeCode": "..." }
 */
data class ForgotPasswordRequestDto(
    val email: String,
    val storeCode: String
)

/**
 * DTO cho request Đặt Lại Mật Khẩu.
 *
 * Backend: POST /api/v1/auth/reset-password
 * Body: { "token": "...", "newPassword": "...", "confirmPassword": "..." }
 *
 * [token] là chuỗi 43 ký tự Base64URL lấy từ link trong email.
 */
data class ResetPasswordRequestDto(
    val token: String,
    val newPassword: String,
    val confirmPassword: String
)

/**
 * Wrapper lỗi từ backend khi success = false.
 *
 * Dùng [ErrorResponse] và [ErrorDetail] đã có sẵn trong RegisterRequestDto.kt
 * — đây chỉ là type alias để tường minh hơn trong AuthRepository.
 *
 * Backend format (thất bại):
 * {
 *   "success": false,
 *   "error": { "code": "RESET_TOKEN_INVALID", "message": "..." },
 *   "timestamp": "..."
 * }
 *
 * [ApiErrorResponse] là alias name trong context forgot/reset — thực ra
 * cùng cấu trúc với [ErrorResponse] đã định nghĩa trong RegisterRequestDto.kt.
 * AuthRepository dùng [ErrorResponse] cho cả 2 trường hợp.
 */
typealias ApiErrorResponse = ErrorResponse

/** Error code chuẩn backend cho token hết hạn / sai / đã dùng */
const val ERROR_CODE_RESET_TOKEN_INVALID = "RESET_TOKEN_INVALID"
