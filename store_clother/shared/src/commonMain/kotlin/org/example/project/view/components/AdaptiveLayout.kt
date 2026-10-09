package org.example.project.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Phân loại kích thước màn hình theo chuẩn Material 3 Window Size Classes.
 */
enum class WindowWidthSizeClass {
    COMPACT,   // < 600dp (Điện thoại portrait)
    MEDIUM,    // 600dp - 840dp (Tablet, Foldable)
    EXPANDED   // >= 840dp (Desktop, Laptop, Màn hình rộng)
}

fun calculateWindowWidthSizeClass(width: Dp): WindowWidthSizeClass = when {
    width < 600.dp -> WindowWidthSizeClass.COMPACT
    width < 840.dp -> WindowWidthSizeClass.MEDIUM
    else -> WindowWidthSizeClass.EXPANDED
}

/**
 * Container thích ứng (Adaptive Scaffold) cho các màn hình Auth (Login, Register, Forgot, Reset).
 * - Trên điện thoại (< 720dp): Giao diện 1 cột cuộn mượt mà, full-width có padding chuẩn.
 * - Trên desktop / máy tính (>= 720dp): Giao diện 2 cột (Split View) gồm:
 *     + Cột trái: Hero Branding giới thiệu hệ thống Store Clother với gradient nổi bật.
 *     + Cột phải: Form nhập liệu được bao bọc trong Elevated Card sang trọng, bề rộng tối đa chuẩn Desktop (440 - 520dp).
 */
@Composable
fun AdaptiveAuthLayout(
    modifier: Modifier = Modifier,
    heroTitle: String = "Store Clother",
    heroSubtitle: String = "Giải pháp quản lý chuỗi cửa hàng thời trang thông minh & tinh gọn",
    topSnackbar: (@Composable () -> Unit)? = null,
    maxWidthDesktopCard: Dp = 480.dp,
    formContent: @Composable (isWideScreen: Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth >= 760.dp
        val sizeClass = calculateWindowWidthSizeClass(maxWidth)

        if (isWide) {
            // ─── GIAO DIỆN DESKTOP / MÁY TÍNH (Wide Split Layout) ────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cột trái: Hero Branding panel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            )
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.widthIn(max = 480.dp)
                        ) {
                            // Logo Box
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "👗",
                                    fontSize = 38.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Text(
                                text = heroTitle,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                lineHeight = 42.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = heroSubtitle,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(36.dp))

                            // Feature Pills
                            FeatureBadge(icon = "🛍️", text = "Bán hàng POS tốc độ cao")
                            Spacer(modifier = Modifier.height(12.dp))
                            FeatureBadge(icon = "📦", text = "Kiểm soát kho hàng chính xác theo thời gian thực")
                            Spacer(modifier = Modifier.height(12.dp))
                            FeatureBadge(icon = "📊", text = "Báo cáo doanh thu & biểu đồ phân tích trực quan")
                        }
                    }

                    // Cột phải: Form nhập liệu Desktop
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .padding(horizontal = 36.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ElevatedCard(
                            modifier = Modifier
                                .widthIn(max = maxWidthDesktopCard)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.elevatedCardElevation(
                                defaultElevation = 6.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(36.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                formContent(true)
                            }
                        }
                    }
                }

                // Top Snackbar nổi trên cùng
                topSnackbar?.invoke()
            }
        } else {
            // ─── GIAO DIỆN ĐIỆN THOẠI (Mobile Compact Layout) ────────────────────
            val scrollState = rememberScrollState()
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .statusBarsPadding()
                        .imePadding()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    formContent(false)
                }

                // Top Snackbar nổi trên cùng
                topSnackbar?.invoke()
            }
        }
    }
}

@Composable
private fun FeatureBadge(icon: String, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class DashboardNavItem(
    val title: String,
    val icon: String,
    val badge: String? = null
)

/**
 * Adaptive Scaffold cho các màn hình điều hành (Admin, POS, Kho).
 * - Trên desktop (>= 800dp): Left Navigation Rail / Sidebar chuyên nghiệp, khu vực làm việc rộng rãi.
 * - Trên mobile (< 800dp): Top bar gọn gàng, nút đăng xuất, bố cục thẻ cuộn 1 cột chạm mượt mà.
 */
@Composable
fun AdaptiveDashboardScaffold(
    title: String,
    roleTitle: String,
    roleBadgeColor: Color = MaterialTheme.colorScheme.primary,
    onLogout: () -> Unit,
    navigationItems: List<DashboardNavItem> = emptyList(),
    selectedNavIndex: Int = 0,
    onNavItemSelected: (Int) -> Unit = {},
    content: @Composable (isWideScreen: Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth >= 800.dp

        if (isWide) {
            // DESKTOP LAYOUT (Left Sidebar + Right Workspace)
            Row(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Brand Logo & Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primaryContainer
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👗", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Store Clother",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fashion Management",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Role badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = roleBadgeColor.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🛡️", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = roleTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = roleBadgeColor
                                )
                            }
                        }

                        // Navigation Items
                        navigationItems.forEachIndexed { index, item ->
                            val isSelected = index == selectedNavIndex
                            Surface(
                                onClick = { onNavItemSelected(index) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.icon, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (item.badge != null) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Text(
                                                text = item.badge,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Status pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF2E7D32), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hệ thống trực tuyến",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Logout Button
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🚪", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đăng xuất", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Right Main Workspace
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    // Desktop Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Hệ thống quản lý chuỗi thời trang đa chi nhánh",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "🖥️ Desktop Mode",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        content(true)
                    }
                }
            }
        } else {
            // MOBILE COMPACT LAYOUT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Mobile Top Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👗", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = roleTitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = roleBadgeColor
                                )
                            }
                        }

                        IconButton(onClick = onLogout) {
                            Text("🚪", fontSize = 20.sp)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    content(false)
                }
            }
        }
    }
}

/**
 * Thẻ hiển thị chỉ số thống kê (KPI Stat Card) dùng chung cho Desktop & Mobile.
 */
@Composable
fun AdaptiveStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: String,
    subtitle: String? = null,
    highlightColor: Color = MaterialTheme.colorScheme.primary
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(text = icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = highlightColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

