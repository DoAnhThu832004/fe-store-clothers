package org.example.project.data.local.datasource

import org.example.project.domain.model.User

/**
 * Interface cho Local Data Source lưu trữ session/token người dùng.
 */
interface SessionLocalDataSource {

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        user: User
    )

    fun updateTokens(accessToken: String, refreshToken: String)

    fun getAccessToken(): String?

    fun getRefreshToken(): String?

    fun getUser(): User?

    fun getUserRoles(): List<String>

    fun isLoggedIn(): Boolean

    fun clearSession()
}
