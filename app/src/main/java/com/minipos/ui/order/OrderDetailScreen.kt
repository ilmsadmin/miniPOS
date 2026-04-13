package com.minipos.ui.order

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.receipt.ReceiptPreviewDialog
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils
import com.minipos.domain.model.OrderItem
import com.minipos.domain.model.OrderPayment
import com.minipos.domain.model.OrderStatus
import com.minipos.domain.model.PaymentMethod
import com.minipos.ui.components.*

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
    var infoDialogTitle by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Coming-soon info dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(infoDialogTitle) },
            text = { Text(stringResource(R.string.feature_coming_soon_msg)) },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text(stringResource(R.string.ok)) }
            },
        )
    }

    // Show messages
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
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else if (state.detail == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.order_not_found), color = AppColors.TextSecondary)
            }
        } else {
            val detail = state.detail!!
            val order = detail.order

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                // ─── Top Bar ───
                MiniPosTopBar(
                    title = stringResource(R.string.order_detail_title),
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = { viewModel.onShareClick() }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = stringResource(R.string.share_order),
                                tint = AppColors.TextSecondary,
                            )
                        }
                    },
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ─── Order Header Card ───
                    item {
                        OrderHeaderCard(
                            orderCode = order.orderCode,
                            totalAmount = order.totalAmount,
                            status = order.status,
                            createdAt = order.createdAt,
                        )
                    }

                    // ─── Products Section ───
                    item {
                        SectionLabel(stringResource(R.string.order_products_section, detail.items.size))
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                                .background(AppColors.Surface)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg)),
                        ) {
                            detail.items.forEachIndexed { index, item ->
                                ProductItemRow(item)
                                if (index < detail.items.size - 1) {
                                    HorizontalDivider(color = AppColors.Border)
                                }
                            }
                        }
                    }

                    // ─── Summary Section ───
                    item {
                        SectionLabel(stringResource(R.string.order_summary_section))
                    }
                    item {
                        SummaryCard(
                            subtotal = order.subtotal,
                            taxAmount = order.taxAmount,
                            discountAmount = order.discountAmount,
                            totalAmount = order.totalAmount,
                        )
                    }

                    // ─── Payment Section ───
                    if (detail.payments.isNotEmpty()) {
                        item {
                            SectionLabel(stringResource(R.string.order_payment_section))
                        }
                        items(detail.payments) { payment ->
                            PaymentMethodCard(payment)
                        }
                    }

                    // ─── Customer Section ───
                    item {
                        SectionLabel(stringResource(R.string.order_customer_section))
                    }
                    item {
                        CustomerInfoCard(
                            customerName = order.customerName,
                            customerPhone = order.customerPhone,
                            notes = order.notes,
                        )
                    }

                    // ─── Action Buttons ───
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Reprint
                            ActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Print,
                                label = stringResource(R.string.btn_reprint),
                                style = ActionButtonStyle.SECONDARY,
                                onClick = { viewModel.showReceiptPreview() },
                            )
                            // Duplicate order — coming soon, use SECONDARY style
                            ActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.ContentCopy,
                                label = stringResource(R.string.btn_duplicate_order),
                                style = ActionButtonStyle.SECONDARY,
                                onClick = {
                                    infoDialogTitle = context.getString(R.string.btn_duplicate_order)
                                    showInfoDialog = true
                                },
                            )
                        }
                    }
                    item {
                        ActionButton(
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Undo,
                            label = stringResource(R.string.btn_return_refund),
                            style = ActionButtonStyle.DANGER,
                            onClick = {
                                infoDialogTitle = context.getString(R.string.btn_return_refund)
                                showInfoDialog = true
                            },
                        )
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// Order Header Card (centered, gradient total)
// ═══════════════════════════════════════

@Composable
private fun OrderHeaderCard(
    orderCode: String,
    totalAmount: Double,
    status: OrderStatus,
    createdAt: Long,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Order ID
        Text(
            text = orderCode,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextTertiary,
            letterSpacing = 0.5.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Big gradient total
        Text(
            text = CurrencyFormatter.format(totalAmount),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            style = LocalTextStyle.current.copy(
                brush = Brush.linearGradient(
                    colors = listOf(AppColors.Primary, AppColors.PrimaryLight)
                )
            ),
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Status badge
        data class StatusInfo(val bg: Color, val fg: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector, val textRes: Int)
        val statusInfo = when (status) {
            OrderStatus.COMPLETED -> StatusInfo(AppColors.SuccessSoft, AppColors.Success, Icons.Rounded.CheckCircle, R.string.status_completed)
            OrderStatus.REFUNDED -> StatusInfo(AppColors.ErrorContainer, AppColors.Error, Icons.Rounded.Undo, R.string.status_refunded)
            OrderStatus.PARTIALLY_REFUNDED -> StatusInfo(AppColors.WarningSoft, AppColors.Warning, Icons.Rounded.Undo, R.string.status_partially_refunded)
            OrderStatus.CANCELLED -> StatusInfo(AppColors.ErrorContainer, AppColors.Error, Icons.Rounded.Cancel, R.string.status_cancelled)
        }
        Row(
            modifier = Modifier
                .background(statusInfo.bg, RoundedCornerShape(MiniPosTokens.RadiusFull))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                statusInfo.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = statusInfo.fg,
            )
            Text(
                stringResource(statusInfo.textRes),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = statusInfo.fg,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Date + Time
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.CalendarToday, null, modifier = Modifier.size(16.dp), tint = AppColors.TextTertiary)
                Text(DateUtils.formatDate(createdAt), fontSize = 12.sp, color = AppColors.TextTertiary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(16.dp), tint = AppColors.TextTertiary)
                Text(DateUtils.formatTime(createdAt), fontSize = 12.sp, color = AppColors.TextTertiary)
            }
        }
    }
}

