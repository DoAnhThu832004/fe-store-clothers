package org.example.project.view.screen.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.example.project.viewmodel.RegisterNavigationEvent
import org.example.project.viewmodel.RegisterViewModel

@Composable
fun RegisterContactScreenRouter(
    registerViewModel: RegisterViewModel,
    onNavigateToLogin: () -> Unit
) {
    val registerState by registerViewModel.registerState.collectAsState()
    LaunchedEffect(Unit) {
        registerViewModel.navigationEvent.collect { event ->
            when (event) {
                is RegisterNavigationEvent.ToLogin -> onNavigateToLogin()
            }
        }
    }
    RegisterContactScreen(
        registerState = registerState,
        onContactEmailChanged = { registerViewModel.onContactEmailChanged(it) },
        onContactPhoneChanged = { registerViewModel.onContactPhoneChanged(it) },
        onEmailChanged = { registerViewModel.onEmailChanged(it) },
        onPhoneChanged = { registerViewModel.onPhoneChanged(it) },
        onRegisterClick = { registerViewModel.registerTenants() },
        onErrorMessageShown = { registerViewModel.onErrorMessageShown() }
    )
}