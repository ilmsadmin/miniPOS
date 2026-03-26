package com.minipos.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.minipos.core.theme.AppColors
import com.minipos.core.theme.PriceTypography
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.ui.navigation.Screen

data class HomeCard(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val route: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Refresh dashboard data when returning to this screen
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshDashboard()
    }

    val cards = listOf(
        HomeCard("Bán hàng\n(POS)", Icons.Default.ShoppingCart, AppColors.Primary, Screen.PosStep1.route),
        HomeCard("Quản lý kho\n& Kiểm kho", Icons.Default.Inventory, AppColors.Accent, Screen.InventoryOverview.route),
        HomeCard("Nhập hàng", Icons.Default.MoveToInbox, AppColors.Secondary, Screen.PurchaseOrder.route),
        HomeCard("Sản phẩm", Icons.Default.Inventory2, Color(0xFF8B5CF6), Screen.ProductList.route),
        HomeCard("Danh mục", Icons.Default.Category, Color(0xFFD97706), Screen.CategoryList.route),
        HomeCard("Nhà\ncung cấp", Icons.Default.LocalShipping, Color(0xFF0891B2), Screen.SupplierList.route),
        HomeCard("Khách\nhàng", Icons.Default.People, Color(0xFFE11D48), Screen.CustomerList.route),
        HomeCard("Báo cáo", Icons.Default.BarChart, Color(0xFF059669), Screen.Reports.route),
        HomeCard("Lịch sử\nđơn hàng", Icons.Default.Receipt, Color(0xFF7C3AED), Screen.OrderList.route),
        HomeCard("Cài đặt", Icons.Default.Settings, AppColors.TextSecondary, Screen.Settings.route),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.storeName, style = MaterialTheme.typography.titleLarge)
                        Text("Xin chào, ${state.userName}!", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Đăng xuất", tint = AppColors.TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Surface),
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Today's summary strip
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Primary),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💰 Doanh thu hôm nay", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            CurrencyFormatter.format(state.todayRevenue),
                            style = PriceTypography.price,
                            color = Color.White,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📦 Đơn hàng", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            "${state.todayOrders} đơn",
                            style = PriceTypography.price,
                            color = Color.White,
                        )
                    }
                }
            }

            // Low stock alert
            if (state.lowStockCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onNavigate(Screen.InventoryOverview.route) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.AccentContainer),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⚠️", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${state.lowStockCount} sản phẩm sắp hết hàng",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.AccentDark,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Function cards grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(cards) { card ->
                    FunctionCard(card = card, onClick = { onNavigate(card.route) })
                }
            }
        }
    }
}

@Composable
private fun FunctionCard(card: HomeCard, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                card.icon,
                contentDescription = card.title,
                modifier = Modifier.size(32.dp),
                tint = card.color,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                card.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = AppColors.TextPrimary,
                maxLines = 2,
            )
        }
    }
}
