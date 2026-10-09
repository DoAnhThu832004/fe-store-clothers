package org.example.project.data.remote.datasource

import org.example.project.core.common.Resource
import org.example.project.data.remote.dto.AuthDataDto
import org.example.project.data.remote.dto.ForgotPasswordRequestDto
import org.example.project.data.remote.dto.LoginRequestDto
import org.example.project.data.remote.dto.RegisterRequestDto
import org.example.project.data.remote.dto.RegisterResponseDto
import org.example.project.data.remote.dto.ResetPasswordRequestDto
import org.example.project.data.remote.dto.UserInfoDto

/**
 * Mock Remote Data Source cung cấp phản hồi mẫu khi chạy preview / desktop / tests không có mạng.
 */
class MockAuthRemoteDataSource : AuthRemoteDataSource {

    override suspend fun login(request: LoginRequestDto): Resource<AuthDataDto> {
        val mockUser = UserInfoDto(
            id = 1L,
            username = request.username,
            fullName = "Người dùng Thử nghiệm",
            email = "demo@storeclother.com",
            roles = listOf("ROLE_OWNER")
        )
        return Resource.Success(
            AuthDataDto(
                accessToken = "mock_access_token_kmp",
                refreshToken = "mock_refresh_token_kmp",
                expiresIn = 86400L,
                userInfo = mockUser
            )
        )
    }

    override suspend fun registerTenant(request: RegisterRequestDto): Resource<RegisterResponseDto> {
        return Resource.Success(
            RegisterResponseDto(
                storeName = request.storeName,
                storeCode = request.storeCode,
                ownerUsername = request.ownerUsername,
                message = "Đăng ký thành công cửa hàng ${request.storeName}"
            )
        )
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequestDto): Resource<String> {
        return Resource.Success("Nếu email tồn tại, link đặt lại mật khẩu đã được gửi.")
    }

    override suspend fun resetPassword(request: ResetPasswordRequestDto): Resource<Unit> {
        return Resource.Success(Unit)
    }

    override fun refreshToken(refreshToken: String): String? {
        return "mock_refreshed_access_token"
    }
}
