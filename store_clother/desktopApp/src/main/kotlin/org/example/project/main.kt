package org.example.project

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.project.di.DesktopAppContainer
import java.awt.Dimension

fun main() = application {
    val container = DesktopAppContainer.init()

    val windowState = rememberWindowState(
        width = 1100.dp,
        height = 750.dp,
        position = WindowPosition(Alignment.Center)
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Store Clother - Quản lý chuỗi cửa hàng thời trang",
    ) {
        window.minimumSize = Dimension(820, 580)
        App(container = container)
    }
}