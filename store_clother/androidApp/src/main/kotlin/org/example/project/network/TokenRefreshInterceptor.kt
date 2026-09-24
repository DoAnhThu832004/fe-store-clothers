package org.example.project.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.example.project.data.remote.dto.ApiResponse
import org.example.project.data.remote.dto.AuthDataDto
import org.example.project.local.SessionManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * TokenRefreshInterceptor — Tự động làm mới token khi nhận HTTP 401 (Unauthorized).
 *
 * # Vấn đề cần giải quyết
 * AccessToken có thời hạn ngắn (VD: 24h). Khi hết hạn, server trả về HTTP 401.
 * Thay vì đá văng user ra màn hình Login, interceptor này sẽ:
 *   1. Lặng lẽ gọi API refresh token ở nền.
 *   2. Lưu token mới vào SessionManager.
 *   3. Tự động thử lại (retry) request gốc với token mới.
 *
 * # Vấn đề "Thundering Herd" (bầy thú xông vào)
 * Khi nhiều API đồng thời nhận 401 (VD: app gọi 5 API cùng lúc khi token vừa hết hạn),
 * tất cả 5 requests sẽ cùng lúc cố gắng refresh token → sinh ra 5 lần gọi refresh API.
 * Giải pháp: Dùng [ReentrantLock] + [refreshed] flag để đảm bảo chỉ CÓ MỘT request
 * refresh token được thực thi, các request còn lại chờ và dùng chung token mới.
 *
 * @param sessionManager  Để đọc/ghi token
 * @param baseUrl         Base URL để tạo retrofit client riêng gọi refresh API
 */
class TokenRefreshInterceptor(
    private val sessionManager: SessionManager,
    private val baseUrl: String
) : Interceptor { // kế thừa thư viện OkHttp. Nó chặn request và trả về response

    private val lock = ReentrantLock() // Lock dùng để ngăn nhiều thread cùng refresh token một lúc (Thundering Herd fix)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request() // lấy request hiện tại
        val originalResponse = chain.proceed(originalRequest) // tiến hành request và lấy response

        // Nếu server không trả về 401, trả về response bình thường
        if (originalResponse.code != 401) {
            return originalResponse
        }

        // Tránh vòng lặp vô hạn: nếu chính request refresh token bị 401 thì xóa session và return
        if (originalRequest.url.encodedPath.contains("refresh")) {
            originalResponse.close() // Đóng response cũ để tránh leak bộ nhớ
            sessionManager.clearSession() // Xoá session bắt đăng nhập lại
            return originalResponse
        }

        // --- Bắt đầu xử lý 401 ---
        originalResponse.close() // Đóng response cũ để tránh leak bộ nhớ

        // Dùng Lock để chỉ cho 1 thread refresh token, các thread khác chờ
        return lock.withLock {
            // Sau khi lock được giải phóng, kiểm tra xem token đã được refresh bởi thread khác chưa
            val currentToken = sessionManager.getAccessToken()
            val requestToken = originalRequest.header("Authorization")
                ?.removePrefix("Bearer ")?.trim()

            // Nếu token trong storage đã khác token trong request gốc → thread khác đã refresh rồi
            // → Thử lại request với token mới luôn, không cần gọi refresh API nữa
            if (currentToken != null && currentToken != requestToken) {
                val retryRequest = originalRequest.withNewToken(currentToken)
                return@withLock chain.proceed(retryRequest)
            }

            // Token vẫn giống cũ → Đây là thread đầu tiên, tiến hành refresh
            val refreshToken = sessionManager.getRefreshToken()

            if (refreshToken.isNullOrBlank()) {
                // Không có refresh token → Session không còn hợp lệ, xóa và yêu cầu đăng nhập lại
                sessionManager.clearSession()
                return@withLock chain.proceed(originalRequest)
            }

            // Gọi API refresh token (dùng OkHttpClient riêng, KHÔNG qua interceptor này để tránh loop)
            val newToken = runCatching {
                callRefreshTokenApi(refreshToken)
            }.getOrNull()

            if (newToken == null) {
                // Refresh thất bại (refresh token cũng hết hạn) → Xóa session
                sessionManager.clearSession()
                return@withLock chain.proceed(originalRequest)
            }

            // Refresh thành công → Thử lại request gốc với token mới
            val retryRequest = originalRequest.withNewToken(newToken)
            chain.proceed(retryRequest)
        }
    }

    /**
     * Gọi API refresh token bằng một OkHttpClient riêng biệt (không có interceptor).
     * Việc dùng client riêng đảm bảo request này KHÔNG đi qua [TokenRefreshInterceptor],
     * tránh vòng lặp vô hạn.
     *
     * @return accessToken mới nếu thành công, null nếu thất bại
     */
    private fun callRefreshTokenApi(refreshToken: String): String? {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        // Client riêng — không có AuthInterceptor và TokenRefreshInterceptor
        val plainClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(plainClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(AuthApiService::class.java)

        // Gọi API đồng bộ (blocking) — truyền refreshToken thẳng làm @Query param
        // GET /clothes/api/v1/auth/refresh?refreshToken=...
        val response = runCatching {
            service.refreshToken(refreshToken).execute()
        }.getOrNull() ?: return null

        if (!response.isSuccessful) return null

        val authData = response.body()?.data ?: return null

        // Lưu token mới vào SessionManager
        sessionManager.updateTokens(
            accessToken = authData.accessToken,
            refreshToken = authData.refreshToken
        )

        return authData.accessToken
    }

    /** Extension function: tạo bản sao request với token mới */
    private fun Request.withNewToken(token: String): Request =
        newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
}
