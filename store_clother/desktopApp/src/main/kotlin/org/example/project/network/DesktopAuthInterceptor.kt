package org.example.project.network

import okhttp3.Interceptor
import okhttp3.Response
import org.example.project.local.DesktopSessionManager

/**
 * Gắn JWT Access Token vào Header của request trên Desktop.
 */
class DesktopAuthInterceptor(
    private val sessionManager: DesktopSessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = sessionManager.getAccessToken()

        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
