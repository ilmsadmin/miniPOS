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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.receipt.ReceiptPreviewDialog
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

    // Show messages (only when receipt preview is NOT open)
    LaunchedEffect(state.message, state.showReceiptPreview) {
        if (!state.showReceiptPreview) {
            state.message?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearMessage()
            }
        }
    }

    // Printer selection dialog
    if (state.showPrinterDialog) {
        PrinterSelectionDialog(
            devices = state.pairedDevices,
            onSelect = { viewModel.printToDevice(context, it) },
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

    // Receipt preview dialog
    if (state.showReceiptPreview && state.store != null && state.detail != null) {
        ReceiptPreviewDialog(
            store = state.store!!,
            orderDetail = state.detail!!,
            isPrinting = state.isPrinting,
            isSharing = state.isSharing,
            errorMessage = state.message,
            onPrint = { viewModel.onPrintClick(context) },
            onShare = { viewModel.shareAsPdf(context) },
            onDismiss = { viewModel.dismissReceiptPreview() },
            onErrorShown = { viewModel.clearMessage() },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.order?.orderCode ?: stringResource(R.string.order_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showReceiptPreview() }) {
                        Icon(Icons.Default.Receipt, contentDescription = stringResource(R.string.receipt_preview_title))
                    }
                    if (state.isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = { viewModel.onShareClick() }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_btn))
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
                Text(stringResource(R.string.order_not_found), color = AppColors.TextSecondary)
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
                            Text(stringResource(R.string.order_info_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoRow(stringResource(R.string.order_code_label), order.orderCode)
                            InfoRow(stringResource(R.string.time_label), DateUtils.formatDateTime(order.createdAt))
                            InfoRow(stringResource(R.string.status_label), order.status.name)
                            if (!order.customerName.isNullOrBlank()) {
                                InfoRow(stringResource(R.string.customer_label), order.customerName)
                            }
                            if (!order.notes.isNullOrBlank()) {
                                InfoRow(stringResource(R.string.notes_label), order.notes)
                            }
                        }
                    }
                }

                // Items
                item {
                    Text(stringResource(R.string.products_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
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
                            InfoRow(stringResource(R.string.subtotal), CurrencyFormatter.format(order.subtotal))
                            if (order.discountAmount > 0) {
                                InfoRow(stringResource(R.string.discount), "-${CurrencyFormatter.format(order.discountAmount)}", AppColors.Error)
                            }
                            if (order.taxAmount > 0) {
                                InfoRow(stringResource(R.string.tax), CurrencyFormatter.format(order.taxAmount))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(stringResource(R.string.grand_total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                        Text(stringResource(R.string.payment_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
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
                                val paymentContext = LocalContext.current
                                Text(payment.method.displayName(paymentContext), fontWeight = FontWeight.Medium)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(CurrencyFormatter.format(payment.amount), fontWeight = FontWeight.Bold)
                                    if (payment.changeAmount > 0) {
                                        Text(
                                            stringResource(R.string.change_prefix, CurrencyFormatter.format(payment.changeAmount)),
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
                            onClick = { viewModel.showReceiptPreview() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.print_receipt_btn))
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
                            Text(stringResource(R.string.share_btn))
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
        title = { Text(stringResource(R.string.select_printer)) },
        text = {
            if (devices.isEmpty()) {
                Text(stringResource(R.string.no_bluetooth_devices))
            } else {
                Column {
                    Text(
                        stringResource(R.string.select_paired_printer),
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
                                        device.name ?: stringResource(R.string.unknown_device),
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
                Text(stringResource(R.string.close))
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
        title = { Text(stringResource(R.string.share_receipt_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.select_share_format),
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
                            Text(stringResource(R.string.share_pdf), fontWeight = FontWeight.Medium)
                            Text(
                                stringResource(R.string.share_pdf_desc),
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
                            Text(stringResource(R.string.share_text), fontWeight = FontWeight.Medium)
                            Text(
                                stringResource(R.string.share_text_desc),
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
                Text(stringResource(R.string.close))
            }
        },
    )
}
