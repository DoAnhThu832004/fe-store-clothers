package org.example.project.domain.usecase

import org.example.project.core.common.Resource
import org.example.project.domain.model.User
import org.example.project.domain.repository.AuthRepository

/**
 * UseCase thực hiện nghiệp vụ đăng nhập trong Clean Architecture.
 */
class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        storeCode: String,
        username: String,
        password: String
    ): Resource<User> {
        return authRepository.login(
            storeCode = storeCode.trim(),
            username = username.trim(),
            password = password
        )
    }
}
