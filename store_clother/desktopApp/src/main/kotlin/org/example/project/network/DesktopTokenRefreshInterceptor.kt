package org.example.project.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.example.project.data.remote.dto.ApiResponse
import org.example.project.data.remote.dto.AuthDataDto
import org.example.project.local.DesktopSessionManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Tự động làm mới token khi nhận HTTP 401 trên Desktop.
 */
class DesktopTokenRefreshInterceptor(
    private val sessionManager: DesktopSessionManager,
    private val baseUrl: String
) : Interceptor {

    private val lock = ReentrantLock()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalResponse = chain.proceed(originalRequest)

        if (originalResponse.code != 401) {
            return originalResponse
        }

        if (originalRequest.url.encodedPath.contains("refresh")) {
            originalResponse.close()
            sessionManager.clearSession()
            return originalResponse
        }

        originalResponse.close()

        val newAccessToken = lock.withLock {
            val currentToken = sessionManager.getAccessToken()
            val requestToken = originalRequest.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()

            if (currentToken != null && currentToken != requestToken) {
                return@withLock currentToken
            }

            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                sessionManager.clearSession()
                return@withLock null
            }

            performTokenRefresh(refreshToken)
        }

        return if (newAccessToken != null) {
            val retriedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
            chain.proceed(retriedRequest)
        } else {
            sessionManager.clearSession()
            val emptyBody = "".toResponseBody("application/json".toMediaTypeOrNull())
            Response.Builder()
                .request(originalRequest)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized: Session Expired")
                .body(emptyBody)
                .build()
        }
    }

    private fun performTokenRefresh(refreshToken: String): String? {
        val plainClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(plainClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(DesktopAuthApiService::class.java)

        return try {
            val response = service.refreshToken(refreshToken).execute()
            if (response.isSuccessful) {
                val authData: AuthDataDto? = response.body()?.data
                if (authData != null) {
                    sessionManager.updateTokens(
                        accessToken = authData.accessToken,
                        refreshToken = authData.refreshToken
                    )
                    authData.accessToken
                } else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
