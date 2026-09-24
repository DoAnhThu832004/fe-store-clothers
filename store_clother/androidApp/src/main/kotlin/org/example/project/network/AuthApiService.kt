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
 * Retrofit interface định nghĩa tất cả các API endpoint liên quan đến xác thực.
 *
 * Trả về [Response]<[ApiResponse]<T>> để:
 * - Kiểm tra HTTP status code (isSuccessful, code())
 * - Truy cập error body khi thất bại
 */
interface AuthApiService {

    /**
     * Đăng nhập với username và password.
     * Endpoint: POST /clothes/api/v1/auth/login
     */
    @POST("clothes/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<ApiResponse<AuthDataDto>>

    /**
     * Đăng ký tenant mới.
     * Endpoint: POST /clothes/api/v1/tenants/register
     */
    @POST("clothes/api/v1/tenants/register")
    suspend fun registerTenants(@Body request: RegisterRequestDto): Response<RegisterResponseDto>

    /**
     * Làm mới access token bằng refresh token.
     * Endpoint: POST /clothes/api/v1/auth/refresh?refreshToken=...
     *
     * ⚠️ QUAN TRỌNG: Backend nhận refreshToken qua Query Parameter (@RequestParam),
     * KHÔNG phải Request Body.
     *
     * ⚠️ Trả về [Call] thay vì suspend để TokenRefreshInterceptor
     * có thể gọi ĐỒNG BỘ (blocking) bằng .execute() — interceptor
     * không thể dùng suspend/coroutine.
     */
    @POST("clothes/api/v1/auth/refresh")
    fun refreshToken(
        @Query("refreshToken") refreshToken: String
    ): Call<ApiResponse<AuthDataDto>>

    /**
     * Gửi email đặt lại mật khẩu.
     * Endpoint: POST /clothes/api/v1/auth/forgot-password
     *
     * Backend LUÔN trả 200 OK kể cả email không tồn tại (chống User Enumeration).
     * Chỉ thất bại khi 429 (> 3 lần / 15 phút).
     */
    @POST("clothes/api/v1/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequestDto
    ): Response<ApiResponse<Any>>

    /**
     * Đặt lại mật khẩu bằng token từ Deep Link email.
     * Endpoint: POST /clothes/api/v1/auth/reset-password
     *
     * Lỗi 400 với code RESET_TOKEN_INVALID nếu token sai/hết hạn/đã dùng.
     * Sau thành công, backend revoke toàn bộ JWT cũ → mọi request cũ sẽ nhận 401.
     */
    @POST("clothes/api/v1/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequestDto
    ): Response<ApiResponse<Any>>
}

