package org.example.project.view.screen.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.view.components.AdaptiveDashboardScaffold
import org.example.project.view.components.AdaptiveStatCard
import org.example.project.view.components.DashboardNavItem

@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit = {}
) {
    var selectedIndex by remember { mutableStateOf(0) }
    val navItems = listOf(
        DashboardNavItem("Tổng quan", "📊"),
        DashboardNavItem("Nhân viên", "👥"),
        DashboardNavItem("Doanh thu", "💰"),
        DashboardNavItem("Cài đặt", "⚙️")
    )

    AdaptiveDashboardScaffold(
        title = "Quản Trị Hệ Thống",
        roleTitle = "Chủ cửa hàng (Admin)",
        roleBadgeColor = MaterialTheme.colorScheme.primary,
        onLogout = onLogout,
        navigationItems = navItems,
        selectedNavIndex = selectedIndex,
        onNavItemSelected = { selectedIndex = it }
    ) { isWideScreen ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ─── 4 Thẻ KPI Thống Kê ───────────────────────────────────────────
            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AdaptiveStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Doanh thu hôm nay",
                        value = "24,500,000 ₫",
                        subtitle = "📈 +12.5% so với hôm qua",
                        icon = "💰"
                    )
                    AdaptiveStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Đơn hàng hôm nay",
                        value = "142 đơn",
                        subtitle = "✅ 98% giao thành công",
                        icon = "🛍️"
                    )
                    AdaptiveStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Khách hàng mới",
                        value = "28 khách",
                        subtitle = "👥 Tổng 3,420 thành viên",
                        icon = "👥"
                    )
                    AdaptiveStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Tồn kho cảnh báo",
                        value = "3 mã SKU",
                        subtitle = "⚠️ Cần nhập thêm hàng",
                        icon = "🚨",
                        highlightColor = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdaptiveStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Doanh thu",
                            value = "24.5M ₫",
                            subtitle = "+12.5%",
                            icon = "💰"
                        )
                        AdaptiveStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Đơn hàng",
                            value = "142 đơn",
                            subtitle = "Thành công",
                            icon = "🛍️"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdaptiveStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Khách mới",
                            value = "28 khách",
                            subtitle = "Thành viên",
                            icon = "👥"
                        )
                        AdaptiveStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Tồn kho thấp",
                            value = "3 SKU",
                            subtitle = "Cảnh báo",
                            icon = "🚨",
                            highlightColor = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ─── Tác vụ nhanh (Quick Actions) ─────────────────────────────────
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "⚡ Tác vụ nhanh",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (isWideScreen) "🏷️ Tạo khuyến mãi" else "🏷️ Khuyến mãi")
                        }
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (isWideScreen) "📈 Báo cáo chi tiết" else "📈 Báo cáo")
                        }
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (isWideScreen) "🏪 Thêm chi nhánh" else "🏪 Chi nhánh")
                        }
                    }
                }
            }

            // ─── Hoạt động gần đây ───────────────────────────────────────────
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📋 Hóa đơn mới nhất",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RecentTransactionItem(id = "#HD-8921", time = "10 phút trước", amount = "850,000 ₫", status = "Thành công")
                    RecentTransactionItem(id = "#HD-8920", time = "35 phút trước", amount = "1,420,000 ₫", status = "Thành công")
                    RecentTransactionItem(id = "#HD-8919", time = "1 giờ trước", amount = "320,000 ₫", status = "Thành công")
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionItem(
    id: String,
    time: String,
    amount: String,
    status: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = id, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = time, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = amount, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text(text = status, fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
            }
        }
    }
}