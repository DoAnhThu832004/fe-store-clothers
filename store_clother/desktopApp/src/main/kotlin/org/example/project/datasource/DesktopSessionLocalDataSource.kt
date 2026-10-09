package org.example.project.datasource

import org.example.project.data.local.datasource.SessionLocalDataSource
import org.example.project.domain.model.User
import org.example.project.local.DesktopSessionManager

/**
 * Desktop implementation của [SessionLocalDataSource] sử dụng [DesktopSessionManager].
 */
class DesktopSessionLocalDataSource(
    private val sessionManager: DesktopSessionManager
) : SessionLocalDataSource {

    override fun saveSession(accessToken: String, refreshToken: String, user: User) {
        sessionManager.saveSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user.id,
            username = user.username,
            fullName = user.fullName,
            email = user.email,
            roles = user.roles
        )
    }

    override fun updateTokens(accessToken: String, refreshToken: String) {
        sessionManager.updateTokens(accessToken, refreshToken)
    }

    override fun getAccessToken(): String? = sessionManager.getAccessToken()

    override fun getRefreshToken(): String? = sessionManager.getRefreshToken()

    override fun getUser(): User? {
        val userId = sessionManager.getUserId()
        if (userId == -1L) return null
        return User(
            id = userId,
            username = sessionManager.getUsername() ?: "",
            fullName = sessionManager.getFullName() ?: "",
            email = sessionManager.getEmail() ?: "",
            roles = sessionManager.getUserRoles()
        )
    }

    override fun getUserRoles(): List<String> = sessionManager.getUserRoles()

    override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    override fun clearSession() = sessionManager.clearSession()
}
