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
import org.example.project.network.AuthApiService
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Android implementation của [AuthRemoteDataSource] sử dụng Retrofit [AuthApiService].
 */
class AndroidAuthRemoteDataSource(
    private val apiService: AuthApiService
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
        } catch (e: Exception) {
            Resource.Error(Throwable(e.localizedMessage ?: "Đã xảy ra lỗi kết nối."))
        }
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequestDto): Resource<String> {
        return try {
            val response = apiService.forgotPassword(request)
            when {
                response.isSuccessful -> {
                    val message = response.body()?.message
                        ?: "Nếu email tồn tại, bạn sẽ nhận được link đặt lại mật khẩu."
                    Resource.Success(message)
                }
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

    override suspend fun resetPassword(request: ResetPasswordRequestDto): Resource<Unit> {
        return try {
            val response = apiService.resetPassword(request)
            when {
                response.isSuccessful -> Resource.Success(Unit)
                response.code() == 400 -> {
                    val errorBody = response.errorBody()?.string()
                    val errorCode = parseErrorCode(errorBody)
                    if (errorCode == ERROR_CODE_RESET_TOKEN_INVALID) {
                        Resource.Error(ResetTokenInvalidException())
                    } else {
                        Resource.Error(Throwable(parseErrorMessage(errorBody) ?: "Yêu cầu không hợp lệ."))
                    }
                }
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

    override fun refreshToken(refreshToken: String): String? {
        val response = runCatching {
            apiService.refreshToken(refreshToken).execute()
        }.getOrNull() ?: return null

        if (!response.isSuccessful) return null
        return response.body()?.data?.accessToken
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody == null) return null
        return try {
            gson.fromJson(errorBody, ApiErrorResponse::class.java)?.error?.message
        } catch (e: Exception) { null }
    }

    private fun parseErrorCode(errorBody: String?): String? {
        if (errorBody == null) return null
        return try {
            gson.fromJson(errorBody, ApiErrorResponse::class.java)?.error?.code
        } catch (e: Exception) { null }
    }
}
