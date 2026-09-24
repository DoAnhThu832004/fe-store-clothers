package org.example.project.features.auth.reset

import org.example.project.domain.validator.PasswordStrength

/**
 * MVI Contract cho màn hình Đặt Lại Mật Khẩu.
 *
 * [token] được inject từ Deep Link Navigation argument — không thuộc về UiState
 * vì nó là constant trong suốt vòng đời màn hình.
 */

// ─── UI State ─────────────────────────────────────────────────────────────────

data class ResetPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isNewPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    /** Phân tích độ mạnh mật khẩu — cập nhật realtime mỗi keystroke */
    val passwordStrength: PasswordStrength = PasswordStrength(),
    /** Mật khẩu xác nhận có khớp với mật khẩu mới không */
    val doPasswordsMatch: Boolean = false,
    val screenState: ScreenState = ScreenState.Idle
) {
    sealed interface ScreenState {
        /** Trạng thái mặc định — đang nhập */
        data object Idle : ScreenState

        /**
         * Token hết hạn / sai / đã dùng.
         * Hiển thị full-screen expired UI + nút "Yêu cầu gửi lại link".
         */
        data object TokenExpired : ScreenState

        /** Đặt lại thành công — chuẩn bị navigate về Login */
        data object Success : ScreenState

        /** Lỗi validation phía client */
        data class ValidationError(val error: ResetPasswordValidationError) : ScreenState

        /** Lỗi network hoặc server không xác định */
        data class NetworkError(val message: String) : ScreenState
    }
}

// ─── Validation Error Types ──────────────────────────────────────────────────

sealed interface ResetPasswordValidationError {
    data object NewPasswordBlank : ResetPasswordValidationError
    data object PasswordTooWeak : ResetPasswordValidationError
    data object PasswordsDoNotMatch : ResetPasswordValidationError
}

// ─── User Intents ─────────────────────────────────────────────────────────────

sealed interface ResetPasswordIntent {
    data class NewPasswordChanged(val password: String) : ResetPasswordIntent
    data class ConfirmPasswordChanged(val password: String) : ResetPasswordIntent
    data object ToggleNewPasswordVisibility : ResetPasswordIntent
    data object ToggleConfirmPasswordVisibility : ResetPasswordIntent
    data object SubmitClicked : ResetPasswordIntent
    data object ErrorDismissed : ResetPasswordIntent
}

// ─── One-time Side Effects ────────────────────────────────────────────────────

sealed interface ResetPasswordEffect {
    /** Điều hướng về LoginScreen sau khi reset thành công */
    data object NavigateToLogin : ResetPasswordEffect

    /** Điều hướng về ForgotPasswordScreen khi token hết hạn */
    data object NavigateToForgotPassword : ResetPasswordEffect

    /**
     * JWT cũ bị revoke — bắt buộc logout toàn bộ session.
     * Xảy ra khi backend detect request từ token cũ sau khi reset.
     */
    data object ForceLogout : ResetPasswordEffect
}
