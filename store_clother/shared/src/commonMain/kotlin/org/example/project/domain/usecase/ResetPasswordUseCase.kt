package org.example.project.domain.usecase

import org.example.project.core.common.Resource
import org.example.project.domain.repository.IAuthRepository
import org.example.project.domain.validator.AuthValidator

/**
 * UseCase: Đặt lại mật khẩu bằng token từ Deep Link email.
 *
 * ### Flow:
 * 1. Validate mật khẩu mới đáp ứng Regex backend
 * 2. Validate 2 mật khẩu khớp nhau
 * 3. Gọi [IAuthRepository.resetPassword]
 *
 * ### Lưu ý hậu thành công:
 * Backend sẽ **revoke toàn bộ JWT cũ** sau khi reset thành công.
 * Mọi request tiếp theo với token cũ sẽ nhận 401 Unauthorized.
 * ViewModel phải handle điều này và force logout.
 */
class ResetPasswordUseCase(
    private val authRepository: IAuthRepository
) {

    sealed interface ValidationError {
        data object TokenBlank : ValidationError
        data object NewPasswordBlank : ValidationError
        data object PasswordTooWeak : ValidationError
        data object PasswordsDoNotMatch : ValidationError
    }

    sealed interface Result {
        data class ValidationFailed(val error: ValidationError) : Result
        data object Success : Result
        data class Error(val throwable: Throwable) : Result
    }

    suspend operator fun invoke(
        token: String,
        newPassword: String,
        confirmPassword: String
    ): Result {
        // ── 1. Validate ──────────────────────────────────────────────────────
        when {
            token.isBlank() ->
                return Result.ValidationFailed(ValidationError.TokenBlank)
            newPassword.isBlank() ->
                return Result.ValidationFailed(ValidationError.NewPasswordBlank)
            !AuthValidator.isValidPassword(newPassword) ->
                return Result.ValidationFailed(ValidationError.PasswordTooWeak)
            !AuthValidator.doPasswordsMatch(newPassword, confirmPassword) ->
                return Result.ValidationFailed(ValidationError.PasswordsDoNotMatch)
        }

        // ── 2. Gọi Repository ────────────────────────────────────────────────
        return when (val resource = authRepository.resetPassword(token, newPassword, confirmPassword)) {
            is Resource.Success -> Result.Success
            is Resource.Error   -> Result.Error(resource.throwable)
            is Resource.Loading -> Result.Error(Throwable("Unexpected loading state"))
        }
    }
}
