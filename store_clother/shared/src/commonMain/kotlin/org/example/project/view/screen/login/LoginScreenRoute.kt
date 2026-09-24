package org.example.project.view.screen.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.example.project.viewmodel.LoginNavigationEvent
import org.example.project.viewmodel.LoginViewModel

/**
 * Route Composable — kết nối ViewModel với UI.
 *
 * Tách biệt khỏi [LoginScreen] để:
 * - [LoginScreen] thuần stateless, dễ test và preview
 * - Route collect [LoginViewModel.navigationEvent] và dispatch callback tương ứng
 *
 * **Dumb UI principle**: Route không biết gì về role strings hay business rule.
 * Toàn bộ logic mapping role → destination đã được đẩy vào ViewModel.
 * Route chỉ "nghe" và "thi hành" event từ ViewModel.
 *
 * @param loginViewModel        ViewModel được inject từ NavGraph (có AuthRepository)
 * @param onNavigateToAdmin     Callback điều hướng sang AdminDashboard
 * @param onNavigateToPOS       Callback điều hướng sang PosScreen (CASHIER)
 * @param onNavigateToWarehouse Callback điều hướng sang WarehouseScreen (WAREHOUSE_STAFF)
 */
@Composable
fun LoginScreenRoute(
    loginViewModel: LoginViewModel,
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToPOS: () -> Unit = {},
    onNavigateToWarehouse: () -> Unit = {},
    onRegisterClicked: () -> Unit = {},
    onForgotPasswordClicked: () -> Unit = {},
) {
    val loginState by loginViewModel.loginUiState.collectAsState()

    // Collect One-time navigation event từ ViewModel.
    // Dùng LaunchedEffect(Unit) để coroutine sống cùng Composition,
    // không bị restart khi state thay đổi — tránh miss event.
    LaunchedEffect(Unit) {
        loginViewModel.navigationEvent.collect { event ->
            when (event) {
                is LoginNavigationEvent.ToAdminDashboard -> onNavigateToAdmin()
                is LoginNavigationEvent.ToPOS           -> onNavigateToPOS()
                is LoginNavigationEvent.ToWarehouse     -> onNavigateToWarehouse()
            }
        }
    }

    LoginScreen(
        loginState = loginState,
        onStoreCodeChanged = { loginViewModel.onStoreCodeChanged(it) },
        onUserNameChanged = { loginViewModel.onUserNameChanged(it) },
        onPasswordChanged = { loginViewModel.onPasswordChanged(it) },
        onPasswordVisibilityChanged = { loginViewModel.onPasswordVisibilityChanged() },
        onLoginClicked = { loginViewModel.login() },
        onErrorShown = { loginViewModel.onErrorShown() },
        onRegisterClicked = onRegisterClicked,
        onForgotPasswordClicked = onForgotPasswordClicked
    )
}