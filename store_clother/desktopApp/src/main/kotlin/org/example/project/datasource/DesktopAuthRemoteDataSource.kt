package org.example.project.datasource

import com.google.gson.Gson
import org.example.project.core.common.Resource
import org.example.project.data.remote.datasource.AuthRemoteDataSource
import org.example.project.data.remote.dto.ApiErrorResponse
import org.example.project.data.remote.dto.AuthDataDto
import org.example.project.data.remote.dto.ERROR_CODE_RESET_TOKEN_INVALID
import org.example.project.data.remote.dto.ErrorResponse
import org.example.project.data.remote.dto.ForgotPasswordRequestDto
import org.example.project.data.remote.dto.LoginRequestDto
import org.example.project.data.remote.dto.RegisterRequestDto
import org.example.project.data.remote.dto.RegisterResponseDto
import org.example.project.data.remote.dto.ResetPasswordRequestDto
import org.example.project.domain.model.RateLimitException
import org.example.project.domain.model.ResetTokenInvalidException
import org.example.project.network.DesktopAuthApiService
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Desktop implementation của [AuthRemoteDataSource] gọi Retrofit [DesktopAuthApiService].
 */
class DesktopAuthRemoteDataSource(
    private val apiService: DesktopAuthApiService
) : AuthRemoteDataSource {

    private val gson = Gson()

    override suspend fun login(request: LoginRequestDto): Resource<AuthDataDto> {
        return try {
            val response = apiService.login(request)
            when {
                response.isSuccessful -> {
                    val apiResponse = response.body()
                    val authData = apiResponse?.data
                    if (apiResponse?.success == true && authData != null) {
                        Resource.Success(authData)
                    } else {
                        Resource.Error(Throwable(apiResponse?.message ?: "Phản hồi không hợp lệ từ server"))
                    }
                }
                response.code() == 401 ->
                    Resource.Error(Throwable("Sai tên đăng nhập hoặc mật khẩu"))
                else -> {
                    val errorString = response.errorBody()?.string()
                    val msg = if (errorString != null) {
                        try {
                            val errorObj = gson.fromJson(errorString, ErrorResponse::class.java)
                            errorObj.error?.message ?: "Lỗi server: ${response.code()}"
                        } catch (e: Exception) {
                            "Lỗi server: ${response.code()}"
                        }
                    } else {
                        "Lỗi server: ${response.code()}"
                    }
                    Resource.Error(Throwable(msg))
                }
            }
        } catch (e: UnknownHostException) {
            Resource.Error(Throwable("Không có kết nối mạng."))
        } catch (e: ConnectException) {
            Resource.Error(Throwable("Không thể kết nối đến Backend (localhost:8080). Hãy đảm bảo server Spring Boot đang chạy."))
        } catch (e: SocketTimeoutException) {
            Resource.Error(Throwable("Kết nối tới server quá chậm hoặc timeout."))
        } catch (e: SSLException) {
            Resource.Error(Throwable("Lỗi bảo mật kết nối (SSL)."))
        } catch (e: IOException) {
            Resource.Error(Throwable("Lỗi mạng: ${e.message ?: "Vui lòng thử lại."}"))
        } catch (e: Exception) {
            Resource.Error(Throwable(e.localizedMessage ?: "Đã xảy ra lỗi."))
        }
    }

    override suspend fun registerTenant(request: RegisterRequestDto): Resource<RegisterResponseDto> {
        return try {
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
                            val detailMsg = errorObj.error?.details?.values?.firstOrNull()?.toString()
                            val msg = detailMsg ?: errorObj.error?.message ?: "Lỗi máy chủ"
                            Resource.Error(Throwable(msg))
                        } catch (e: Exception) {
                            Resource.Error(Throwable("Lỗi hệ thống: ${response.code()}"))
                        }
                    } else {
                        Resource.Error(Throwable("Lỗi không xác định: ${response.code()}"))
                    }
                }
            }
        } catch (e: ConnectException) {
            Resource.Error(Throwable("Không thể kết nối đến Backend (localhost:8080). Hãy kiểm tra Spring Boot."))
        } catch (e: Exception) {
            Resource.Error(Throwable(e.localizedMessage ?: "Đã xảy ra lỗi kết nối."))
        }
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequestDto): Resource<String> {
        return try {
            val response = apiService.forgotPassword(request)
            when {
                response.isSuccessful -> {
                    Resource.Success("Email đặt lại mật khẩu đã được gửi thành công.")
                }
                response.code() == 429 -> {
                    Resource.Error(RateLimitException())
                }
                else -> {
                    Resource.Error(Throwable("Gửi yêu cầu thất bại: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Resource.Error(Throwable(e.localizedMessage ?: "Lỗi kết nối khi gửi email."))
        }
    }

    override suspend fun resetPassword(request: ResetPasswordRequestDto): Resource<Unit> {
        return try {
            val response = apiService.resetPassword(request)
            when {
                response.isSuccessful -> Resource.Success(Unit)
                response.code() == 400 -> {
                    val errorBody = response.errorBody()?.string()
                    val errorResponse = runCatching {
                        gson.fromJson(errorBody, ApiErrorResponse::class.java)
                    }.getOrNull()

                    if (errorResponse?.error?.code == ERROR_CODE_RESET_TOKEN_INVALID) {
                        Resource.Error(ResetTokenInvalidException())
                    } else {
                        Resource.Error(Throwable(errorResponse?.error?.message ?: "Yêu cầu không hợp lệ"))
                    }
                }
                else -> Resource.Error(Throwable("Đặt lại mật khẩu thất bại: ${response.code()}"))
            }
        } catch (e: Exception) {
            Resource.Error(Throwable(e.localizedMessage ?: "Lỗi kết nối."))
        }
    }

    override fun refreshToken(refreshToken: String): String? {
        return try {
            val response = apiService.refreshToken(refreshToken).execute()
            if (response.isSuccessful) {
                response.body()?.data?.accessToken
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
