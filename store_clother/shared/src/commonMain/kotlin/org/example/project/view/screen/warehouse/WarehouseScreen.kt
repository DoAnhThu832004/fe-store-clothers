package org.example.project.view.screen.warehouse

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
fun WarehouseScreen(
    onLogout: () -> Unit = {}
) {
    var selectedIndex by remember { mutableStateOf(0) }
    val navItems = listOf(
        DashboardNavItem("Kho hàng", "📦"),
        DashboardNavItem("Nhập hàng", "📥"),
        DashboardNavItem("Xuất hàng", "📤"),
        DashboardNavItem("Kiểm kê", "📋")
    )

    AdaptiveDashboardScaffold(
        title = "Quản Lý Kho Hàng",
        roleTitle = "Nhân viên thủ kho",
        roleBadgeColor = Color(0xFFE65100),
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
            // ─── 4 Thẻ KPI Kho Hàng ──────────────────────────────────────────
            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AdaptiveStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Tổng mã sản phẩm",
                        value = "1,248 SKU",
                        subtitle = "14,500 chiếc trong kho",
                        icon = "📦"
                    )
                    AdaptiveStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Phiếu nhập hôm nay",
                        value = "8 phiếu",
                        subtitle = "✅ Đã lưu kho an toàn",
                        icon = "📥"
                    )
                    AdaptiveStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Phiếu xuất chuyển",
                        value = "12 phiếu",
                        subtitle = "3 chi nhánh nhận",
                        icon = "📤"
                    )
                    AdaptiveStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Cảnh báo sắp hết",
                        value = "5 SKU",
                        subtitle = "🚨 Cần đặt hàng NCC",
                        icon = "⚠️",
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
                            title = "Tổng sản phẩm",
                            value = "1,248 SKU",
                            subtitle = "Kho chính",
                            icon = "📦"
                        )
                        AdaptiveStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Nhập hôm nay",
                            value = "8 phiếu",
                            subtitle = "Hoàn tất",
                            icon = "📥"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdaptiveStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Xuất hôm nay",
                            value = "12 phiếu",
                            subtitle = "Chuyển shop",
                            icon = "📤"
                        )
                        AdaptiveStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Sắp hết hàng",
                            value = "5 SKU",
                            subtitle = "Cần nhập gấp",
                            icon = "⚠️",
                            highlightColor = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ─── Tác vụ nhanh kho ─────────────────────────────────────────────
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "⚡ Nghiệp vụ kho vận",
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
                            Text(text = if (isWideScreen) "📥 Phiếu nhập kho" else "📥 Nhập kho")
                        }
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (isWideScreen) "📤 Phiếu xuất điều chuyển" else "📤 Xuất kho")
                        }
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (isWideScreen) "📋 Phiếu kiểm kê" else "📋 Kiểm kê")
                        }
                    }
                }
            }

            // ─── Nhật ký kho hàng mới nhất ────────────────────────────────────
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📋 Biến động tồn kho gần đây",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    WarehouseLogItem(title = "Nhập 120 Áo polo Cotton Classic", note = "NCC Việt Tiến • Phiếu #NK-204", change = "+120", isPositive = true)
                    WarehouseLogItem(title = "Xuất 30 Quần Kaki Slimfit Be", note = "CN Quận 1 • Phiếu #XK-118", change = "-30", isPositive = false)
                    WarehouseLogItem(title = "Cảnh báo tồn kho: Váy hoa Vintage", note = "Chỉ còn 2 chiếc trong kho", change = "Cảnh báo", isPositive = false)
                }
            }
        }
    }
}

@Composable
private fun WarehouseLogItem(title: String, note: String, change: String, isPositive: Boolean) {
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
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = change,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isPositive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
            )
        }
    }
}