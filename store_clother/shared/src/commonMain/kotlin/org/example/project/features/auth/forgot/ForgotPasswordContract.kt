package org.example.project.features.auth.forgot

/**
 * MVI Contract cho màn hình Quên Mật Khẩu.
 *
 * Theo nguyên tắc UDF (Unidirectional Data Flow):
 * - UI gửi [Intent] → ViewModel xử lý → emit [UiState] mới & [Effect] một lần.
 */

// ─── UI State ─────────────────────────────────────────────────────────────────

/**
 * Trạng thái đầy đủ của màn hình ForgotPassword.
 * UI rebuild hoàn toàn dựa vào object này — không có logic rải rác trong Composable.
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val storeCode: String = "",
    val isLoading: Boolean = false,
    val screenState: ScreenState = ScreenState.Idle
) {
    sealed interface ScreenState {
        /** Trạng thái mặc định — form trống, chờ user nhập */
        data object Idle : ScreenState

        /**
         * Gửi thành công — hiển thị thông báo "Kiểm tra hộp thư".
         * Backend luôn 200 OK kể cả email không tồn tại (chống User Enumeration).
         */
        data object Success : ScreenState

        /** Lỗi validation phía client */
        data class ValidationError(val error: ForgotPasswordValidationError) : ScreenState

        /** Lỗi network hoặc server */
        data class NetworkError(val message: String) : ScreenState

        /**
         * HTTP 429 — quá giới hạn 3 lần gửi trong 15 phút.
         * Hiển thị banner đặc biệt thay vì generic error.
         */
        data object RateLimitError : ScreenState
    }
}

// ─── Validation Error Types ──────────────────────────────────────────────────

sealed interface ForgotPasswordValidationError {
    data object EmailBlank : ForgotPasswordValidationError
    data object EmailInvalid : ForgotPasswordValidationError
    data object StoreCodeBlank : ForgotPasswordValidationError
}

// ─── User Intents ─────────────────────────────────────────────────────────────

/** Tất cả hành động người dùng có thể thực hiện trên màn này */
sealed interface ForgotPasswordIntent {
    data class EmailChanged(val email: String) : ForgotPasswordIntent
    data class StoreCodeChanged(val storeCode: String) : ForgotPasswordIntent
    data object SubmitClicked : ForgotPasswordIntent
    data object ErrorDismissed : ForgotPasswordIntent
}

// ─── One-time Side Effects ────────────────────────────────────────────────────

/** Sự kiện một lần — KHÔNG lưu trong UiState để tránh re-trigger khi recompose */
sealed interface ForgotPasswordEffect {
    /** Không dùng trong version hiện tại — dự phòng cho analytics/logging */
    data object RequestSentAnalytics : ForgotPasswordEffect
}
