package org.example.project.view.screen.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.example.project.viewmodel.RegisterViewModel

@Composable
fun RegisterScreenRoute(
    registerViewModel: RegisterViewModel,
    onRegisterContactClick: () -> Unit
) {
    val registerState by registerViewModel.registerState.collectAsState()
    RegisterScreen(
        registerState = registerState,
        onStoreNameChanged = { registerViewModel.OnStoreNameChanged(it) },
        onStoreCodeChanged = { registerViewModel.onStoreCodeChanged(it) },
        onUserNameChanged = { registerViewModel.onUserNameChanged(it) },
        onPasswordChanged = { registerViewModel.onPasswordChanged(it) },
        onFullNameChanged = { registerViewModel.onFullNameChanged(it) },
        onPasswordVisibilityChanged = { registerViewModel.onPasswordVisibilityChanged() },
        onErrorMessageShown = { registerViewModel.onErrorMessageShown() },
        onRegisterContactClick = {
            if(registerViewModel.validateStep1()) {
                onRegisterContactClick()
            }
        }
    )
}