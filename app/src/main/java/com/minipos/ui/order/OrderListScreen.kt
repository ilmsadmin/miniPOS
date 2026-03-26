package com.minipos.ui.order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils
import com.minipos.domain.model.Order
import com.minipos.domain.model.OrderStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit,
    viewModel: OrderListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đơn hàng") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status filter
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.filterStatus == null,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text("Tất cả") },
                    )
                }
                items(listOf("COMPLETED" to "Hoàn thành", "REFUNDED" to "Hoàn trả", "CANCELLED" to "Hủy")) { (status, label) ->
                    FilterChip(
                        selected = state.filterStatus == status,
                        onClick = { viewModel.setFilter(status) },
                        label = { Text(label) },
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColors.TextTertiary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Chưa có đơn hàng", style = MaterialTheme.typography.titleMedium, color = AppColors.TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.orders) { order ->
                        OrderItem(order = order, onClick = { onOrderClick(order.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderItem(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.orderCode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    OrderStatusBadge(order.status)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    DateUtils.formatDateTime(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
                if (!order.customerName.isNullOrBlank()) {
                    Text(
                        "KH: ${order.customerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
            }
            Text(
                CurrencyFormatter.format(order.totalAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary,
            )
        }
    }
}

@Composable
private fun OrderStatusBadge(status: OrderStatus) {
    val (color, text) = when (status) {
        OrderStatus.COMPLETED -> AppColors.Secondary to "Hoàn thành"
        OrderStatus.REFUNDED -> AppColors.Warning to "Hoàn trả"
        OrderStatus.PARTIALLY_REFUNDED -> AppColors.Accent to "Hoàn trả 1 phần"
        OrderStatus.CANCELLED -> AppColors.Error to "Hủy"
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}
