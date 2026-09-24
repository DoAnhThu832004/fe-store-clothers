package org.example.project.repository

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.core.common.Resource
import org.example.project.data.remote.dto.ApiErrorResponse
import org.example.project.data.remote.dto.ERROR_CODE_RESET_TOKEN_INVALID
import org.example.project.data.remote.dto.ErrorResponse
import org.example.project.data.remote.dto.ForgotPasswordRequestDto
import org.example.project.data.remote.dto.LoginRequestDto
import org.example.project.data.remote.dto.RegisterRequestDto
import org.example.project.data.remote.dto.RegisterResponseDto
import org.example.project.data.remote.dto.ResetPasswordRequestDto
import org.example.project.data.remote.dto.UserInfoDto
import org.example.project.domain.repository.IAuthRepository
import org.example.project.domain.repository.RateLimitException
import org.example.project.domain.repository.ResetTokenInvalidException
import org.example.project.local.SessionManager
import org.example.project.network.AuthApiService
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * AuthRepository — implement [IAuthRepository], xử lý toàn bộ nghiệp vụ xác thực.
 *
 * Trách nhiệm:
 * 1. Gọi [AuthApiService] qua Retrofit trên IO Dispatcher
 * 2. Bóc tách dữ liệu từ ApiResponse envelope
 * 3. Lưu/xóa token và thông tin user vào [SessionManager]
 * 4. Trả về [Resource.Success]<T> hoặc [Resource.Error] với exception đúng loại
 */
