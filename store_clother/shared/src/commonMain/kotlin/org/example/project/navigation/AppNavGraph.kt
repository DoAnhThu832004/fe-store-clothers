package org.example.project.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.savedstate.read
import org.example.project.DeepLinkHandler
import org.example.project.core.di.AppContainer
import org.example.project.core.di.AppContainerHelper
import org.example.project.features.auth.forgot.ForgotPasswordRoute
import org.example.project.features.auth.forgot.ForgotPasswordViewModel
import org.example.project.features.auth.reset.ResetPasswordRoute
import org.example.project.features.auth.reset.ResetPasswordViewModel
import org.example.project.view.screen.admin.AdminDashboardScreen
import org.example.project.view.screen.intro.SplashScreen
import org.example.project.view.screen.login.LoginScreenRoute
import org.example.project.view.screen.pos.POSScreen
import org.example.project.view.screen.register.RegisterContactScreenRouter
import org.example.project.view.screen.register.RegisterScreenRoute
import org.example.project.view.screen.warehouse.WarehouseScreen
import org.example.project.viewmodel.LoginViewModel
import org.example.project.viewmodel.RegisterViewModel

/**
 * AppNavGraph đa nền tảng (KMP) trong Clean Architecture.
 * Nhận [AppContainer] và điều hướng độc lập với bất kỳ platform Android/iOS/Desktop/Web nào.
 */
@Composable
fun AppNavGraph(
    container: AppContainer = AppContainerHelper.current,
    navController: NavHostController = rememberNavController()
) {
    // ── iOS Deep Link listener (từ Swift qua DeepLinkHandler) ─────────────────────
    LaunchedEffect(Unit) {
        DeepLinkHandler.deepLinkFlow.collect { url ->
            val token = url.substringAfter("token=", "").substringBefore("&")
            if (token.isNotBlank()) {
                navController.navigate(Screen.ResetPassword.withToken(token))
            }
        }
    }

    // ── Intro + Auth Guard: Xác định startDestination ────────────────────────────
    val startDestination: String = when {
        // 1️⃣ Chưa xem Intro → vào SplashScreen
        !container.introStatusUseCase.hasSeenIntro() -> Screen.Splash.route

        // 2️⃣ Đã xem Intro, đã đăng nhập → vào thẳng màn hình theo role
        container.checkAuthStatusUseCase.isLoggedIn() -> {
            val roles = container.checkAuthStatusUseCase.getUserRoles()
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

        // ── Splash Screen ─────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onRegisterClicked = {
                    container.introStatusUseCase.markIntroSeen()
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onLoginClicked = {
                    container.introStatusUseCase.markIntroSeen()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Login Screen ──────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(
                    loginUseCase = container.loginUseCase,
                    authRepository = container.authRepository
                )
            )
            LoginScreenRoute(
                loginViewModel = loginViewModel,
                onNavigateToAdmin = {
                    navController.navigate(Screen.AdminDashboard.route) {
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

        // ── Register Screen ───────────────────────────────────────────────────
        composable(Screen.Register.route) { backStackEntry ->
            val registerViewModel: RegisterViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = RegisterViewModel.Factory(
                    registerTenantUseCase = container.registerTenantUseCase,
                    authRepository = container.authRepository
                )
            )
            RegisterScreenRoute(
                registerViewModel = registerViewModel,
                onRegisterContactClick = {
                    navController.navigate(Screen.RegisterContact.route)
                }
            )
        }

        composable(Screen.RegisterContact.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Register.route)
            }
            val registerViewModel: RegisterViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = RegisterViewModel.Factory(
                    registerTenantUseCase = container.registerTenantUseCase,
                    authRepository = container.authRepository
                )
            )
            RegisterContactScreenRouter(
                registerViewModel = registerViewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Admin Dashboard Screen ────────────────────────────────────────────
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onLogout = {
                    container.checkAuthStatusUseCase.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── POS Screen ────────────────────────────────────────────────────────
        composable(Screen.POS.route) {
            POSScreen(
                onLogout = {
                    container.checkAuthStatusUseCase.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Warehouse Screen ──────────────────────────────────────────────────
        composable(Screen.Warehouse.route) {
            WarehouseScreen(
                onLogout = {
                    container.checkAuthStatusUseCase.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Forgot Password Screen ────────────────────────────────────────────
        composable(Screen.ForgotPassword.route) {
            val forgotVM: ForgotPasswordViewModel = viewModel(
                factory = ForgotPasswordViewModel.Factory(
                    container.forgotPasswordUseCase
                )
            )
            ForgotPasswordRoute(
                viewModel = forgotVM,
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // ── Reset Password Screen ─────────────────────────────────────────────
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
            val token = backStackEntry.arguments?.read { getStringOrNull(Screen.ResetPassword.ARG_TOKEN) } ?: ""
            val resetVM: ResetPasswordViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = ResetPasswordViewModel.Factory(
                    token = token,
                    resetPasswordUseCase = container.resetPasswordUseCase
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
