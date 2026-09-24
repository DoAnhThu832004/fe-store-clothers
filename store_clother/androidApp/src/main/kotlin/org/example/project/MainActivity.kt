package org.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import org.example.project.local.AppPreferences
import org.example.project.local.SessionManager
import org.example.project.navigation.AppNavGraph
import org.example.project.network.RetrofitClient
import org.example.project.network.AuthApiService
import org.example.project.repository.AuthRepository

/**
 * Entry point duy nhất của ứng dụng Android.
 *
 * Trách nhiệm:
 * - Khởi tạo dependency graph thủ công (manual DI):
 *     SessionManager → AuthApiService → AuthRepository
 * - Truyền các dependency vào [AppNavGraph]
 *
 * Lưu ý: Khi dự án lớn hơn, nên chuyển sang Hilt/Koin để quản lý DI tốt hơn.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ── Khởi tạo dependencies ──────────────────────────────────────────
        val appPreferences  = AppPreferences(applicationContext)
        val sessionManager  = SessionManager(applicationContext)
        // ⚠️ Phải init trước khi tạo service — cấp SessionManager cho interceptors
        RetrofitClient.init(sessionManager)
        val authApiService  = RetrofitClient.createService<AuthApiService>()
        val authRepository  = AuthRepository(authApiService, sessionManager)

        setContent {
            org.example.project.ui.theme.AppTheme {
                AppNavGraph(
                    appPreferences = appPreferences,
                    sessionManager = sessionManager,
                    authRepository = authRepository
                )
            }
        }
    }
}