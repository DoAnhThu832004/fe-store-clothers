package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.common.Resource
import org.example.project.data.remote.dto.RegisterRequestDto
import org.example.project.domain.repository.IAuthRepository
import kotlin.reflect.KClass

sealed class RegisterNavigationEvent {
    /** Đăng ký thành công → Chuyển người dùng về màn hình Login để đăng nhập */
    data object ToLogin : RegisterNavigationEvent()
}
data class RegisterState(
    val storeName: String = "",
    val storeCode: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val ownerUsername: String = "",
    val ownerPassword: String = "",
    val ownerFullName: String = "",
    val ownerEmail: String = "",
    val ownerPhone: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: LoginError? = null,
    val registerResult: RegisterResult = RegisterResult.Idle
)
sealed class RegisterResult {
    data object Idle : RegisterResult()
    data object Loading : RegisterResult()
    data object Success : RegisterResult()
    data class Error(val message: LoginError) : RegisterResult()
}
class RegisterViewModel(
    private val authRepository: IAuthRepository
): ViewModel() {
    private val _registerState = MutableStateFlow(RegisterState())
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()
    private val _navigationEvent = MutableSharedFlow<RegisterNavigationEvent>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<RegisterNavigationEvent> = _navigationEvent.asSharedFlow()


    fun OnStoreNameChanged(storeName: String) {
        _registerState.update { it.copy(storeName = storeName) }
    }
    fun onStoreCodeChanged(storeCode: String) {
        _registerState.update { it.copy(storeCode = storeCode) }
    }
    fun onContactEmailChanged(contactEmail: String) {
        _registerState.update { it.copy(contactEmail = contactEmail) }
    }
    fun onContactPhoneChanged(contactPhone: String) {
        _registerState.update { it.copy(contactPhone = contactPhone) }
    }
    fun onUserNameChanged(ownerUsername: String) {
        _registerState.update { it.copy(ownerUsername = ownerUsername) }
    }
    fun onPasswordChanged(ownerPassword: String) {
        _registerState.update { it.copy(ownerPassword = ownerPassword) }
    }
    fun onFullNameChanged(ownerFullName: String) {
        _registerState.update { it.copy(ownerFullName = ownerFullName) }
    }
    fun onEmailChanged(ownerEmail: String) {
        _registerState.update { it.copy(ownerEmail = ownerEmail) }
    }
    fun onPhoneChanged(ownerPhone: String) {
        _registerState.update { it.copy(ownerPhone = ownerPhone) }
    }
    fun onPasswordVisibilityChanged() {
        _registerState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }
    fun onErrorMessageShown() {
        _registerState.update { it.copy(errorMessage = null, registerResult = RegisterResult.Idle) }
    }
    fun validateStep1(): Boolean {
        val state = _registerState.value
        val error = when {
            state.storeName.isBlank()     -> LoginError.EmptyStoreName
            state.storeCode.isBlank()     -> LoginError.EmptyStoreCode
            state.ownerUsername.isBlank() -> LoginError.EmptyUsername
            state.ownerPassword.isBlank() -> LoginError.EmptyPassword
            state.ownerFullName.isBlank() -> LoginError.EmptyFullName
            else                          -> null
        }
        if (error != null) {
            _registerState.update { it.copy(errorMessage = error) }
            return false
        }
        return true
    }
    fun registerTenants() {
        val state = _registerState.value
        if (state.isLoading) return
        val step2Error = when {
            state.contactEmail.isBlank() -> LoginError.EmptyEmailContact
            state.contactPhone.isBlank() -> LoginError.EmptyPhoneContact
            state.ownerEmail.isBlank()   -> LoginError.EmptyEmail
            state.ownerPhone.isBlank()   -> LoginError.EmptyPhone
            else                         -> null
        }
        if (step2Error != null) {
            _registerState.update { it.copy(errorMessage = step2Error) }
            return
        }
        val requestDto = RegisterRequestDto(
            storeName = state.storeName.trim(),
            storeCode = state.storeCode.trim(),
            contactEmail = state.contactEmail.trim(),
            contactPhone = state.contactPhone.trim(),
            ownerUsername = state.ownerUsername.trim(),
            ownerPassword = state.ownerPassword,
            ownerFullName = state.ownerFullName.trim(),
            ownerEmail = state.ownerEmail.trim(),
            ownerPhone = state.ownerPhone.trim()
        )
        viewModelScope.launch {
            // Bắt đầu loading
            _registerState.update {
                it.copy(isLoading = true, errorMessage = null, registerResult = RegisterResult.Loading)
            }
            when (val result = authRepository.registerTenant(requestDto)) {
                is Resource.Success -> {
                    // Cập nhật state thành công
                    _registerState.update {
                        it.copy(isLoading = false, registerResult = RegisterResult.Success)
                    }

                    // Emit sự kiện điều hướng về Login
                    _navigationEvent.tryEmit(RegisterNavigationEvent.ToLogin)
                }
                is Resource.Error -> {
                    // Lỗi (Ví dụ trùng mã cửa hàng, sai định dạng...)
                    // Chú ý: Ở đây bạn đang dùng result.throwable (theo code Login của bạn) thay vì result.exception
                    val message = mapThrowableTRegisterError(result.throwable)
                    _registerState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = message,
                            registerResult = RegisterResult.Error(message)
                        )
                    }
                }
                Resource.Loading -> Unit
            }
        }
    }
    private fun mapThrowableTRegisterError(throwable: Throwable): LoginError {
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
    class Factory(private val authRepository: IAuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            if (modelClass == RegisterViewModel::class) {
                return RegisterViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}")
        }
    }
}