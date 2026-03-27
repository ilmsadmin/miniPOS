package com.minipos.ui.pos

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.CartItem
import com.minipos.domain.model.Discount
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosStep2Screen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: PosStep2ViewModel = hiltViewModel(),
) {
    val cart by viewModel.cartHolder.cart.collectAsState()
    val stockError by viewModel.stockError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show stock error as snackbar
    LaunchedEffect(stockError) {
        stockError?.let { error ->
            snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Short)
            viewModel.clearStockError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.step2_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Order discount row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.order_discount_label), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { viewModel.showOrderDiscountDialog() }) {
                            Text(
                                if (cart.orderDiscount != null) {
                                    when (cart.orderDiscount!!.type) {
                                        "percent" -> "${cart.orderDiscount!!.value.toInt()}%"
                                        else -> CurrencyFormatter.format(cart.orderDiscount!!.value)
                                    }
                                } else stringResource(R.string.add_label),
                                color = AppColors.Primary,
                            )
                        }
                    }
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(R.string.subtotal), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                        Text(CurrencyFormatter.format(cart.subtotal), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (cart.orderDiscountAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(stringResource(R.string.discount), style = MaterialTheme.typography.bodyMedium, color = AppColors.Error)
                            Text("-${CurrencyFormatter.format(cart.orderDiscountAmount)}", color = AppColors.Error)
                        }
                    }
                    if (cart.taxAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(stringResource(R.string.tax), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                            Text(CurrencyFormatter.format(cart.taxAmount))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(R.string.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            CurrencyFormatter.format(cart.grandTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                        enabled = !cart.isEmpty(),
                    ) {
                        Text(stringResource(R.string.next), modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        val showDiscountDialog by viewModel.showOrderDiscount.collectAsState()

        if (showDiscountDialog) {
            OrderDiscountDialog(
                current = cart.orderDiscount,
                onDismiss = { viewModel.dismissOrderDiscountDialog() },
                onApply = { discount ->
                    viewModel.setOrderDiscount(discount)
                    viewModel.dismissOrderDiscountDialog()
                },
            )
        }

        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColors.TextTertiary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.cart_empty), style = MaterialTheme.typography.titleMedium, color = AppColors.TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back_to_products)) }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(cart.items) { index, item ->
                    CartItemCard(
                        item = item,
                        onQuantityChange = { viewModel.updateQuantity(index, it) },
                        onPriceChange = { viewModel.updatePrice(index, it) },
                        onRemove = { viewModel.removeItem(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(
    item: CartItem,
    onQuantityChange: (Double) -> Unit,
    onPriceChange: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    var showPriceEdit by remember { mutableStateOf(false) }
    var priceText by remember(item.unitPrice) { mutableStateOf(item.unitPrice.toLong().toString()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.variant != null) {
                        Text(
                            item.variant.variantName,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.Accent,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        "${item.product.unit} · SKU: ${item.variant?.sku ?: item.product.sku}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_delete), tint = AppColors.Error, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Quantity controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onQuantityChange((item.quantity - 1).coerceAtLeast(0.0)) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AppColors.SurfaceVariant),
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_decrease), modifier = Modifier.size(16.dp))
                    }
                    Text(
                        if (item.quantity == item.quantity.toLong().toDouble()) item.quantity.toLong().toString()
                        else item.quantity.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = { onQuantityChange(item.quantity + 1) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AppColors.SurfaceVariant),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_increase), modifier = Modifier.size(16.dp))
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    // Price (clickable to edit)
                    Text(
                        CurrencyFormatter.format(item.unitPrice),
                        modifier = Modifier.clickable { showPriceEdit = !showPriceEdit },
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                    )
                    Text(
                        CurrencyFormatter.format(item.lineTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary,
                    )
                }
            }

            // Inline price editor
            if (showPriceEdit) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.unit_price_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = {
                            priceText.toDoubleOrNull()?.let { onPriceChange(it) }
                            showPriceEdit = false
                        }) { Text("OK") }
                    },
                    shape = RoundedCornerShape(8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderDiscountDialog(
    current: Discount?,
    onDismiss: () -> Unit,
    onApply: (Discount?) -> Unit,
) {
    var type by remember { mutableStateOf(current?.type ?: "percent") }
    var value by remember { mutableStateOf(current?.value?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.order_discount_dialog_title)) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "percent",
                        onClick = { type = "percent" },
                        label = { Text(stringResource(R.string.percent_label)) },
                    )
                    FilterChip(
                        selected = type == "fixed",
                        onClick = { type = "fixed" },
                        label = { Text(stringResource(R.string.fixed_amount_label)) },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(if (type == "percent") stringResource(R.string.percent_field) else stringResource(R.string.amount_field)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    suffix = { Text(if (type == "percent") "%" else "đ") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = value.toDoubleOrNull()
                if (v != null && v > 0) {
                    onApply(Discount(type, v))
                } else {
                    onApply(null)
                }
            }) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = {
                onApply(null)
            }) { Text(stringResource(R.string.remove_discount)) }
        },
    )
}
