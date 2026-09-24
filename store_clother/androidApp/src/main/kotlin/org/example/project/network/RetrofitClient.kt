package org.example.project.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.example.project.local.SessionManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton cung cấp instance Retrofit đã được cấu hình.
 *
 * # Thứ tự Interceptor (RẤT QUAN TRỌNG)
 * OkHttp xử lý interceptor theo đúng thứ tự được thêm vào:
 *
 *  Request đi ra:  [AuthInterceptor] → [TokenRefreshInterceptor] → [Logger] → NETWORK
 *  Response về:    NETWORK → [Logger] → [TokenRefreshInterceptor] → [AuthInterceptor]
 *
 *  1. [AuthInterceptor]          : Gắn token vào header trước khi gửi.
 *  2. [TokenRefreshInterceptor]  : Nếu nhận 401, refresh token và retry.
 *  3. [loggingInterceptor]       : Log toàn bộ request/response sau khi đã có token.
 *
 * ⚠️ Bắt buộc gọi [init] trước khi dùng bất kỳ service nào.
 *
 * LƯU Ý: Base URL dùng 10.0.2.2 thay localhost vì Android Emulator
 *         map 10.0.2.2 → máy host (PC/Mac của bạn).
 */
object RetrofitClient {

    private const val BASE_URL = "http://192.168.1.79:8080/"
    private const val TIMEOUT_SECONDS = 30L

    private lateinit var sessionManager: SessionManager

    /** Khởi tạo RetrofitClient với SessionManager. Gọi một lần trong MainActivity. */
    fun init(sessionManager: SessionManager) {
        this.sessionManager = sessionManager
    }

    /** Logging interceptor — ghi toàn bộ body request/response để debug */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * OkHttpClient chính — được cấu hình đầy đủ với cả 2 interceptors.
     * Lazy initialization để đảm bảo [sessionManager] đã được set trước khi dùng.
     */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // 1️⃣ Gắn "Authorization: Bearer <token>" vào mọi request
            .addInterceptor(AuthInterceptor(sessionManager))
            // 2️⃣ Khi nhận 401: tự động refresh token rồi retry request
            .addInterceptor(TokenRefreshInterceptor(sessionManager, BASE_URL))
            // 3️⃣ Log request/response (đặt cuối cùng để log đầy đủ cả header đã được gắn)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /** Retrofit instance sử dụng Gson để parse JSON */
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /** Factory method tạo service instance */
    inline fun <reified T> createService(): T = retrofit.create(T::class.java)
}
