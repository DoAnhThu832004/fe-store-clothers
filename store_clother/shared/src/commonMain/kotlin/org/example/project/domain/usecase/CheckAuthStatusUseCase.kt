package org.example.project.domain.usecase

import org.example.project.domain.model.User
import org.example.project.domain.repository.AuthRepository

/**
 * UseCase kiểm tra trạng thái xác thực và phân quyền điều hướng người dùng.
 */
class CheckAuthStatusUseCase(
    private val authRepository: AuthRepository
) {
    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun getUserRoles(): List<String> = authRepository.getUserRoles()

    fun getCurrentUser(): User? = authRepository.getCurrentUser()

    fun clearSession() = authRepository.clearSession()
}
