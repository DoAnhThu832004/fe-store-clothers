package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * App() hiện chỉ dùng cho preview trên các platform khác (Desktop, iOS, Web).
 * Trên Android, entry point thực sự là MainActivity → AppNavGraph.
 *
 * Nếu cần preview nhanh một màn hình cụ thể, thay đổi nội dung bên trong
 * MaterialTheme { ... } tại đây.
 */
@Composable
@Preview
fun App() {
    org.example.project.ui.theme.AppTheme {
        // Preview placeholder — trên Android thực tế dùng AppNavGraph trong MainActivity
    }
}