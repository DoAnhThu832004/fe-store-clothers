package org.example.project.data.remote.dto

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val timestamp: String?
)
data class RefreshTokenRequestDto(
    val refreshToken: String
)

data class AuthDataDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val userInfo: UserInfoDto
)
data class UserInfoDto(
    val id: Long,
    val username: String,
    val fullName: String,
    val email: String,
    val roles: List<String>
)