// ═══════════════════════════════════════
// Section Label
// ═══════════════════════════════════════

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = AppColors.TextTertiary,
        letterSpacing = 0.8.sp,
    )
}

// ═══════════════════════════════════════
// Product Item Row
// ═══════════════════════════════════════

@Composable
private fun ProductItemRow(item: OrderItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Category icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                .background(
                    Brush.linearGradient(
                        listOf(AppColors.Primary, AppColors.PrimaryLight)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ShoppingBag,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Body
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.productName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
            Text(
                "${CurrencyFormatter.format(item.unitPrice)} × ${
                    if (item.quantity == item.quantity.toLong().toDouble()) item.quantity.toLong().toString()
                    else item.quantity.toString()
                }",
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
        }

        // Price
        Text(
            CurrencyFormatter.format(item.totalPrice),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.Accent,
        )
    }
}

// ═══════════════════════════════════════
// Summary Card
// ═══════════════════════════════════════

@Composable
private fun SummaryCard(
    subtotal: Double,
    taxAmount: Double,
    discountAmount: Double,
    totalAmount: Double,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(16.dp),
    ) {
        SummaryRow(stringResource(R.string.label_subtotal), CurrencyFormatter.format(subtotal))
        if (taxAmount > 0) {
            val taxPercent = if (subtotal > 0) ((taxAmount / subtotal) * 100).toInt().toString() else "0"
            SummaryRow(stringResource(R.string.label_tax_percent, taxPercent), CurrencyFormatter.format(taxAmount))
        }
        SummaryRow(
            stringResource(R.string.label_discount),
            if (discountAmount > 0) "-${CurrencyFormatter.format(discountAmount)}" else CurrencyFormatter.format(0.0),
            valueColor = if (discountAmount > 0) AppColors.Error else AppColors.Success,
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 2.dp, color = AppColors.BorderLight)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.label_grand_total),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = AppColors.TextPrimary,
            )
            Text(
                CurrencyFormatter.format(totalAmount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = AppColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = AppColors.TextSecondary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = AppColors.TextSecondary)
        Text(value, fontSize = 13.sp, color = valueColor)
    }
}

