package org.example.project.domain.usecase

import org.example.project.core.common.Resource
import org.example.project.domain.model.RegisterTenantParams
import org.example.project.domain.model.RegisterTenantResult
import org.example.project.domain.repository.AuthRepository

/**
 * UseCase thực hiện nghiệp vụ đăng ký tenant/cửa hàng.
 */
class RegisterTenantUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(params: RegisterTenantParams): Resource<RegisterTenantResult> {
        return authRepository.registerTenant(params)
    }
}
