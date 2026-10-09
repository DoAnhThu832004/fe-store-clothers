package org.example.project.domain.repository

import org.example.project.core.common.Resource
import org.example.project.domain.model.RateLimitException
import org.example.project.domain.model.RegisterTenantParams
import org.example.project.domain.model.RegisterTenantResult
import org.example.project.domain.model.ResetTokenInvalidException
import org.example.project.domain.model.User

/**
 * Interface định nghĩa contract cho Auth Repository theo Clean Architecture.
 * Hoàn toàn thuần Domain (sử dụng Domain Models thay vì DTO).
 */
interface AuthRepository {

    /**
     * Đăng nhập và lưu session.
     * @return [Resource.Success] chứa [User] entity hoặc [Resource.Error]
     */
    suspend fun login(storeCode: String, username: String, password: String): Resource<User>

    /**
     * Đăng ký tenant mới với [RegisterTenantParams].
     */
    suspend fun registerTenant(params: RegisterTenantParams): Resource<RegisterTenantResult>

    /**
     * Gửi email đặt lại mật khẩu.
     */
    suspend fun forgotPassword(email: String, storeCode: String): Resource<String>

    /**
     * Đặt lại mật khẩu bằng token từ Deep Link email.
     */
    suspend fun resetPassword(
        token: String,
        newPassword: String,
        confirmPassword: String
    ): Resource<Unit>

    fun isLoggedIn(): Boolean

    fun getUserRoles(): List<String>

    fun getCurrentUser(): User?

    fun clearSession()
}

/**
 * Typealias để đảm bảo tương thích ngược với code cũ gọi [IAuthRepository].
 */
typealias IAuthRepository = AuthRepository

// Re-export exception để backward compatibility
typealias RateLimitException = org.example.project.domain.model.RateLimitException
typealias ResetTokenInvalidException = org.example.project.domain.model.ResetTokenInvalidException
