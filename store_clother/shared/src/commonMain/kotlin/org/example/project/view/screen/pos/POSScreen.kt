package org.example.project.view.screen.pos

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
fun POSScreen(
    onLogout: () -> Unit = {}
) {
    var selectedIndex by remember { mutableStateOf(0) }
    val navItems = listOf(
        DashboardNavItem("Bán hàng", "🛍️"),
        DashboardNavItem("Hóa đơn ca", "🧾"),
        DashboardNavItem("Tra cứu giá", "🔍"),
        DashboardNavItem("Kết ca", "⏱️")
    )

    AdaptiveDashboardScaffold(
        title = "Quầy Thu Ngân (POS)",
        roleTitle = "Nhân viên thu ngân",
        roleBadgeColor = Color(0xFF2E7D32),
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
            // ─── 4 Thẻ KPI ca làm việc ──────────────────────────────────────
            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AdaptiveStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Doanh số ca này",
                        value = "8,920,000 ₫",
                        subtitle = "38 hóa đơn đã thanh toán",
                        icon = "💵"
                    )
                    AdaptiveStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Số hóa đơn ca",
                        value = "38 đơn",
                        subtitle = "TB 235k / đơn",
                        icon = "🧾"
                    )
                    AdaptiveStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Khách đang chờ",
                        value = "2 khách",
                        subtitle = "Quầy số 1",
                        icon = "👥"
                    )
                    AdaptiveStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Máy in hóa đơn",
                        value = "Sẵn sàng",
                        subtitle = "🟢 Kết nối ổn định",
                        icon = "🖨️",
                        highlightColor = Color(0xFF2E7D32)
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
                            title = "Doanh số ca",
                            value = "8.9M ₫",
                            subtitle = "38 đơn",
                            icon = "💵"
                        )
                        AdaptiveStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Số hóa đơn",
                            value = "38 đơn",
                            subtitle = "Thành công",
                            icon = "🧾"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdaptiveStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Khách chờ",
                            value = "2 khách",
                            subtitle = "Quầy 1",
                            icon = "👥"
                        )
                        AdaptiveStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Máy in",
                            value = "Sẵn sàng",
                            subtitle = "🟢 Trực tuyến",
                            icon = "🖨️",
                            highlightColor = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // ─── Tác vụ nhanh thu ngân ─────────────────────────────────────────
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "⚡ Thao tác bán hàng",
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
                            Text(text = if (isWideScreen) "🛒 Tạo đơn mới (F1)" else "🛒 Đơn mới")
                        }
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (isWideScreen) "🔍 Tra cứu sản phẩm (F2)" else "🔍 Tra cứu")
                        }
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (isWideScreen) "🖨️ In lại hóa đơn" else "🖨️ In lại")
                        }
                    }
                }
            }

            // ─── Giỏ hàng đơn hiện tại ─────────────────────────────────────────
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛍️ Đơn hàng đang quét (#DH-CURRENT)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tổng: 1,210,000 ₫",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    POSItemRow(name = "Áo sơ mi Oxford Slimfit", code = "SKU: SM-OX-01", qty = "2", price = "580,000 ₫")
                    POSItemRow(name = "Quần Jeans nam Regular", code = "SKU: JN-RG-04", qty = "1", price = "450,000 ₫")
                    POSItemRow(name = "Áo phông Basic Cotton", code = "SKU: TS-BC-09", qty = "1", price = "180,000 ₫")
                }
            }
        }
    }
}

@Composable
private fun POSItemRow(name: String, code: String, qty: String, price: String) {
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
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "$code  •  SL: $qty", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = price, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}