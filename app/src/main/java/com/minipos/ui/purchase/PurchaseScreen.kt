package com.minipos.ui.purchase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    onBack: () -> Unit,
    viewModel: PurchaseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Show messages
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    // Product picker dialog
    if (state.showProductPicker) {
        ProductPickerDialog(
            products = viewModel.filteredProducts,
            searchQuery = state.searchQuery,
            onSearchChange = { viewModel.updateSearch(it) },
            onSelect = { product ->
                viewModel.addProduct(product)
                viewModel.dismissProductPicker()
            },
            onDismiss = { viewModel.dismissProductPicker() },
        )
    }

    // Variant picker dialog
    if (state.showVariantPicker && state.variantPickerProduct != null) {
        val pickerProduct = state.variantPickerProduct!!
        Dialog(
            onDismissRequest = { viewModel.dismissVariantPicker() },
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AppColors.Surface,
                shadowElevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.select_variant_hint),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        pickerProduct.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val variants = state.variantPickerVariants
                    if (variants.isEmpty()) {
                        Text(
                            stringResource(R.string.purchase_no_products),
                            color = AppColors.TextSecondary,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 400.dp),
                        ) {
                            items(variants, key = { it.id }) { variant ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addProductWithVariant(pickerProduct, variant)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = AppColors.SurfaceVariant,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                variant.variantName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            if (!variant.sku.isNullOrBlank()) {
                                                Text(
                                                    "SKU: ${variant.sku}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = AppColors.TextSecondary,
                                                )
                                            }
                                        }
                                        if (variant.costPrice != null && variant.costPrice > 0) {
                                            Text(
                                                CurrencyFormatter.format(variant.costPrice),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = AppColors.Primary,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Icon(
                                            Icons.Default.AddCircleOutline,
                                            contentDescription = null,
                                            tint = AppColors.Secondary,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { viewModel.dismissVariantPicker() },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }

    // Confirm dialog
    if (showConfirmDialog) {
        val validItems = state.lineItems.filter { it.quantity > 0 }
        val totalQty = validItems.sumOf { it.quantity }.toLong()
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.purchase_confirm_title)) },
            text = { Text(stringResource(R.string.purchase_confirm_msg, validItems.size, totalQty)) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.confirmPurchase()
                    },
                    shape = RoundedCornerShape(8.dp),
                ) { Text(stringResource(R.string.confirm_btn)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.purchase_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showProductPicker() }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.purchase_add_product))
                    }
                },
            )
        },
        bottomBar = {
            if (state.lineItems.isNotEmpty()) {
                val validItems = state.lineItems.filter { it.quantity > 0 }
                val totalQty = validItems.sumOf { it.quantity }.toLong()
                val totalCost = validItems.sumOf { it.costPrice * it.quantity }

                Surface(
                    shadowElevation = 8.dp,
                    color = AppColors.Surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.purchase_summary, validItems.size, totalQty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary,
                            )
                            if (totalCost > 0) {
                                Text(
                                    CurrencyFormatter.format(totalCost),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Primary,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = validItems.isNotEmpty() && !state.isSaving,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Secondary),
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Default.MoveToInbox, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.purchase_confirm_btn),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Supplier selection
                item {
                    SupplierSelector(
                        suppliers = state.suppliers,
                        selectedSupplierId = state.selectedSupplierId,
                        onSelectSupplier = { viewModel.selectSupplier(it) },
                    )
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { viewModel.updateNotes(it) },
                        label = { Text(stringResource(R.string.purchase_notes_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = AppColors.TextTertiary) },
                    )
                }

                // Section header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.purchase_add_product),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        FilledTonalIconButton(
                            onClick = { viewModel.showProductPicker() },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = AppColors.PrimaryContainer,
                                contentColor = AppColors.Primary,
                            ),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.purchase_add_product))
                        }
                    }
                }

                if (state.lineItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.MoveToInbox,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = AppColors.TextTertiary,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.purchase_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextSecondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    items(state.lineItems, key = { it.lineKey }) { item ->
                        PurchaseLineItemCard(
                            item = item,
                            onQuantityChange = { viewModel.updateQuantity(item.lineKey, it) },
                            onCostPriceChange = { viewModel.updateCostPrice(item.lineKey, it) },
                            onRemove = { viewModel.removeProduct(item.lineKey) },
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierSelector(
    suppliers: List<com.minipos.domain.model.Supplier>,
    selectedSupplierId: String?,
    onSelectSupplier: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = suppliers.find { it.id == selectedSupplierId }?.name
        ?: stringResource(R.string.purchase_no_supplier)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.purchase_supplier_label)) },
            leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = AppColors.TextSecondary) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.purchase_no_supplier),
                        color = AppColors.TextSecondary,
                    )
                },
                onClick = {
                    onSelectSupplier(null)
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(20.dp), tint = AppColors.TextTertiary) },
            )
            suppliers.forEach { supplier ->
                DropdownMenuItem(
                    text = { Text(supplier.name) },
                    onClick = {
                        onSelectSupplier(supplier.id)
                        expanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(20.dp), tint = AppColors.Primary) },
                )
            }
        }
    }
}

