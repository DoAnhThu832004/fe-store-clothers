package org.example.project.viewmodel

/**
 * Định nghĩa tất cả loại lỗi có thể xảy ra trên màn hình Login.
 *
 * ## Lý do dùng sealed class thay vì String
 *
 * - **Testability**: ViewModel emit `LoginError` thuần Kotlin — Unit Test không cần
 *   Context, không cần Compose runtime, không cần mock getString.
 * - **Localization**: Việc map `LoginError → String` được giao cho UI layer (Composable),
 *   nơi duy nhất có thể gọi `stringResource()` một cách hợp lệ.
 * - **Exhaustive**: `when (error)` buộc compile-time kiểm tra mọi case —
 *   không bao giờ bỏ sót lỗi mới thêm vào.
 * - **Separation of Concerns**: ViewModel không biết gì về ngôn ngữ hiển thị —
 *   đúng nguyên tắc Clean Architecture.
 *
 * ## Phân loại
 * - [Validation]: Lỗi validate input phía client, không cần gọi API.
 * - [Network]: Lỗi từ tầng network sau khi gọi API.
 */
sealed interface LoginError {

    // ── Validation Errors ────────────────────────────────────────────────────

    /** Store code để trống */
    data object EmptyStoreCode : LoginError

    data object EmptyStoreName : LoginError

    /** Username để trống */
    data object EmptyUsername : LoginError

    /** Password để trống */
    data object EmptyPassword : LoginError
    data object EmptyFullName : LoginError
    data object EmptyEmail : LoginError
    data object EmptyPhone : LoginError
    data object EmptyEmailContact : LoginError
    data object EmptyPhoneContact : LoginError

    // ── Network / Server Errors ──────────────────────────────────────────────

    /** Sai username hoặc password */
    data object InvalidCredentials : LoginError

    /** Server không phản hồi hoặc timeout */
    data object NetworkUnavailable : LoginError

    /** Lỗi không xác định từ server — kèm message gốc để log */
    data class Unknown(val rawMessage: String?) : LoginError
}
