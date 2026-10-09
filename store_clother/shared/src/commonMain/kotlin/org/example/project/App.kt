package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.core.di.AppContainer
import org.example.project.core.di.AppContainerHelper
import org.example.project.navigation.AppNavGraph
import org.example.project.ui.theme.AppTheme

/**
 * Entry point dùng chung cho toàn bộ các platform KMP (Android, iOS, Desktop, Web).
 * Khởi chạy [AppTheme] và [AppNavGraph] với Clean Architecture [AppContainer].
 */
@Composable
@Preview
fun App(
    container: AppContainer = AppContainerHelper.current
) {
    AppTheme {
        AppNavGraph(container = container)
    }
}