package org.example.project.network

import okhttp3.Interceptor
import okhttp3.Response
import org.example.project.local.SessionManager

/**
 * AuthInterceptor — Tự động gắn JWT access token vào header của MỌI request.
 *
 * Cách hoạt động:
 * - Trước khi request nào được gửi đi, interceptor này "chặn" lại.
 * - Nếu đang có accessToken trong SessionManager, nó sẽ thêm header:
 *     Authorization: Bearer eyJhbGci...
 * - Nếu không có token (chưa đăng nhập), request được gửi đi bình thường.
 *
 * ⚠️ Thứ tự quan trọng trong OkHttpClient.Builder:
 *   1. addInterceptor(AuthInterceptor)       → gắn token
 *   2. addInterceptor(TokenRefreshInterceptor) → xử lý 401 nếu token hết hạn
 *   3. addInterceptor(loggingInterceptor)    → log để debug
 */
class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Lấy accessToken hiện tại từ storage
        val token = sessionManager.getAccessToken()

        // Nếu không có token, gửi request gốc không thay đổi
        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        // Gắn header "Authorization: Bearer <token>" vào request
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
