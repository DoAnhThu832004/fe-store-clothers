package org.example.project.domain.usecase

import org.example.project.core.common.Resource
import org.example.project.domain.repository.IAuthRepository
import org.example.project.domain.validator.AuthValidator

/**
 * UseCase: Gửi email đặt lại mật khẩu.
 *
 * ### Flow:
 * 1. Validate email format phía client
 * 2. Validate storeCode không blank
 * 3. Gọi [IAuthRepository.forgotPassword]
 *
 * Backend luôn trả 200 OK kể cả email không tồn tại (chống User Enumeration).
 * Chỉ fail khi lỗi mạng hoặc 429 Rate Limit.
 */
class ForgotPasswordUseCase(
    private val authRepository: IAuthRepository
) {

    sealed interface ValidationError {
        data object EmailBlank : ValidationError
        data object EmailInvalid : ValidationError
        data object StoreCodeBlank : ValidationError
    }

    sealed interface Result {
        data class ValidationFailed(val error: ValidationError) : Result
        data class Success(val message: String) : Result
        data class Error(val throwable: Throwable) : Result
    }

    suspend operator fun invoke(email: String, storeCode: String): Result {
        // ── 1. Validate ──────────────────────────────────────────────────────
        val trimmedEmail = email.trim()
        val trimmedStore = storeCode.trim()

        when {
            trimmedEmail.isBlank() ->
                return Result.ValidationFailed(ValidationError.EmailBlank)
            !AuthValidator.isValidEmail(trimmedEmail) ->
                return Result.ValidationFailed(ValidationError.EmailInvalid)
            trimmedStore.isBlank() ->
                return Result.ValidationFailed(ValidationError.StoreCodeBlank)
        }

        // ── 2. Gọi Repository ────────────────────────────────────────────────
        return when (val resource = authRepository.forgotPassword(trimmedEmail, trimmedStore)) {
            is Resource.Success -> Result.Success(resource.data)
            is Resource.Error   -> Result.Error(resource.throwable)
            is Resource.Loading -> Result.Error(Throwable("Unexpected loading state"))
        }
    }
}
