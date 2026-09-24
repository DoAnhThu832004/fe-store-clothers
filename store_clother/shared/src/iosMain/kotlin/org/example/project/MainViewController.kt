package org.example.project

import androidx.compose.ui.window.ComposeUIViewController

/**
 * iOS Entry Point — tạo UIViewController từ Compose Multiplatform.
 *
 * Deep Link Flow:
 * iOS Swift `ContentView.onOpenURL` → `DeepLinkHandler.handleDeepLink(urlString)`
 * → `DeepLinkHandler.deepLinkFlow` → NavController.navigate()
 *
 * Việc consume [DeepLinkHandler.deepLinkFlow] được thực hiện trong NavHost
 * của Android App (qua navDeepLink{}) và nên được tích hợp vào iOS NavHost
 * khi iOS có navigation riêng trong tương lai.
 */
fun MainViewController() = ComposeUIViewController { App() }