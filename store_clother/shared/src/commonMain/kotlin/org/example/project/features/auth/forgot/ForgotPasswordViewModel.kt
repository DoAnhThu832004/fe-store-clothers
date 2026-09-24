package org.example.project.features.auth.forgot

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
import org.example.project.domain.repository.RateLimitException
import org.example.project.domain.usecase.ForgotPasswordUseCase
import kotlin.reflect.KClass

/**
 * ViewModel cho màn hình Quên Mật Khẩu.
 *
 * ### Kiến trúc MVI / UDF:
 * - UI gọi [processIntent] → ViewModel xử lý → emit [uiState] + [effects]
 * - [uiState]: [StateFlow] — UI subscribe, rebuild theo state mới nhất
 * - [effects]: [Channel] → [receiveAsFlow] — đảm bảo each effect được consume đúng 1 lần
 *
 * ### Xử lý đặc biệt:
 * - HTTP 429 ([RateLimitException]) → [ForgotPasswordUiState.ScreenState.RateLimitError]
 *   hiển thị banner đặc biệt thay vì generic error
 * - Backend luôn 200 OK → không phân biệt email tồn tại hay không (chống User Enumeration)
 */
class ForgotPasswordViewModel(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    /**
     * Channel cho one-time effects.
     * Capacity = Channel.BUFFERED để không drop event nếu UI chưa kịp collect.
     */
    private val _effects = Channel<ForgotPasswordEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // ─── Intent Handler (entry point từ UI) ──────────────────────────────────

    fun processIntent(intent: ForgotPasswordIntent) {
        when (intent) {
            is ForgotPasswordIntent.EmailChanged ->
                _uiState.update { it.copy(email = intent.email, screenState = ForgotPasswordUiState.ScreenState.Idle) }

            is ForgotPasswordIntent.StoreCodeChanged ->
                _uiState.update { it.copy(storeCode = intent.storeCode, screenState = ForgotPasswordUiState.ScreenState.Idle) }

            is ForgotPasswordIntent.SubmitClicked -> submit()

            is ForgotPasswordIntent.ErrorDismissed ->
                _uiState.update { it.copy(screenState = ForgotPasswordUiState.ScreenState.Idle) }
        }
    }

    // ─── Private Logic ────────────────────────────────────────────────────────

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, screenState = ForgotPasswordUiState.ScreenState.Idle) }

            when (val result = forgotPasswordUseCase(email = state.email, storeCode = state.storeCode)) {

                is ForgotPasswordUseCase.Result.ValidationFailed -> {
                    val error = when (result.error) {
                        ForgotPasswordUseCase.ValidationError.EmailBlank ->
                            ForgotPasswordValidationError.EmailBlank
                        ForgotPasswordUseCase.ValidationError.EmailInvalid ->
                            ForgotPasswordValidationError.EmailInvalid
                        ForgotPasswordUseCase.ValidationError.StoreCodeBlank ->
                            ForgotPasswordValidationError.StoreCodeBlank
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            screenState = ForgotPasswordUiState.ScreenState.ValidationError(error)
                        )
                    }
                }

                is ForgotPasswordUseCase.Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, screenState = ForgotPasswordUiState.ScreenState.Success)
                    }
                    _effects.send(ForgotPasswordEffect.RequestSentAnalytics)
                }

                is ForgotPasswordUseCase.Result.Error -> {
                    val newState = when (result.throwable) {
                        // ── HTTP 429: Rate Limit ──────────────────────────────
                        is RateLimitException ->
                            ForgotPasswordUiState.ScreenState.RateLimitError

                        // ── Mọi lỗi khác: network / server ───────────────────
                        else ->
                            ForgotPasswordUiState.ScreenState.NetworkError(
                                result.throwable.message ?: "Đã xảy ra lỗi. Vui lòng thử lại."
                            )
                    }
                    _uiState.update { it.copy(isLoading = false, screenState = newState) }
                }
            }
        }
    }

    // ─── Factory (Manual DI) ─────────────────────────────────────────────────

    class Factory(
        private val forgotPasswordUseCase: ForgotPasswordUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            if (modelClass == ForgotPasswordViewModel::class) {
                return ForgotPasswordViewModel(forgotPasswordUseCase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}")
        }
    }
}
