package org.example.project.features.auth.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.domain.repository.ResetTokenInvalidException
import org.example.project.domain.usecase.ResetPasswordUseCase
import org.example.project.domain.validator.AuthValidator
import kotlin.reflect.KClass

/**
 * ViewModel cho màn hình Đặt Lại Mật Khẩu.
 *
 * ### Đặc điểm kỹ thuật:
 * - [token] được inject qua constructor từ Deep Link argument — immutable
 * - Realtime password checklist: mỗi keystroke trigger [AuthValidator.analyzePassword]
 * - [ResetTokenInvalidException] → emit [ResetPasswordEffect.NavigateToForgotPassword]
 * - Sau khi reset thành công → backend revoke JWT cũ → emit [ResetPasswordEffect.NavigateToLogin]
 *   (UI sẽ tự clear session vì token cũ không còn dùng được)
 */
class ResetPasswordViewModel(
    private val token: String,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ResetPasswordEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // ─── Intent Handler ───────────────────────────────────────────────────────

    fun processIntent(intent: ResetPasswordIntent) {
        when (intent) {
            is ResetPasswordIntent.NewPasswordChanged -> onNewPasswordChanged(intent.password)
            is ResetPasswordIntent.ConfirmPasswordChanged -> onConfirmPasswordChanged(intent.password)
            is ResetPasswordIntent.ToggleNewPasswordVisibility ->
                _uiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }
            is ResetPasswordIntent.ToggleConfirmPasswordVisibility ->
                _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
            is ResetPasswordIntent.SubmitClicked -> submit()
            is ResetPasswordIntent.ErrorDismissed ->
                _uiState.update { it.copy(screenState = ResetPasswordUiState.ScreenState.Idle) }
        }
    }

    // ─── Private Logic ────────────────────────────────────────────────────────

    /**
     * Cập nhật mật khẩu mới và phân tích realtime.
     * Trigger sau mỗi keystroke — không debounce để checklist phản hồi ngay lập tức.
     */
    private fun onNewPasswordChanged(password: String) {
        val strength = AuthValidator.analyzePassword(password)
        val matches = AuthValidator.doPasswordsMatch(password, _uiState.value.confirmPassword)
        _uiState.update {
            it.copy(
                newPassword = password,
                passwordStrength = strength,
                doPasswordsMatch = matches,
                screenState = ResetPasswordUiState.ScreenState.Idle
            )
        }
    }

    private fun onConfirmPasswordChanged(password: String) {
        val matches = AuthValidator.doPasswordsMatch(_uiState.value.newPassword, password)
        _uiState.update {
            it.copy(
                confirmPassword = password,
                doPasswordsMatch = matches,
                screenState = ResetPasswordUiState.ScreenState.Idle
            )
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, screenState = ResetPasswordUiState.ScreenState.Idle) }

            when (val result = resetPasswordUseCase(
                token = token,
                newPassword = state.newPassword,
                confirmPassword = state.confirmPassword
            )) {
                is ResetPasswordUseCase.Result.ValidationFailed -> {
                    val error = when (result.error) {
                        ResetPasswordUseCase.ValidationError.TokenBlank ->
                            ResetPasswordValidationError.NewPasswordBlank // token blank = fatal, map to generic
                        ResetPasswordUseCase.ValidationError.NewPasswordBlank ->
                            ResetPasswordValidationError.NewPasswordBlank
                        ResetPasswordUseCase.ValidationError.PasswordTooWeak ->
                            ResetPasswordValidationError.PasswordTooWeak
                        ResetPasswordUseCase.ValidationError.PasswordsDoNotMatch ->
                            ResetPasswordValidationError.PasswordsDoNotMatch
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            screenState = ResetPasswordUiState.ScreenState.ValidationError(error)
                        )
                    }
                }

                is ResetPasswordUseCase.Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, screenState = ResetPasswordUiState.ScreenState.Success)
                    }
                    // JWT cũ đã bị revoke bởi backend — navigate về Login (session mới cần thiết)
                    _effects.send(ResetPasswordEffect.NavigateToLogin)
                }

                is ResetPasswordUseCase.Result.Error -> {
                    when (result.throwable) {
                        // ── Token hết hạn / đã dùng / sai ────────────────────
                        is ResetTokenInvalidException -> {
                            _uiState.update {
                                it.copy(isLoading = false, screenState = ResetPasswordUiState.ScreenState.TokenExpired)
                            }
                        }

                        // ── Mọi lỗi còn lại (kể cả 401 revoke) ──────────────
                        else -> {
                            val msg = result.throwable.message ?: ""
                            if (msg.contains("401") || msg.contains("unauthorized", ignoreCase = true)) {
                                // JWT bị revoke sau khi đã reset từ device khác
                                _uiState.update { it.copy(isLoading = false) }
                                _effects.send(ResetPasswordEffect.ForceLogout)
                            } else {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        screenState = ResetPasswordUiState.ScreenState.NetworkError(
                                            msg.ifBlank { "Đã xảy ra lỗi. Vui lòng thử lại." }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── Factory ─────────────────────────────────────────────────────────────

    class Factory(
        private val token: String,
        private val resetPasswordUseCase: ResetPasswordUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            if (modelClass == ResetPasswordViewModel::class) {
                return ResetPasswordViewModel(token, resetPasswordUseCase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}")
        }
    }
}
