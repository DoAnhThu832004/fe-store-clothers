package org.example.project.data.local.datasource

import org.example.project.domain.model.User

/**
 * Implementation lưu trữ Session trong bộ nhớ (In-memory).
 * Dùng làm fallback mặc định cho KMP Desktop, iOS preview, Web và Unit Tests.
 */
class InMemorySessionLocalDataSource : SessionLocalDataSource {

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var currentUser: User? = null

    override fun saveSession(accessToken: String, refreshToken: String, user: User) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.currentUser = user
    }

    override fun updateTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    override fun getAccessToken(): String? = accessToken

    override fun getRefreshToken(): String? = refreshToken

    override fun getUser(): User? = currentUser

    override fun getUserRoles(): List<String> = currentUser?.roles ?: emptyList()

    override fun isLoggedIn(): Boolean = !accessToken.isNullOrBlank()

    override fun clearSession() {
        accessToken = null
        refreshToken = null
        currentUser = null
    }
}
