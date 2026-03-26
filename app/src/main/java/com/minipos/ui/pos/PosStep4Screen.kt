package com.minipos.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.PaymentMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosStep4Screen(
    onPaymentSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: PosStep4ViewModel = hiltViewModel(),
) {
    val cart by viewModel.cartHolder.cart.collectAsState()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bước 4: Thanh toán") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Order summary
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tóm tắt đơn hàng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        cart.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "${item.product.name} x${
                                        if (item.quantity == item.quantity.toLong().toDouble()) item.quantity.toLong().toString()
                                        else item.quantity.toString()
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    CurrencyFormatter.format(item.lineTotal),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SummaryRow("Tạm tính", CurrencyFormatter.format(cart.subtotal))
                        if (cart.orderDiscountAmount > 0) {
                            SummaryRow("Giảm giá", "-${CurrencyFormatter.format(cart.orderDiscountAmount)}", color = AppColors.Error)
                        }
                        if (cart.taxAmount > 0) {
                            SummaryRow("Thuế", CurrencyFormatter.format(cart.taxAmount))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("TỔNG CỘNG", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                CurrencyFormatter.format(cart.grandTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary,
                            )
                        }
                        if (cart.customer != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppColors.TextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Khách: ${cart.customer!!.name}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            }
                        }
                    }
                }
            }

            // Payment method selection
            item {
                Text("Phương thức thanh toán", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PaymentMethodChip(
                        method = PaymentMethod.CASH,
                        icon = Icons.Default.Payments,
                        selected = state.selectedMethod == PaymentMethod.CASH,
                        onClick = { viewModel.selectMethod(PaymentMethod.CASH) },
                        modifier = Modifier.weight(1f),
                    )
                    PaymentMethodChip(
                        method = PaymentMethod.TRANSFER,
                        icon = Icons.Default.AccountBalance,
                        selected = state.selectedMethod == PaymentMethod.TRANSFER,
                        onClick = { viewModel.selectMethod(PaymentMethod.TRANSFER) },
                        modifier = Modifier.weight(1f),
                    )
                    PaymentMethodChip(
                        method = PaymentMethod.EWALLET,
                        icon = Icons.Default.PhoneAndroid,
                        selected = state.selectedMethod == PaymentMethod.EWALLET,
                        onClick = { viewModel.selectMethod(PaymentMethod.EWALLET) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Cash calculator
            if (state.selectedMethod == PaymentMethod.CASH) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Tiền khách đưa", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.receivedAmountText,
                                onValueChange = { viewModel.updateReceivedAmount(it) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                suffix = { Text("đ") },
                                textStyle = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick amount buttons
                            val quickAmounts = viewModel.getQuickAmounts(cart.grandTotal)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                quickAmounts.take(3).forEach { amount ->
                                    OutlinedButton(
                                        onClick = { viewModel.updateReceivedAmount(amount.toLong().toString()) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(CurrencyFormatter.formatCompact(amount), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            if (quickAmounts.size > 3) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    quickAmounts.drop(3).forEach { amount ->
                                        OutlinedButton(
                                            onClick = { viewModel.updateReceivedAmount(amount.toLong().toString()) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                        ) {
                                            Text(CurrencyFormatter.formatCompact(amount), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    // Fill remaining space
                                    repeat(3 - quickAmounts.drop(3).size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            if (state.changeAmount > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppColors.SecondaryContainer)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Tiền thừa", style = MaterialTheme.typography.titleSmall, color = AppColors.Secondary)
                                    Text(
                                        CurrencyFormatter.format(state.changeAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Secondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notes
            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                )
            }

            // Confirm button
            item {
                Button(
                    onClick = {
                        viewModel.confirmPayment(
                            grandTotal = cart.grandTotal,
                            onSuccess = onPaymentSuccess,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Secondary),
                    enabled = !state.isProcessing && state.canConfirm(cart.grandTotal),
                ) {
                    if (state.isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Xác nhận thanh toán", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Error
            if (state.error != null) {
                item {
                    Text(state.error!!, color = AppColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PaymentMethodChip(
    method: PaymentMethod,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(2.dp, AppColors.Primary, RoundedCornerShape(12.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AppColors.PrimaryContainer else AppColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = method.displayName(),
                tint = if (selected) AppColors.Primary else AppColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                method.displayName(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) AppColors.Primary else AppColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, color: Color = AppColors.TextSecondary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
