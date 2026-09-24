package org.example.project.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object AdminDashboard : Screen("admin_dashboard")
    data object POS : Screen("pos")
    data object Warehouse : Screen("warehouse")
    data object Register : Screen("register")
    data object RegisterContact : Screen("register_contact")

    /** Màn hình Quên Mật Khẩu */
    data object ForgotPassword : Screen("forgot_password")

    /**
     * Màn hình Đặt Lại Mật Khẩu — nhận [token] từ Deep Link argument.
     *
     * Route: `reset_password?token={token}`
     * Deep Link Android:  `storeclothes://reset-password?token={token}`
     * Universal Link iOS: `https://yourdomain.com/reset-password?token={token}`
     */
    data object ResetPassword : Screen("reset_password?token={token}") {
        const val ARG_TOKEN = "token"
        /** Tạo route cụ thể với token thực — dùng khi navigate programmatically */
        fun withToken(token: String) = "reset_password?token=$token"
    }
}