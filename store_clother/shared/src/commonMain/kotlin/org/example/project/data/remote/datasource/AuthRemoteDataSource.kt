package org.example.project.data.remote.datasource

import org.example.project.core.common.Resource
import org.example.project.data.remote.dto.AuthDataDto
import org.example.project.data.remote.dto.ForgotPasswordRequestDto
import org.example.project.data.remote.dto.LoginRequestDto
import org.example.project.data.remote.dto.RegisterRequestDto
import org.example.project.data.remote.dto.RegisterResponseDto
import org.example.project.data.remote.dto.ResetPasswordRequestDto

/**
 * Interface cho Remote Data Source xử lý gọi API mạng.
 * Cho phép cắm implementation Retrofit (Android), Ktor (KMP), hoặc Mock (Desktop/Tests).
 */
interface AuthRemoteDataSource {

    suspend fun login(request: LoginRequestDto): Resource<AuthDataDto>

    suspend fun registerTenant(request: RegisterRequestDto): Resource<RegisterResponseDto>

    suspend fun forgotPassword(request: ForgotPasswordRequestDto): Resource<String>

    suspend fun resetPassword(request: ResetPasswordRequestDto): Resource<Unit>

    fun refreshToken(refreshToken: String): String?
}
