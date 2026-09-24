package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.common.Resource
import org.example.project.domain.repository.IAuthRepository

// ─── One-time Navigation Event ───────────────────────────────────────────────

/**
 * Sự kiện điều hướng một lần (One-time Event) sau khi đăng nhập thành công.
 *
 * ViewModel là nơi duy nhất chứa logic mapping role → destination.
 * UI (LoginScreenRoute) chỉ collect và thi hành — hoàn toàn "dumb".
 *
 * Dùng [SharedFlow] thay vì StateFlow để tránh re-emit khi recompose.
 */
sealed class LoginNavigationEvent {
    /** ROLE_OWNER hoặc ROLE_MANAGER → màn hình quản trị */
    data object ToAdminDashboard : LoginNavigationEvent()

    /** ROLE_CASHIER → màn hình bán hàng (POS) */
    data object ToPOS : LoginNavigationEvent()

    /** ROLE_WAREHOUSE_STAFF → màn hình quản lý kho */
    data object ToWarehouse : LoginNavigationEvent()
}

// ─── UI State ────────────────────────────────────────────────────────────────

/**
 * Trạng thái toàn diện của màn hình Login.
 *
 * [error] là [LoginError] thuần Kotlin, không chứa String.
 * UI layer tự map [LoginError] → string resource — đảm bảo localization đúng chỗ.
 */
data class LoginUiState(
    val storeCode: String = "",
    val userName: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: LoginError? = null,           // ← thay thế errorMessage: String?
    val loginResult: LoginResult = LoginResult.Idle
)

/**
 * Kết quả sau khi gọi API login — dùng để drive UI state (loading indicator, v.v.).
 * Logic điều hướng KHÔNG nằm ở đây; xem [LoginNavigationEvent].
 *
 * [Loading] đã bị xoá: login() là one-shot suspend function, không trả về Flow.
 * Trạng thái loading được quản lý riêng qua [LoginUiState.isLoading].
 */
sealed class LoginResult {
    /** Trạng thái mặc định — chưa có hành động nào */
    data object Idle : LoginResult()

    /**
     * Đăng nhập thành công.
     * Không carry roles — navigation đã được xử lý qua [LoginNavigationEvent].
     */
    data object Success : LoginResult()

