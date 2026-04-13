package com.minipos.ui.purchase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import com.minipos.ui.components.*

// ═══════════════════════════════════════════════════════════════
// PURCHASE SCREEN — Redesigned to match purchase-order.html mock
// ═══════════════════════════════════════════════════════════════

@Composable
fun PurchaseScreen(
    onBack: () -> Unit,
    viewModel: PurchaseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    // Barcode scanner overlay
    if (showBarcodeScanner) {
        com.minipos.ui.scanner.BarcodeScannerScreen(
            onBarcodeScanned = { value, _ ->
                viewModel.addProductByBarcode(value)
                showBarcodeScanner = false
            },
            onClose = { showBarcodeScanner = false },
            title = stringResource(R.string.scan_barcode_to_add),
        )
        return
    }

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
        VariantPickerDialog(
            product = state.variantPickerProduct!!,
            variants = state.variantPickerVariants,
            onSelectVariant = { product, variant ->
                viewModel.addProductWithVariant(product, variant)
            },
            onDismiss = { viewModel.dismissVariantPicker() },
        )
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
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
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
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                // ── Top bar ──
                MiniPosTopBar(
                    title = stringResource(R.string.purchase_title),
                    onBack = onBack,
                )

                // ── Scrollable body ──
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ─── Supplier field ───
                    item {
                        FieldSection(label = stringResource(R.string.purchase_supplier_label)) {
                            SupplierSelector(
                                suppliers = state.suppliers,
                                selectedSupplierId = state.selectedSupplierId,
                                onSelectSupplier = { viewModel.selectSupplier(it) },
                            )
                        }
                    }

                    // ─── Date & PO Code row ───
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            FieldSection(
                                label = stringResource(R.string.purchase_date_label),
                                modifier = Modifier.weight(1f),
                            ) {
                                ReadOnlyField(value = state.purchaseDate)
                            }
                            FieldSection(
                                label = stringResource(R.string.purchase_code_label),
                                modifier = Modifier.weight(1f),
                            ) {
                                ReadOnlyField(
                                    value = state.purchaseCode,
                                    textColor = AppColors.TextTertiary,
                                )
                            }
                        }
                    }

                    // ─── Section label: Products ───
                    item {
                        SectionTitle(title = stringResource(R.string.purchase_product_list_section))
                    }

                    // ─── Search + Scan row ───
                    item {
                        SearchAndScanBar(
                            searchQuery = state.inlineSearchQuery,
                            onSearchChange = { viewModel.updateInlineSearch(it) },
                            onScanClick = { showBarcodeScanner = true },
                        )
                    }

                    // ─── Inline search suggestions ───
                    val suggestions = viewModel.inlineFilteredProducts
                    if (suggestions.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
                                color = AppColors.Surface,
                                shadowElevation = 4.dp,
                            ) {
                                Column {
                                    suggestions.forEach { product ->
                                        InlineProductSuggestion(
                                            product = product,
                                            onClick = {
                                                viewModel.addProduct(product)
                                                viewModel.updateInlineSearch("")
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ─── PO Line items ───
                    if (state.lineItems.isNotEmpty()) {
                        items(state.lineItems, key = { it.lineKey }) { item ->
                            PurchaseItemCard(
                                item = item,
                                onQuantityChange = { viewModel.updateQuantity(item.lineKey, it) },
                                onCostPriceChange = { viewModel.updateCostPrice(item.lineKey, it) },
                                onRemove = { viewModel.removeProduct(item.lineKey) },
                            )
                        }
                    }

                    // ─── Add product button (dashed) ───
                    item {
                        DashedAddButton(
                            text = stringResource(R.string.purchase_add_product),
                            onClick = { viewModel.showProductPicker() },
                        )
                    }

                    // ─── Empty state ───
                    if (state.lineItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.MoveToInbox,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = AppColors.TextTertiary,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        stringResource(R.string.purchase_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.TextSecondary,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }

                    // ─── Summary card ───
                    if (state.lineItems.isNotEmpty()) {
                        item {
                            val validItems = state.lineItems.filter { it.quantity > 0 }
                            val totalQty = validItems.sumOf { it.quantity }.toLong()
                            val totalCost = validItems.sumOf { it.costPrice * it.quantity }

                            PurchaseSummaryCard(
                                typesCount = validItems.size,
                                unitsCount = totalQty,
                                totalCost = totalCost,
                            )
                        }
                    }

                    // ─── Confirm button ───
                    if (state.lineItems.isNotEmpty()) {
                        item {
                            val validItems = state.lineItems.filter { it.quantity > 0 }
                            MiniPosGradientButton(
                                text = stringResource(R.string.purchase_confirm_btn),
                                onClick = { showConfirmDialog = true },
                                enabled = validItems.isNotEmpty() && !state.isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.Verified,
                            )
                        }
                    }

                    // Bottom spacing
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// FIELD SECTION (label + content)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FieldSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextTertiary,
            letterSpacing = 0.5.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

// ═══════════════════════════════════════════════════════════════
// READ-ONLY FIELD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ReadOnlyField(
    value: String,
    textColor: Color = AppColors.TextPrimary,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(AppColors.InputBackground, RoundedCornerShape(MiniPosTokens.RadiusMd))
            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(MiniPosTokens.RadiusMd))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// SUPPLIER SELECTOR — MiniPosSelectBox
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SupplierSelector(
    suppliers: List<com.minipos.domain.model.Supplier>,
    selectedSupplierId: String?,
    onSelectSupplier: (String?) -> Unit,
) {
    val noSupplierLabel = stringResource(R.string.purchase_no_supplier)
    val selectTitle = stringResource(R.string.purchase_supplier_label)

    val items = buildList {
        add(
            SelectListItem(
                id = "__none__",
                name = noSupplierLabel,
                icon = Icons.Default.Block,
                iconTint = AppColors.TextTertiary,
            )
        )
        suppliers.forEach { supplier ->
            add(
                SelectListItem(
                    id = supplier.id,
                    name = supplier.name,
                    icon = Icons.Default.Business,
                    iconTint = AppColors.Primary,
                )
            )
        }
    }

    MiniPosSelectBox(
        label = "",
        title = selectTitle,
        items = items,
        selectedId = selectedSupplierId ?: "__none__",
        placeholder = noSupplierLabel,
        onSelect = { item ->
            onSelectSupplier(if (item.id == "__none__") null else item.id)
        },
    )
}

// ═══════════════════════════════════════════════════════════════
// SEARCH + SCAN BAR (inline on page, matching mock)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SearchAndScanBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onScanClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Search bar
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .background(AppColors.InputBackground, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        stringResource(R.string.purchase_search_or_scan_hint),
                        color = AppColors.TextTertiary,
                        fontSize = 13.sp,
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    textStyle = TextStyle(
                        color = AppColors.TextPrimary,
                        fontSize = 13.sp,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (searchQuery.isNotEmpty()) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = null,
                    tint = AppColors.TextTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onSearchChange("") },
                )
            }
        }

        // Scan button (gradient circle)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(AppColors.Primary, AppColors.PrimaryLight)
                    )
                )
                .clickable(onClick = onScanClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = stringResource(R.string.scan_barcode_title),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// INLINE PRODUCT SUGGESTION (for search results)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun InlineProductSuggestion(
    product: Product,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Inventory2,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                product.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "SKU: ${product.sku}",
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
        }
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            tint = AppColors.Secondary,
            modifier = Modifier.size(20.dp),
        )
    }
    HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
}