@Composable
private fun PurchaseLineItemCard(
    item: PurchaseLineItem,
    onQuantityChange: (Double) -> Unit,
    onCostPriceChange: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    var qtyText by remember(item.lineKey, item.quantity) {
        mutableStateOf(
            if (item.quantity == item.quantity.toLong().toDouble()) item.quantity.toLong().toString()
            else item.quantity.toString()
        )
    }
    var costPriceText by remember(item.lineKey, item.costPrice) {
        mutableStateOf(
            if (item.costPrice == 0.0) ""
            else if (item.costPrice == item.costPrice.toLong().toDouble()) item.costPrice.toLong().toString()
            else item.costPrice.toString()
        )
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Product icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppColors.PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.variant != null) {
                        Text(
                            item.variant.variantName,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.Primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        "SKU: ${item.displaySku} · ${stringResource(R.string.purchase_stock_current, item.currentStock.toLong().toString())} ${item.product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }

                // Remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.delete_btn),
                        tint = AppColors.Error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.purchase_qty_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextSecondary,
                )

                FilledTonalIconButton(
                    onClick = {
                        val newQty = (item.quantity - 1).coerceAtLeast(0.0)
                        onQuantityChange(newQty)
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = AppColors.ErrorContainer,
                        contentColor = AppColors.Error,
                    ),
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                }

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { value ->
                        qtyText = value
                        value.toDoubleOrNull()?.let { onQuantityChange(it) }
                    },
                    modifier = Modifier.width(80.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                )

                FilledTonalIconButton(
                    onClick = { onQuantityChange(item.quantity + 1) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = AppColors.SecondaryContainer,
                        contentColor = AppColors.Secondary,
                    ),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    item.product.unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cost price row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.cost_price_input_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextSecondary,
                )

                OutlinedTextField(
                    value = costPriceText,
                    onValueChange = { value ->
                        costPriceText = value
                        value.toDoubleOrNull()?.let { onCostPriceChange(it) }
                        if (value.isEmpty()) onCostPriceChange(0.0)
                    },
                    modifier = Modifier.width(140.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Bold,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    suffix = { Text(stringResource(R.string.currency_symbol)) },
                    placeholder = { Text("0", style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End), color = AppColors.TextTertiary) },
                )
            }
        }
    }
}

@Composable
private fun ProductPickerDialog(
    products: List<Product>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelect: (Product) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(20.dp),
            color = AppColors.Surface,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.purchase_pick_product_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }

                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(stringResource(R.string.purchase_search_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (products.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = AppColors.TextTertiary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.purchase_no_products),
                                color = AppColors.TextSecondary,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductPickerItem(
                                product = product,
                                onClick = { onSelect(product) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductPickerItem(
    product: Product,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppColors.PrimaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "SKU: ${product.sku} · ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                    if (product.hasVariants) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AppColors.SecondaryContainer,
                        ) {
                            Text(
                                stringResource(R.string.has_variants_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.Secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            if (product.costPrice > 0) {
                Text(
                    CurrencyFormatter.format(product.costPrice),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.Primary,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.Default.AddCircleOutline,
                contentDescription = null,
                tint = AppColors.Secondary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
    HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
}
