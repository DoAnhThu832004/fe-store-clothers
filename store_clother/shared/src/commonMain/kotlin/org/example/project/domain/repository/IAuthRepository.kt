package org.example.project.domain.repository

import org.example.project.core.common.Resource
import org.example.project.data.remote.dto.RegisterRequestDto
import org.example.project.data.remote.dto.RegisterResponseDto
import org.example.project.data.remote.dto.UserInfoDto

/**
 * Interface định nghĩa contract cho Auth Repository.
 * Đặt ở commonMain để ViewModel và tầng domain có thể depend vào đây
 * mà không cần biết implementation cụ thể (Retrofit, Mock, ...).
 *
 * Implementation thực tế: androidApp/repository/AuthRepository.kt
 */
interface IAuthRepository {

    /**
     * Đăng nhập và lưu session.
     * @return [Resource.Success] chứa [UserInfoDto] hoặc [Resource.Error]
     */
    suspend fun login(storeCode: String, username: String, password: String): Resource<UserInfoDto>

    suspend fun registerTenant(request: RegisterRequestDto): Resource<RegisterResponseDto>

    /**
     * Gửi email đặt lại mật khẩu.
     *
     * Backend luôn trả 200 OK (chống User Enumeration).
     * Chỉ thất bại khi lỗi mạng hoặc 429 Rate Limit (>3 lần/15 phút).
     *
     * @return [Resource.Success] kèm generic message từ server
     *         [Resource.Error] với [RateLimitException] nếu 429
     */
    suspend fun forgotPassword(email: String, storeCode: String): Resource<String>

    /**
     * Đặt lại mật khẩu bằng token từ Deep Link email.
     *
     * [token] là 43 ký tự Base64URL trong URL email.
     * Lỗi [ResetTokenInvalidException] nếu token sai/hết hạn/đã dùng (400 RESET_TOKEN_INVALID).
     *
     * Thành công → backend revoke toàn bộ JWT cũ → mọi request cũ sẽ 401.
     *
     * @return [Resource.Success] khi đặt lại thành công
     *         [Resource.Error] với [ResetTokenInvalidException] nếu token hỏng
     */
    suspend fun resetPassword(
        token: String,
        newPassword: String,
        confirmPassword: String
    ): Resource<Unit>
}

/** Thrown khi backend trả HTTP 429 — quá 3 lần gửi email trong 15 phút */
class RateLimitException(message: String = "Quá nhiều yêu cầu. Vui lòng thử lại sau 15 phút.") : Exception(message)

/** Thrown khi backend trả 400 với code RESET_TOKEN_INVALID */
class ResetTokenInvalidException(message: String = "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.") : Exception(message)