// ═══════════════════════════════════════════════════════════════
// PURCHASE ITEM CARD (matching mock .poi design)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PurchaseItemCard(
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

    val lineTotal = item.quantity * item.costPrice

    Surface(
        shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.Border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // ── Top row: name + delete ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Delete button (red circle)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AppColors.ErrorContainer)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.purchase_remove_product),
                        tint = AppColors.Error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Fields row: Quantity + Cost price ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Quantity field
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.purchase_qty_label),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextTertiary,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    BasicTextField(
                        value = qtyText,
                        onValueChange = { value ->
                            qtyText = value
                            value.toDoubleOrNull()?.let { onQuantityChange(it) }
                        },
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                            textAlign = TextAlign.End,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(
                                AppColors.InputBackground,
                                RoundedCornerShape(MiniPosTokens.RadiusSm)
                            )
                            .border(
                                1.dp,
                                AppColors.Border,
                                RoundedCornerShape(MiniPosTokens.RadiusSm)
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }

                // Cost price field
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.cost_price_input_label),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextTertiary,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    BasicTextField(
                        value = costPriceText,
                        onValueChange = { value ->
                            costPriceText = value
                            value.replace(",", "").toDoubleOrNull()?.let { onCostPriceChange(it) }
                            if (value.isEmpty()) onCostPriceChange(0.0)
                        },
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                            textAlign = TextAlign.End,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(
                                AppColors.InputBackground,
                                RoundedCornerShape(MiniPosTokens.RadiusSm)
                            )
                            .border(
                                1.dp,
                                AppColors.Border,
                                RoundedCornerShape(MiniPosTokens.RadiusSm)
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            }

            // ── Subtotal ──
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.purchase_line_subtotal, CurrencyFormatter.format(lineTotal)),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.Accent,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DASHED ADD BUTTON
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DashedAddButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(
                2.dp,
                AppColors.BorderLight,
                RoundedCornerShape(MiniPosTokens.RadiusMd),
            )
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextTertiary,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SUMMARY CARD (matching mock .po-summary)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PurchaseSummaryCard(
    typesCount: Int,
    unitsCount: Long,
    totalCost: Double,
) {
    Surface(
        shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.Border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Total products row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.purchase_summary_products),
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary,
                )
                Text(
                    stringResource(R.string.purchase_summary_types_units, typesCount, unitsCount),
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Divider
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 2.dp, color = AppColors.BorderLight)
            Spacer(modifier = Modifier.height(8.dp))

            // Grand total row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.purchase_summary_total),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = AppColors.TextPrimary,
                )
                Text(
                    CurrencyFormatter.format(totalCost),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = AppColors.TextPrimary,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// VARIANT PICKER DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
private fun VariantPickerDialog(
    product: Product,
    variants: List<com.minipos.domain.model.ProductVariant>,
    onSelectVariant: (Product, com.minipos.domain.model.ProductVariant) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
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
                    product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(12.dp))

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
                                    .clickable { onSelectVariant(product, variant) },
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
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PRODUCT PICKER DIALOG (full-screen style)
// ═══════════════════════════════════════════════════════════════

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
                    shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
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

// ═══════════════════════════════════════════════════════════════
// PRODUCT PICKER ITEM
// ═══════════════════════════════════════════════════════════════

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
