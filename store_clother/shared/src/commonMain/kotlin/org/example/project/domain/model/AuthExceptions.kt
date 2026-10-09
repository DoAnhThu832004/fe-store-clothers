package org.example.project.domain.model

/** Thrown khi backend trả HTTP 429 — quá 3 lần gửi email trong 15 phút */
class RateLimitException(
    message: String = "Quá nhiều yêu cầu. Vui lòng thử lại sau 15 phút."
) : Exception(message)

/** Thrown khi backend trả 400 với code RESET_TOKEN_INVALID */
class ResetTokenInvalidException(
    message: String = "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."
) : Exception(message)

/** Generic auth domain exception */
class AuthException(
    message: String,
    val code: Int? = null
) : Exception(message)
