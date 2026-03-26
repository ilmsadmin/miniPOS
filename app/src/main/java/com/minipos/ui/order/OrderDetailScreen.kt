package com.minipos.ui.order

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show messages
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Printer selection dialog
    if (state.showPrinterDialog) {
        PrinterSelectionDialog(
            devices = state.pairedDevices,
            onSelect = { viewModel.printToDevice(it) },
            onDismiss = { viewModel.dismissPrinterDialog() },
        )
    }

    // Share options dialog
    if (state.showShareOptions) {
        ShareOptionsDialog(
            onSharePdf = { viewModel.shareAsPdf(context) },
            onShareText = { viewModel.shareAsText(context) },
            onDismiss = { viewModel.dismissShareOptions() },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.order?.orderCode ?: "Chi tiết đơn hàng") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (state.isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = { viewModel.onPrintClick(context) }) {
                            Icon(Icons.Default.Print, contentDescription = "In")
                        }
                    }
                    if (state.isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = { viewModel.onShareClick() }) {
                            Icon(Icons.Default.Share, contentDescription = "Chia sẻ")
                        }
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
                                InfoRow("Khách hàng", order.customerName)
                            }
                            if (!order.notes.isNullOrBlank()) {
                                InfoRow("Ghi chú", order.notes)
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
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Print & Share buttons at bottom
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onPrintClick(context) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !state.isPrinting,
                        ) {
                            if (state.isPrinting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("In hóa đơn")
                        }
                        Button(
                            onClick = { viewModel.onShareClick() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                            enabled = !state.isSharing,
                        ) {
                            if (state.isSharing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = androidx.compose.ui.graphics.Color.White)
                            } else {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chia sẻ")
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

@SuppressLint("MissingPermission")
@Composable
private fun PrinterSelectionDialog(
    devices: List<android.bluetooth.BluetoothDevice>,
    onSelect: (android.bluetooth.BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Print, contentDescription = null, tint = AppColors.Primary) },
        title = { Text("Chọn máy in") },
        text = {
            if (devices.isEmpty()) {
                Text("Không tìm thấy thiết bị Bluetooth nào.\nVui lòng ghép nối máy in trong cài đặt Bluetooth.")
            } else {
                Column {
                    Text(
                        "Chọn máy in Bluetooth đã ghép nối:",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    devices.forEach { device ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(device) },
                            shape = RoundedCornerShape(8.dp),
                            color = AppColors.SurfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        device.name ?: "Không rõ tên",
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        device.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        },
    )
}

@Composable
private fun ShareOptionsDialog(
    onSharePdf: () -> Unit,
    onShareText: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Share, contentDescription = null, tint = AppColors.Primary) },
        title = { Text("Chia sẻ hóa đơn") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Chọn định dạng chia sẻ:",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))

                // PDF option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSharePdf() },
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.SurfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = AppColors.Error,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Chia sẻ PDF", fontWeight = FontWeight.Medium)
                            Text(
                                "Gửi hóa đơn dạng file PDF đẹp",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary,
                            )
                        }
                    }
                }

                // Text option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShareText() },
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.SurfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TextSnippet,
                            contentDescription = null,
                            tint = AppColors.Secondary,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Chia sẻ văn bản", fontWeight = FontWeight.Medium)
                            Text(
                                "Gửi qua Zalo, SMS, Messenger...",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        },
    )
}
