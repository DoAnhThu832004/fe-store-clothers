package org.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import org.example.project.di.AndroidAppContainer

/**
 * Entry point duy nhất của ứng dụng Android.
 * Khởi tạo [AndroidAppContainer] để cung cấp Data Sources (Retrofit, EncryptedSharedPreferences)
 * cho KMP Clean Architecture và kích hoạt giao diện chung [App].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val container = AndroidAppContainer.init(applicationContext)

        setContent {
            App(container = container)
        }
    }
}