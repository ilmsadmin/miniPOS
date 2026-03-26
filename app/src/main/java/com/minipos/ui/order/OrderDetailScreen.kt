package com.minipos.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.order?.orderCode ?: "Chi tiết đơn hàng") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Print */ }) {
                        Icon(Icons.Default.Print, contentDescription = "In")
                    }
                    IconButton(onClick = { /* TODO: Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Chia sẻ")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.detail == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Không tìm thấy đơn hàng", color = AppColors.TextSecondary)
            }
        } else {
            val detail = state.detail!!
            val order = detail.order

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Order info
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Thông tin đơn hàng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoRow("Mã đơn", order.orderCode)
                            InfoRow("Thời gian", DateUtils.formatDateTime(order.createdAt))
                            InfoRow("Trạng thái", order.status.name)
                            if (!order.customerName.isNullOrBlank()) {
                                InfoRow("Khách hàng", order.customerName!!)
                            }
                            if (!order.notes.isNullOrBlank()) {
                                InfoRow("Ghi chú", order.notes!!)
                            }
                        }
                    }
                }

                // Items
                item {
                    Text("Sản phẩm", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                }
                items(detail.items) { item ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontWeight = FontWeight.Medium)
                                Text(
                                    "${CurrencyFormatter.format(item.unitPrice)} x ${
                                        if (item.quantity == item.quantity.toLong().toDouble()) item.quantity.toLong().toString()
                                        else item.quantity.toString()
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary,
                                )
                            }
                            Text(
                                CurrencyFormatter.format(item.totalPrice),
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary,
                            )
                        }
                    }
                }

                // Totals
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("Tạm tính", CurrencyFormatter.format(order.subtotal))
                            if (order.discountAmount > 0) {
                                InfoRow("Giảm giá", "-${CurrencyFormatter.format(order.discountAmount)}", AppColors.Error)
                            }
                            if (order.taxAmount > 0) {
                                InfoRow("Thuế", CurrencyFormatter.format(order.taxAmount))
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("TỔNG CỘNG", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    CurrencyFormatter.format(order.totalAmount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Primary,
                                )
                            }
                        }
                    }
                }

                // Payments
                if (detail.payments.isNotEmpty()) {
                    item {
                        Text("Thanh toán", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    }
                    items(detail.payments) { payment ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.SecondaryContainer),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(payment.method.displayName(), fontWeight = FontWeight.Medium)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(CurrencyFormatter.format(payment.amount), fontWeight = FontWeight.Bold)
                                    if (payment.changeAmount > 0) {
                                        Text(
                                            "Thừa: ${CurrencyFormatter.format(payment.changeAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppColors.TextSecondary,
                                        )
                                    }
                                }
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
private fun InfoRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = AppColors.TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.Medium)
    }
}
