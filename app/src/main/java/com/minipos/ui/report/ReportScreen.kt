package com.minipos.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Báo cáo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Tab selector
                item {
                    TabRow(
                        selectedTabIndex = state.selectedTab,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    ) {
                        Tab(selected = state.selectedTab == 0, onClick = { viewModel.selectTab(0) }) {
                            Text("Hôm nay", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = state.selectedTab == 1, onClick = { viewModel.selectTab(1) }) {
                            Text("7 ngày", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = state.selectedTab == 2, onClick = { viewModel.selectTab(2) }) {
                            Text("30 ngày", modifier = Modifier.padding(12.dp))
                        }
                    }
                }

                // Revenue card
                item {
                    val (revenue, orders) = when (state.selectedTab) {
                        0 -> state.todayRevenue to state.todayOrders
                        1 -> state.weekRevenue to state.weekOrders
                        else -> state.monthRevenue to state.monthOrders
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Primary),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Doanh thu", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                CurrencyFormatter.format(revenue),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$orders đơn hàng",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }
                }

                // Summary cards
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val (revenue, orders) = when (state.selectedTab) {
                            0 -> state.todayRevenue to state.todayOrders
                            1 -> state.weekRevenue to state.weekOrders
                            else -> state.monthRevenue to state.monthOrders
                        }
                        val avgOrderValue = if (orders > 0) revenue / orders else 0.0

                        ReportMetricCard(
                            icon = Icons.Default.Receipt,
                            label = "Số đơn",
                            value = orders.toString(),
                            color = AppColors.Secondary,
                            modifier = Modifier.weight(1f),
                        )
                        ReportMetricCard(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            label = "TB/đơn",
                            value = CurrencyFormatter.formatCompact(avgOrderValue),
                            color = AppColors.Accent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Recent orders for today
                if (state.selectedTab == 0 && state.recentOrders.isNotEmpty()) {
                    item {
                        Text("Đơn hàng hôm nay", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    }
                    items(state.recentOrders) { order ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(order.orderCode, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        DateUtils.formatTime(order.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary,
                                    )
                                }
                                Text(
                                    CurrencyFormatter.format(order.totalAmount),
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Primary,
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ReportMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
    }
}
