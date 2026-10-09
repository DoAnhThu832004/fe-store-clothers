package org.example.project.local

import java.util.prefs.Preferences

/**
 * Quản lý session và token trên Desktop (sử dụng Java Preferences API).
 * Dữ liệu được lưu an toàn vào registry người dùng (Windows) hoặc preferences file (macOS/Linux).
 */
class DesktopSessionManager {

    private val prefs: Preferences = Preferences.userNodeForPackage(DesktopSessionManager::class.java)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROLES = "roles"
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Long,
        username: String,
        fullName: String,
        email: String,
        roles: List<String>
    ) {
        prefs.put(KEY_ACCESS_TOKEN, accessToken)
        prefs.put(KEY_REFRESH_TOKEN, refreshToken)
        prefs.putLong(KEY_USER_ID, userId)
        prefs.put(KEY_USERNAME, username)
        prefs.put(KEY_FULL_NAME, fullName)
        prefs.put(KEY_EMAIL, email)
        prefs.put(KEY_ROLES, roles.joinToString(","))
        prefs.flush()
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        prefs.put(KEY_ACCESS_TOKEN, accessToken)
        prefs.put(KEY_REFRESH_TOKEN, refreshToken)
        prefs.flush()
    }

    fun getAccessToken(): String? = prefs.get(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.get(KEY_REFRESH_TOKEN, null)

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun getUsername(): String? = prefs.get(KEY_USERNAME, null)

    fun getFullName(): String? = prefs.get(KEY_FULL_NAME, null)

    fun getEmail(): String? = prefs.get(KEY_EMAIL, null)

    fun getUserRoles(): List<String> {
        val rolesString = prefs.get(KEY_ROLES, null) ?: return emptyList()
        return rolesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    fun clearSession() {
        prefs.remove(KEY_ACCESS_TOKEN)
        prefs.remove(KEY_REFRESH_TOKEN)
        prefs.remove(KEY_USER_ID)
        prefs.remove(KEY_USERNAME)
        prefs.remove(KEY_FULL_NAME)
        prefs.remove(KEY_EMAIL)
        prefs.remove(KEY_ROLES)
        prefs.flush()
    }
}
