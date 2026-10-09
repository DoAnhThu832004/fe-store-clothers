package org.example.project.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.example.project.local.DesktopSessionManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton RetrofitClient dành cho Desktop.
 * Sử dụng localhost:8080 kết nối trực tiếp đến backend Spring Boot.
 */
object DesktopRetrofitClient {

    private const val BASE_URL = "http://localhost:8080/"
    private const val TIMEOUT_SECONDS = 30L

    private lateinit var sessionManager: DesktopSessionManager

    fun init(sessionManager: DesktopSessionManager) {
        this.sessionManager = sessionManager
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(DesktopAuthInterceptor(sessionManager))
            .addInterceptor(DesktopTokenRefreshInterceptor(sessionManager, BASE_URL))
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    inline fun <reified T> createService(): T = retrofit.create(T::class.java)
}
