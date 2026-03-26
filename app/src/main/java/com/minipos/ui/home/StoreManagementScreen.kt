package com.minipos.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minipos.core.theme.AppColors
import com.minipos.ui.navigation.Screen

private data class ManagementItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val route: String,
    val iconBgColor: Color,
    val iconTint: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreManagementScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    val catalogItems = listOf(
        ManagementItem(
            icon = Icons.Default.Category,
            title = "Danh mục",
            subtitle = "Quản lý nhóm sản phẩm",
            route = Screen.CategoryList.route,
            iconBgColor = Color(0xFFFFF7ED),
            iconTint = Color(0xFFD97706),
        ),
        ManagementItem(
            icon = Icons.Default.Inventory2,
            title = "Sản phẩm",
            subtitle = "Quản lý sản phẩm, giá bán",
            route = Screen.ProductList.route,
            iconBgColor = Color(0xFFF3E8FF),
            iconTint = Color(0xFF8B5CF6),
        ),
        ManagementItem(
            icon = Icons.Default.People,
            title = "Khách hàng",
            subtitle = "Quản lý thông tin khách hàng",
            route = Screen.CustomerList.route,
            iconBgColor = Color(0xFFFFF1F2),
            iconTint = Color(0xFFE11D48),
        ),
        ManagementItem(
            icon = Icons.Default.LocalShipping,
            title = "Nhà cung cấp",
            subtitle = "Quản lý nhà cung cấp",
            route = Screen.SupplierList.route,
            iconBgColor = Color(0xFFECFEFF),
            iconTint = Color(0xFF0891B2),
        ),
        ManagementItem(
            icon = Icons.Default.BarChart,
            title = "Báo cáo",
            subtitle = "Thống kê doanh thu, lợi nhuận",
            route = Screen.Reports.route,
            iconBgColor = Color(0xFFECFDF5),
            iconTint = Color(0xFF059669),
        ),
    )

    val operationItems = listOf(
        ManagementItem(
            icon = Icons.Default.MoveToInbox,
            title = "Nhập hàng",
            subtitle = "Nhập hàng từ nhà cung cấp",
            route = Screen.PurchaseOrder.route,
            iconBgColor = Color(0xFFECFDF5),
            iconTint = Color(0xFF059669),
        ),
        ManagementItem(
            icon = Icons.Default.Receipt,
            title = "Lịch sử đơn hàng",
            subtitle = "Xem, tìm kiếm đơn hàng",
            route = Screen.OrderList.route,
            iconBgColor = Color(0xFFF3E8FF),
            iconTint = Color(0xFF7C3AED),
        ),
        ManagementItem(
            icon = Icons.Default.Inventory,
            title = "Quản lý kho & Kiểm kho",
            subtitle = "Tồn kho, nhập/xuất, kiểm kê",
            route = Screen.InventoryOverview.route,
            iconBgColor = Color(0xFFFEF3C7),
            iconTint = Color(0xFFD97706),
        ),
        ManagementItem(
            icon = Icons.Default.QrCode,
            title = "Mã vạch (Barcode)",
            subtitle = "Tạo, in mã vạch sản phẩm",
            route = Screen.BarcodeManagement.route,
            iconBgColor = Color(0xFFF1F5F9),
            iconTint = Color(0xFF374151),
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Quản lý cửa hàng", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Surface),
            )
        },
        containerColor = AppColors.Background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Section: Danh mục & sản phẩm ──
            SectionHeader(title = "Danh mục & Sản phẩm")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column {
                    catalogItems.forEachIndexed { index, item ->
                        ManagementRow(
                            item = item,
                            onClick = { onNavigate(item.route) },
                        )
                        if (index < catalogItems.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = AppColors.Divider,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section: Kho hàng & Vận hành ──
            SectionHeader(title = "Kho hàng & Vận hành")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column {
                    operationItems.forEachIndexed { index, item ->
                        ManagementRow(
                            item = item,
                            onClick = { onNavigate(item.route) },
                        )
                        if (index < operationItems.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = AppColors.Divider,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.TextSecondary,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun ManagementRow(
    item: ManagementItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon container
        Surface(
            shape = CircleShape,
            color = item.iconBgColor,
            modifier = Modifier.size(42.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = item.iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary,
            )
            Text(
                item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
            )
        }

        // Chevron
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}
