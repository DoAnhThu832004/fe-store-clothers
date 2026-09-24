package org.example.project.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import org.example.project.domain.repository.IAuthRepository
import org.example.project.domain.usecase.ForgotPasswordUseCase
import org.example.project.domain.usecase.ResetPasswordUseCase
import org.example.project.features.auth.forgot.ForgotPasswordRoute
import org.example.project.features.auth.forgot.ForgotPasswordViewModel
import org.example.project.features.auth.reset.ResetPasswordRoute
import org.example.project.features.auth.reset.ResetPasswordViewModel
import org.example.project.local.AppPreferences
import org.example.project.local.SessionManager
import org.example.project.view.screen.admin.AdminDashboardScreen
import org.example.project.view.screen.login.LoginScreenRoute
import org.example.project.view.screen.register.RegisterScreenRoute
import org.example.project.view.screen.intro.SplashScreen
import org.example.project.view.screen.pos.POSScreen
import org.example.project.view.screen.register.RegisterContactScreenRouter
import org.example.project.view.screen.warehouse.WarehouseScreen
import org.example.project.viewmodel.LoginViewModel
import org.example.project.viewmodel.RegisterViewModel


@Composable
fun AppNavGraph(
    appPreferences: AppPreferences,
    sessionManager: SessionManager,
    authRepository: IAuthRepository,
    navController: NavHostController = rememberNavController()
) {
    // ── Intro + Auth Guard: Xác định startDestination ────────────────────────────
    val startDestination: String = when {
        // 1️⃣ Chưa bao giờ xem Intro → bắt buộc qua SplashScreen (chỉ lần đầu)
        !appPreferences.hasSeenIntro() -> Screen.Splash.route

        // 2️⃣ Đã xem Intro, đã đăng nhập → vào thẳng màn hình theo role
        sessionManager.isLoggedIn() -> {
            val roles = sessionManager.getUserRoles()
            when {
                roles.contains("ROLE_OWNER") || roles.contains("ROLE_MANAGER") ->
                    Screen.AdminDashboard.route
                roles.contains("ROLE_CASHIER") ->
                    Screen.POS.route
                roles.contains("ROLE_WAREHOUSE_STAFF") ->
                    Screen.Warehouse.route
                else -> Screen.Login.route
            }
        }

        // 3️⃣ Đã xem Intro, chưa đăng nhập → Login
        else -> Screen.Login.route
    }

    // ── NavHost ───────────────────────────────────────────────────────────
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {

        // ── Splash Screen (chỉ hiển thị lần đầu khi cài app) ────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onRegisterClicked = {
                    // Đánh dấu đã xem Intro rồi — không bao giờ hiển lại
                    appPreferences.markIntroSeen()
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onLoginClicked = {
                    // Đánh dấu đã xem Intro rồi — không bao giờ hiển lại
                    appPreferences.markIntroSeen()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Login Screen ─────────────────────────────────────────────────
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(authRepository)
            )
            LoginScreenRoute(
                loginViewModel = loginViewModel,
                onNavigateToAdmin = {
                    navController.navigate(Screen.AdminDashboard.route) {
                        // Xoá Login khỏi backstack — user không thể Back về Login
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToPOS = {
                    navController.navigate(Screen.POS.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToWarehouse = {
                    navController.navigate(Screen.Warehouse.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegisterClicked = {
                    navController.navigate(Screen.Register.route)
                },
                onForgotPasswordClicked = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }
        composable(Screen.Register.route) { backStackEntry ->
            val registerViewModel: RegisterViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = RegisterViewModel.Factory(authRepository)
            )
            RegisterScreenRoute(
                registerViewModel = registerViewModel,
                onRegisterContactClick = {
                    navController.navigate(Screen.RegisterContact.route)
                }
            )
        }
        composable(Screen.RegisterContact.route) {  backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Register.route)
            }
            val registerViewModel: RegisterViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = RegisterViewModel.Factory(authRepository)
            )
            RegisterContactScreenRouter(
                registerViewModel = registerViewModel,
                onNavigateToLogin = {

                }
            )
        }

        // ── Admin Dashboard Screen (ROLE_OWNER, ROLE_MANAGER) ────────────
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── POS Screen (ROLE_CASHIER) ────────────────────────────────────
        composable(Screen.POS.route) {
            POSScreen(
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Warehouse Screen (ROLE_WAREHOUSE_STAFF) ──────────────────────
        composable(Screen.Warehouse.route) {
            WarehouseScreen(
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Forgot Password Screen ────────────────────────────────────────
        composable(Screen.ForgotPassword.route) {
            val forgotVM: ForgotPasswordViewModel = viewModel(
                factory = ForgotPasswordViewModel.Factory(
                    ForgotPasswordUseCase(authRepository)
                )
            )
            ForgotPasswordRoute(
                viewModel = forgotVM,
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // ── Reset Password Screen (Deep Link entry point) ─────────────────
        //
        // Deep Links bắt:
        //   - Custom scheme:    storeclothes://reset-password?token={token}
        //   - HTTPS (App Link): https://yourdomain.com/reset-password?token={token}
        //
        // token được truyền qua Query Param — khớp với cấu trúc URL trong email.
        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument(Screen.ResetPassword.ARG_TOKEN) {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = false
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "storeclothes://reset-password?token={token}"
                },
                navDeepLink {
                    uriPattern = "https://yourdomain.com/reset-password?token={token}"
                }
            )
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString(Screen.ResetPassword.ARG_TOKEN) ?: ""
            val resetVM: ResetPasswordViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = ResetPasswordViewModel.Factory(
                    token = token,
                    resetPasswordUseCase = ResetPasswordUseCase(authRepository)
                )
            )
            ResetPasswordRoute(
                viewModel = resetVM,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route) {
                        popUpTo(Screen.ResetPassword.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

// ─── Placeholder Screens ─────────────────────────────────────────────────────

/**
 * Placeholder đơn giản cho các màn hình đích.
 * Sẽ được thay thế bằng UI thực tế trong các Sprint sau.
 */
@Composable
private fun PlaceholderScreen(label: String, description: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
