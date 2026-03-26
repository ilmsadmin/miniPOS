package com.minipos.ui.inventory

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.StockMovementType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.showAdjustDialog && state.selectedProduct != null) {
        StockAdjustDialog(
            productName = state.selectedProduct!!.name,
            suppliers = state.suppliers,
            error = state.adjustError,
            onDismiss = { viewModel.dismissAdjustDialog() },
            onAdjust = { amount, type, supplierId -> viewModel.adjustStock(amount, type, supplierId) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tồn kho") },
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
        } else if (state.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColors.TextTertiary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Chưa có sản phẩm theo dõi tồn kho", style = MaterialTheme.typography.titleMedium, color = AppColors.TextSecondary)
                }
            }
        } else {
            // Summary header
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Summary cards
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val totalProducts = state.items.size
                        val lowStock = state.items.count { it.currentStock <= it.product.minStock && it.currentStock > 0 }
                        val outOfStock = state.items.count { it.currentStock <= 0 }

                        SummaryCard(
                            label = "Tổng SP",
                            value = totalProducts.toString(),
                            color = AppColors.Primary,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryCard(
                            label = "Sắp hết",
                            value = lowStock.toString(),
                            color = AppColors.Warning,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryCard(
                            label = "Hết hàng",
                            value = outOfStock.toString(),
                            color = AppColors.Error,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                items(state.items) { item ->
                    val isLow = item.currentStock <= item.product.minStock && item.currentStock > 0
                    val isOut = item.currentStock <= 0

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isOut -> AppColors.ErrorContainer
                                isLow -> AppColors.AccentContainer
                                else -> AppColors.Surface
                            }
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Text(
                                    "SKU: ${item.product.sku} · Min: ${item.product.minStock}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${item.currentStock.toLong()} ${item.product.unit}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isOut -> AppColors.Error
                                        isLow -> AppColors.Warning
                                        else -> AppColors.Secondary
                                    },
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.showAdjustDialog(item.product) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Điều chỉnh", tint = AppColors.Primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockAdjustDialog(
    productName: String,
    suppliers: List<com.minipos.domain.model.Supplier>,
    error: String?,
    onDismiss: () -> Unit,
    onAdjust: (Double, StockMovementType, String?) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(StockMovementType.PURCHASE_IN) }
    var selectedSupplierId by remember { mutableStateOf<String?>(null) }
    var supplierExpanded by remember { mutableStateOf(false) }

    val types = listOf(
        StockMovementType.PURCHASE_IN to "Nhập hàng",
        StockMovementType.ADJUSTMENT_IN to "Điều chỉnh tăng",
        StockMovementType.ADJUSTMENT_OUT to "Điều chỉnh giảm",
        StockMovementType.DAMAGE_OUT to "Hàng hỏng",
        StockMovementType.RETURN_IN to "Trả lại",
    )

    val isPurchaseIn = selectedType == StockMovementType.PURCHASE_IN
    val selectedSupplierName = suppliers.find { it.id == selectedSupplierId }?.name ?: "Chọn nhà cung cấp"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Điều chỉnh tồn kho") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)

                // Type selection
                types.forEach { (type, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                // Reset supplier if not purchase_in
                                if (type != StockMovementType.PURCHASE_IN) {
                                    selectedSupplierId = null
                                }
                            },
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Supplier selection - only for PURCHASE_IN
                if (isPurchaseIn) {
                    ExposedDropdownMenuBox(
                        expanded = supplierExpanded,
                        onExpandedChange = { supplierExpanded = !supplierExpanded },
                    ) {
                        OutlinedTextField(
                            value = selectedSupplierName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Nhà cung cấp") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                        )
                        ExposedDropdownMenu(
                            expanded = supplierExpanded,
                            onDismissRequest = { supplierExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("-- Không chọn --", color = AppColors.TextSecondary) },
                                onClick = {
                                    selectedSupplierId = null
                                    supplierExpanded = false
                                },
                            )
                            suppliers.forEach { supplier ->
                                DropdownMenuItem(
                                    text = { Text(supplier.name) },
                                    onClick = {
                                        selectedSupplierId = supplier.id
                                        supplierExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Số lượng") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                )

                if (error != null) {
                    Text(error, color = AppColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = amount.toDoubleOrNull()
                    if (qty != null && qty > 0) onAdjust(qty, selectedType, selectedSupplierId)
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0,
                shape = RoundedCornerShape(8.dp),
            ) { Text("Xác nhận") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}