class AuthRepository(
    private val apiService: AuthApiService,
    private val sessionManager: SessionManager
) : IAuthRepository {

    private val gson = Gson()

    // ─── Register ────────────────────────────────────────────────────────────

    override suspend fun registerTenant(request: RegisterRequestDto): Resource<RegisterResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.registerTenants(request)
                when {
                    response.isSuccessful -> {
                        val data = response.body()
                        if (data != null) Resource.Success(data)
                        else Resource.Error(Throwable("Đăng ký thành công nhưng không có dữ liệu trả về"))
                    }
                    else -> {
                        val errorString = response.errorBody()?.string()
                        if (errorString != null) {
                            try {
                                val errorObj = gson.fromJson(errorString, ErrorResponse::class.java)
                                Resource.Error(Throwable(errorObj.error?.message ?: "Lỗi máy chủ"))
                            } catch (e: Exception) {
                                Resource.Error(Throwable("Lỗi hệ thống: ${response.code()}"))
                            }
                        } else {
                            Resource.Error(Throwable("Lỗi không xác định: ${response.code()}"))
                        }
                    }
                }
            } catch (e: Exception) {
                Resource.Error(Throwable(e.localizedMessage ?: "Đã xảy ra lỗi kết nối."))
            }
        }

    // ─── Login ───────────────────────────────────────────────────────────────

    override suspend fun login(storeCode: String, username: String, password: String): Resource<UserInfoDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequestDto(storeCode, username, password))
                when {
                    response.isSuccessful -> {
                        val apiResponse = response.body()
                        val authData = apiResponse?.data
                        if (apiResponse?.success == true && authData != null) {
                            sessionManager.saveSession(
                                accessToken = authData.accessToken,
                                refreshToken = authData.refreshToken,
                                userId = authData.userInfo.id,
                                username = authData.userInfo.username,
                                fullName = authData.userInfo.fullName,
                                email = authData.userInfo.email,
                                roles = authData.userInfo.roles
                            )
                            Resource.Success(authData.userInfo)
                        } else {
                            Resource.Error(Throwable(apiResponse?.message ?: "Phản hồi không hợp lệ từ server"))
                        }
                    }
                    response.code() == 401 ->
                        Resource.Error(Throwable("Sai tên đăng nhập hoặc mật khẩu"))
                    else -> {
                        val errorMessage = runCatching { response.errorBody()?.string() }.getOrNull()
                        Resource.Error(Throwable(errorMessage ?: "Lỗi server: ${response.code()}"))
                    }
                }
            } catch (e: UnknownHostException) {
                Resource.Error(Throwable("Không có kết nối internet. Vui lòng thử lại."))
            } catch (e: ConnectException) {
                Resource.Error(Throwable("Không thể kết nối đến server. Kiểm tra server đang chạy."))
            } catch (e: SocketTimeoutException) {
                Resource.Error(Throwable("Kết nối quá chậm. Vui lòng thử lại."))
            } catch (e: SSLException) {
                Resource.Error(Throwable("Lỗi bảo mật kết nối (SSL). Vui lòng thử lại."))
            } catch (e: IOException) {
                Resource.Error(Throwable("Lỗi mạng: ${e.message ?: "Vui lòng thử lại."}"))
            } catch (e: Exception) {
                Resource.Error(Throwable(e.localizedMessage ?: "Đã xảy ra lỗi. Vui lòng thử lại."))
            }
        }

    // ─── Forgot Password ──────────────────────────────────────────────────────

    /**
     * Gửi email đặt lại mật khẩu.
     *
     * Backend luôn 200 OK kể cả email không tồn tại (chống User Enumeration).
     * Trả về [RateLimitException] nếu HTTP 429 (> 3 lần / 15 phút).
     */
    override suspend fun forgotPassword(email: String, storeCode: String): Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.forgotPassword(ForgotPasswordRequestDto(email, storeCode))
                when {
                    // 200 OK: Luôn hiển thị success để chống User Enumeration
                    response.isSuccessful -> {
                        val message = response.body()?.message
                            ?: "Nếu email tồn tại, bạn sẽ nhận được link đặt lại mật khẩu."
                        Resource.Success(message)
                    }
                    // 429 Rate Limit — ViewModel sẽ hiển thị banner đặc biệt
                    response.code() == 429 ->
                        Resource.Error(RateLimitException())
                    else -> {
                        val errorMessage = parseErrorMessage(response.errorBody()?.string())
                        Resource.Error(Throwable(errorMessage ?: "Lỗi server: ${response.code()}"))
                    }
                }
            } catch (e: UnknownHostException) {
                Resource.Error(Throwable("Không có kết nối internet. Vui lòng thử lại."))
            } catch (e: ConnectException) {
                Resource.Error(Throwable("Không thể kết nối đến server."))
            } catch (e: SocketTimeoutException) {
                Resource.Error(Throwable("Kết nối quá chậm. Vui lòng thử lại."))
            } catch (e: IOException) {
                Resource.Error(Throwable("Lỗi mạng: ${e.message ?: "Vui lòng thử lại."}"))
            } catch (e: Exception) {
                Resource.Error(Throwable(e.localizedMessage ?: "Đã xảy ra lỗi. Vui lòng thử lại."))
            }
        }

    // ─── Reset Password ───────────────────────────────────────────────────────

    /**
     * Đặt lại mật khẩu bằng token từ Deep Link email.
     *
     * Trả về [ResetTokenInvalidException] khi HTTP 400 + code RESET_TOKEN_INVALID.
     * Sau khi thành công, backend revoke toàn bộ JWT cũ — session phải được clear.
     */
    override suspend fun resetPassword(
        token: String,
        newPassword: String,
        confirmPassword: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.resetPassword(
                ResetPasswordRequestDto(token, newPassword, confirmPassword)
            )
            when {
                // 200 OK: Thành công — clear session vì JWT cũ bị revoke
                response.isSuccessful -> {
                    sessionManager.clearSession()
                    Resource.Success(Unit)
                }
                // 400 RESET_TOKEN_INVALID: Token hết hạn / sai / đã dùng
                response.code() == 400 -> {
                    val errorBody = response.errorBody()?.string()
                    val errorCode = parseErrorCode(errorBody)
                    if (errorCode == ERROR_CODE_RESET_TOKEN_INVALID) {
                        Resource.Error(ResetTokenInvalidException())
                    } else {
                        Resource.Error(Throwable(parseErrorMessage(errorBody) ?: "Yêu cầu không hợp lệ."))
                    }
                }
                // 401: JWT đã bị revoke bởi request khác trước đó
                response.code() == 401 ->
                    Resource.Error(Throwable("401 Unauthorized: Phiên đăng nhập đã hết hạn."))
                else -> {
                    val errorMessage = parseErrorMessage(response.errorBody()?.string())
                    Resource.Error(Throwable(errorMessage ?: "Lỗi server: ${response.code()}"))
                }
            }
        } catch (e: UnknownHostException) {
            Resource.Error(Throwable("Không có kết nối internet. Vui lòng thử lại."))
        } catch (e: ConnectException) {
            Resource.Error(Throwable("Không thể kết nối đến server."))
        } catch (e: SocketTimeoutException) {
            Resource.Error(Throwable("Kết nối quá chậm. Vui lòng thử lại."))
        } catch (e: IOException) {
            Resource.Error(Throwable("Lỗi mạng: ${e.message ?: "Vui lòng thử lại."}"))
        } catch (e: Exception) {
            Resource.Error(Throwable(e.localizedMessage ?: "Đã xảy ra lỗi. Vui lòng thử lại."))
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /** Bóc tách message từ JSON lỗi envelope backend */
    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody == null) return null
        return try {
            gson.fromJson(errorBody, ApiErrorResponse::class.java)?.error?.message
        } catch (e: Exception) { null }
    }

    /** Bóc tách error code — dùng để nhận diện RESET_TOKEN_INVALID */
    private fun parseErrorCode(errorBody: String?): String? {
        if (errorBody == null) return null
        return try {
            gson.fromJson(errorBody, ApiErrorResponse::class.java)?.error?.code
        } catch (e: Exception) { null }
    }
}
