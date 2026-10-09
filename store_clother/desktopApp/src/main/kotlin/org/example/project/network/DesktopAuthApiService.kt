package org.example.project.network

import org.example.project.data.remote.dto.ApiResponse
import org.example.project.data.remote.dto.AuthDataDto
import org.example.project.data.remote.dto.ForgotPasswordRequestDto
import org.example.project.data.remote.dto.LoginRequestDto
import org.example.project.data.remote.dto.RegisterRequestDto
import org.example.project.data.remote.dto.RegisterResponseDto
import org.example.project.data.remote.dto.ResetPasswordRequestDto
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface cho Desktop App gọi backend Spring Boot.
 */
interface DesktopAuthApiService {

    @POST("clothes/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<ApiResponse<AuthDataDto>>

    @POST("clothes/api/v1/tenants/register")
    suspend fun registerTenants(@Body request: RegisterRequestDto): Response<RegisterResponseDto>

    @POST("clothes/api/v1/auth/refresh")
    fun refreshToken(
        @Query("refreshToken") refreshToken: String
    ): Call<ApiResponse<AuthDataDto>>

    @POST("clothes/api/v1/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequestDto
    ): Response<ApiResponse<Any>>

    @POST("clothes/api/v1/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequestDto
    ): Response<ApiResponse<Any>>
}