// ═══════════════════════════════════════
// Payment Method Card
// ═══════════════════════════════════════

@Composable
private fun PaymentMethodCard(payment: OrderPayment) {
    val paymentContext = LocalContext.current
    val (icon, iconBg, iconTint) = when (payment.method) {
        PaymentMethod.CASH -> Triple(Icons.Rounded.Payments, AppColors.SuccessSoft, AppColors.Success)
        PaymentMethod.TRANSFER -> Triple(Icons.Rounded.AccountBalance, AppColors.PrimaryContainer, AppColors.Primary)
        PaymentMethod.EWALLET -> Triple(Icons.Rounded.Smartphone, AppColors.AccentContainer, AppColors.Accent)
        PaymentMethod.OTHER -> Triple(Icons.Rounded.CreditCard, AppColors.SurfaceVariant, AppColors.TextSecondary)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = iconTint)
        }
        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                payment.method.displayName(paymentContext),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
            if (payment.receivedAmount != null && payment.changeAmount > 0) {
                Text(
                    stringResource(
                        R.string.payment_received_change,
                        CurrencyFormatter.format(payment.receivedAmount),
                        CurrencyFormatter.format(payment.changeAmount),
                    ),
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                )
            }
        }
    }
}

// ═══════════════════════════════════════
// Customer Info Card
// ═══════════════════════════════════════

@Composable
private fun CustomerInfoCard(
    customerName: String?,
    customerPhone: String?,
    notes: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(16.dp),
    ) {
        InfoRow(
            stringResource(R.string.label_customer_name),
            customerName ?: stringResource(R.string.label_guest),
        )
        InfoRow(
            stringResource(R.string.label_customer_phone),
            customerPhone ?: stringResource(R.string.label_none),
            valueColor = if (customerPhone != null) AppColors.TextPrimary else AppColors.TextTertiary,
        )
        InfoRow(
            stringResource(R.string.label_notes),
            notes ?: stringResource(R.string.label_none),
            valueColor = if (notes != null) AppColors.TextPrimary else AppColors.TextTertiary,
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = AppColors.TextPrimary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = AppColors.TextTertiary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ═══════════════════════════════════════
// Action Buttons
// ═══════════════════════════════════════

private enum class ActionButtonStyle { PRIMARY, SECONDARY, DANGER }

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    style: ActionButtonStyle,
    onClick: () -> Unit,
) {
    val (bg, contentColor, borderColor) = when (style) {
        ActionButtonStyle.PRIMARY -> Triple(
            Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight)),
            Color.White,
            Color.Transparent,
        )
        ActionButtonStyle.SECONDARY -> Triple(
            Brush.linearGradient(listOf(AppColors.InputBackground, AppColors.InputBackground)),
            AppColors.TextPrimary,
            AppColors.Border,
        )
        ActionButtonStyle.DANGER -> Triple(
            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            AppColors.Error,
            AppColors.Error,
        )
    }

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
            .background(bg)
            .then(
                if (borderColor != Color.Transparent) Modifier.border(
                    1.dp, borderColor, RoundedCornerShape(MiniPosTokens.Radius2xl)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = contentColor)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor,
        )
    }
}

// ═══════════════════════════════════════
// Dialogs (kept from original)
// ═══════════════════════════════════════

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
    MiniPosActionSheet(
        visible = true,
        title = stringResource(R.string.share_receipt_title),
        description = stringResource(R.string.select_share_format),
        items = listOf(
            ActionSheetItem(
                label = stringResource(R.string.share_pdf),
                icon = Icons.Rounded.PictureAsPdf,
                onClick = onSharePdf,
            ),
            ActionSheetItem(
                label = stringResource(R.string.share_text),
                icon = Icons.Rounded.TextSnippet,
                onClick = onShareText,
            ),
        ),
        onDismiss = onDismiss,
    )
}