    /**
     * Đăng nhập thất bại.
     * Mang [LoginError] thay vì String — UI tự map sang localized string.
     */
    data class Error(val error: LoginError) : LoginResult()
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

/**
 * LoginViewModel quản lý toàn bộ business logic cho màn hình Login.
 *
 * ### Thiết kế cho Testability
 * - Không import bất kỳ thứ gì từ Compose, Android Context, hay string resource.
 * - Chỉ depend vào [IAuthRepository] (interface) → mock dễ dàng trong Unit Test.
 * - Error được emit dưới dạng [LoginError] sealed class — assert trong test không cần UI.
 *
 * ### Flow hoạt động
 * 1. UI gọi [login]
 * 2. ViewModel validate input → emit [LoginError] nếu lỗi
 * 3. ViewModel gọi [IAuthRepository.login]
 * 4. Nếu thành công → emit [LoginNavigationEvent] qua SharedFlow
 * 5. Nếu thất bại → emit [LoginError] qua UiState
 * 6. UI observe [loginUiState] để render, collect [navigationEvent] để navigate
 *
 * @param authRepository Interface repository — inject từ Factory, mock trong test
 */
class LoginViewModel(
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    /**
     * One-time Event channel cho navigation.
     *
     * - [MutableSharedFlow] với extraBufferCapacity = 1: không drop event nếu collector
     *   chưa kịp collect (tránh mất event khi Composition chưa active).
     * - replay = 0 (mặc định): tránh re-navigate khi recompose hoặc config change.
     */
    private val _navigationEvent = MutableSharedFlow<LoginNavigationEvent>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<LoginNavigationEvent> = _navigationEvent.asSharedFlow()

    // ─── Input handlers ──────────────────────────────────────────────────────

    fun onStoreCodeChanged(storeCode: String) {
        _loginUiState.update { it.copy(storeCode = storeCode, error = null) }
    }

    fun onUserNameChanged(userName: String) {
        _loginUiState.update { it.copy(userName = userName, error = null) }
    }

    fun onPasswordChanged(password: String) {
        _loginUiState.update { it.copy(password = password, error = null) }
    }

    fun onPasswordVisibilityChanged() {
        _loginUiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /** Gọi sau khi UI đã hiển thị lỗi (Snackbar dismiss) → xoá error khỏi state */
    fun onErrorShown() {
        _loginUiState.update { it.copy(error = null, loginResult = LoginResult.Idle) }
    }

    // ─── Login action ────────────────────────────────────────────────────────

    /**
     * Trigger đăng nhập.
     *
     * **Validate** input phía client trước, emit [LoginError.Validation] nếu thiếu.
     * **Gọi API** và emit [LoginNavigationEvent] nếu thành công,
     * hoặc [LoginError.Network] nếu thất bại.
     *
     * Dùng `emit` (suspend) thay vì `tryEmit` để đảm bảo
     * event không bao giờ bị rớt — an toàn tuyệt đối trong coroutine scope.
     */
    fun login() {
        val state = _loginUiState.value
        if (state.isLoading) return

        viewModelScope.launch {

            // ── Validate input ────────────────────────────────────────────
            val validationError = when {
                state.storeCode.isBlank() -> LoginError.EmptyStoreCode
                state.userName.isBlank()  -> LoginError.EmptyUsername
                state.password.isBlank()  -> LoginError.EmptyPassword
                else                      -> null
            }

            if (validationError != null) {
                _loginUiState.update { it.copy(error = validationError) }
                return@launch
            }

            // ── Gọi API ───────────────────────────────────────────────────
            _loginUiState.update {
                it.copy(isLoading = true, error = null)
            }

            when (val result = authRepository.login(
                storeCode = state.storeCode,
                username  = state.userName.trim(),
                password  = state.password
            )) {
                is Resource.Success -> {
                    val roles = result.data.roles

                    // Mapping role → navigation event (business rule nằm ở ViewModel)
                    val event = when {
                        roles.contains("ROLE_OWNER") || roles.contains("ROLE_MANAGER") ->
                            LoginNavigationEvent.ToAdminDashboard
                        roles.contains("ROLE_CASHIER") ->
                            LoginNavigationEvent.ToPOS
                        roles.contains("ROLE_WAREHOUSE_STAFF") ->
                            LoginNavigationEvent.ToWarehouse
                        else ->
                            LoginNavigationEvent.ToAdminDashboard
                    }

                    // emit() thay vì tryEmit() — suspend, không bao giờ rớt event
                    _navigationEvent.emit(event)

                    _loginUiState.update {
                        it.copy(isLoading = false, loginResult = LoginResult.Success)
                    }
                }

                is Resource.Error -> {
                    // Map Throwable → LoginError — không hardcode String ở ViewModel
                    val error = mapThrowableToLoginError(result.throwable)
                    _loginUiState.update {
                        it.copy(
                            isLoading = false,
                            error = error,
                            loginResult = LoginResult.Error(error)
                        )
                    }
                }

                // Resource.Loading không bao giờ được emit từ một one-shot suspend fun.
                // Exhaustive when yêu cầu handle case này nhưng thực thi không có gì.
                is Resource.Loading -> Unit
            }
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Chuyển [Throwable] từ network layer → [LoginError] có nghĩa với UI.
     *
     * Đặt ở đây thay vì ở Repository để:
     * - Repository không biết context hiển thị (error nào cần show vs log)
     * - ViewModel quyết định cách trình bày lỗi cho đúng màn hình
     */
    private fun mapThrowableToLoginError(throwable: Throwable): LoginError {
        val message = throwable.message?.lowercase() ?: ""
        return when {
            // Các lỗi mạng — message được set bởi AuthRepository
            message.contains("internet")
                || message.contains("connect")
                || message.contains("timeout")
                || message.contains("network") -> LoginError.NetworkUnavailable

            // Sai credentials — HTTP 401
            message.contains("sai") || message.contains("unauthorized") ->
                LoginError.InvalidCredentials

            else -> LoginError.Unknown(throwable.message)
        }
    }

    // ─── Factory ─────────────────────────────────────────────────────────────

    /**
     * ViewModelProvider.Factory để inject [IAuthRepository] vào ViewModel.
     * Cần thiết vì ViewModel không nên tự khởi tạo dependency.
     */
    class Factory(private val authRepository: IAuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            if (modelClass == LoginViewModel::class) {
                return LoginViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}")
        }
    }
}