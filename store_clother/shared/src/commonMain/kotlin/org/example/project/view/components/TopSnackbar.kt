package org.example.project.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Custom Snackbar hiển thị ở **đầu màn hình** với animation slide từ phải sang trái.
 *
 * **Animation flow**:
 * - Enter: slide từ ngoài phải → vào (slideInHorizontally) + fade in
 * - Exit:  slide từ trong → ra phải (slideOutHorizontally) + fade out
 * - Easing: [FastOutSlowInEasing] cho cảm giác vật lý tự nhiên, mượt mà
 *
 * **Auto-dismiss**: Sau [autoDismissMs] milliseconds, gọi [onDismiss] tự động.
 *
 * @param message       Nội dung lỗi cần hiển thị. `null` = ẩn snackbar.
 * @param onDismiss     Callback khi snackbar tự dismiss hoặc user dismiss
 * @param autoDismissMs Thời gian tự động dismiss (mặc định 3000ms)
 */
@Composable
fun TopSnackbar(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissMs: Long = 3_000L
) {
    // Mỗi khi message thay đổi sang giá trị không null → bắt đầu đếm ngược auto-dismiss
    LaunchedEffect(message) {
        if (message != null) {
            delay(autoDismissMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = slideInHorizontally(
            // initialOffsetX = fullWidth → bắt đầu từ ngoài rìa phải màn hình
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = 420,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutHorizontally(
            // targetOffsetX = fullWidth → kết thúc ra ngoài rìa phải màn hình
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = 320,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(durationMillis = 200)
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    spotColor = MaterialTheme.colorScheme.errorContainer
                ),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // ── Nội dung thông báo ─────────────────────────────────────
                Text(
                    text = message ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
