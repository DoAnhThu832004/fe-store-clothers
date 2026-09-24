package org.example.project.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Quản lý session người dùng bằng [EncryptedSharedPreferences].
 *
 * EncryptedSharedPreferences mã hóa cả key và value trước khi ghi ra disk —
 * đảm bảo token không bị đọc ngay cả khi thiết bị bị root.
 *
 * Được khởi tạo một lần trong Application/MainActivity và inject vào Repository.
 */
class SessionManager(context: Context) {

    private val gson = Gson()

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREF_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ─── Ghi session ────────────────────────────────────────────────────────

    /**
     * Lưu toàn bộ thông tin phiên đăng nhập vào EncryptedSharedPreferences.
     *
     * @param accessToken  JWT access token
     * @param refreshToken JWT refresh token
     * @param userId       ID người dùng
     * @param username     Username
     * @param fullName     Họ tên đầy đủ
     * @param email        Email
     * @param roles        Danh sách role (ROLE_OWNER, ROLE_CASHIER, ...)
     */
    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Long,
        username: String,
        fullName: String,
        email: String,
        roles: List<String>
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROLES, gson.toJson(roles))
            .apply()
    }

    // ─── Đọc session ────────────────────────────────────────────────────────

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getFullName(): String? = prefs.getString(KEY_FULL_NAME, null)

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    /**
     * Trả về danh sách Role của user hiện tại.
     * Trả về list rỗng nếu chưa đăng nhập.
     */
    fun getUserRoles(): List<String> {
        val json = prefs.getString(KEY_ROLES, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    /**
     * Kiểm tra xem người dùng đã đăng nhập chưa (có accessToken trong storage).
     * Lưu ý: Hàm này chỉ kiểm tra sự tồn tại của token, không validate hết hạn.
     * Validation hết hạn sẽ được xử lý bởi TokenRefreshInterceptor (401 response).
     */
    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    // ─── Xoá session ────────────────────────────────────────────────────────

    /**
     * Cập nhật token mới sau khi refresh thành công.
     * Chỉ ghi đè accessToken và refreshToken, KHÔNG đụng đến userInfo.
     *
     * Được gọi bởi [TokenRefreshInterceptor] sau khi refresh token API thành công.
     */
    fun updateTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    /**
     * Xoá toàn bộ session — dùng khi Logout hoặc token bị revoke.
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREF_FILE_NAME = "store_clother_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROLES = "roles"
    }
}
