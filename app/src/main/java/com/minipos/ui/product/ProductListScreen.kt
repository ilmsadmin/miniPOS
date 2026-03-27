package com.minipos.ui.product

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minipos.core.theme.AppColors
import com.minipos.R
import com.minipos.core.barcode.BarcodeGenerator
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.UuidGenerator
import com.minipos.domain.model.Product
import com.minipos.domain.model.ProductVariant
import com.minipos.ui.scanner.BarcodeScannerScreen
import com.minipos.ui.scanner.ImageViewerScreen
import com.minipos.ui.scanner.ProductImagePicker
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onBack: () -> Unit,
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val variantFormState by viewModel.variantFormState.collectAsState()

    if (state.showForm) {
        ProductFormSheet(
            formState = formState,
            categories = state.categories,
            isEditing = state.editingProduct != null,
            productId = state.editingProduct?.id ?: remember { UuidGenerator.generate() },
            variants = state.variants,
            onFieldChange = { viewModel.updateFormField(it) },
            onSave = { viewModel.saveProduct() },
            onDismiss = { viewModel.dismissForm() },
            onScanBarcode = { viewModel.showBarcodeScanner() },
            onGenerateBarcode = { viewModel.generateBarcode() },
            onAddVariant = { viewModel.showCreateVariantForm() },
            onEditVariant = { viewModel.showEditVariantForm(it) },
            onDeleteVariant = { viewModel.deleteVariant(it) },
        )
    }

    // Variant form dialog
    if (state.showVariantForm) {
        VariantFormDialog(
            formState = variantFormState,
            isEditing = state.editingVariant != null,
            onFieldChange = { viewModel.updateVariantFormField(it) },
            onSave = { viewModel.saveVariant() },
            onDismiss = { viewModel.dismissVariantForm() },
        )
    }

    // Full-screen barcode scanner overlay
    if (state.showBarcodeScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { value, _ -> viewModel.onBarcodeScanned(value) },
            onClose = { viewModel.dismissBarcodeScanner() },
            title = stringResource(R.string.scan_product_barcode_title),
        )
        return // Don't render the main screen behind the scanner
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.products_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateForm() }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_product_cd))
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
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.search(it) },
                placeholder = { Text(stringResource(R.string.product_search_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            // Category filter
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.filterByCategory(null) },
                        label = { Text(stringResource(R.string.filter_all)) },
                    )
                }
                items(state.categories) { category ->
                    FilterChip(
                        selected = state.selectedCategory?.id == category.id,
                        onClick = { viewModel.filterByCategory(category) },
                        label = { Text(category.name) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColors.TextTertiary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.no_products), style = MaterialTheme.typography.titleMedium, color = AppColors.TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.showCreateForm() }, shape = RoundedCornerShape(8.dp)) {
                            Text(stringResource(R.string.add_first_product))
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.products) { product ->
                        ProductListItem(
                            product = product,
                            currentStock = state.stockMap[product.id] ?: 0.0,
                            onClick = { viewModel.showEditForm(product) },
                            onDelete = { viewModel.deleteProduct(product) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductListItem(
    product: Product,
    currentStock: Double,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }

    // Build all images for viewer
    val allImages = remember(product) {
        buildList {
            product.imagePath?.let { add(it) }
            addAll(product.additionalImages)
        }
    }

    // Full-screen image viewer
    if (showImageViewer && allImages.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            ImageViewerScreen(
                images = allImages,
                initialIndex = 0,
                onClose = { showImageViewer = false },
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_product_title)) },
            text = { Text(stringResource(R.string.delete_product_confirm, product.name)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.delete_label), color = AppColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel_btn_label)) }
            },
        )
    }

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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (product.imagePath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(product.imagePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showImageViewer = true },
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = AppColors.Primary,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val stockDisplay = if (currentStock == currentStock.toLong().toDouble()) {
                    currentStock.toLong().toString()
                } else {
                    currentStock.toString()
                }
                val skuUnit = buildString {
                    append("SKU: ${product.sku} · $stockDisplay ${product.unit}")
                    if (allImages.size > 1) append("  📷${allImages.size}")
                }
                Text(
                    skuUnit,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 130.dp)) {
                Text(
                    CurrencyFormatter.format(product.sellingPrice),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary,
                    maxLines = 1,
                )
                if (product.costPrice > 0) {
                    Text(
                        stringResource(R.string.cost_prefix, CurrencyFormatter.format(product.costPrice)),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextTertiary,
                        maxLines = 1,
                    )
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_label), tint = AppColors.TextTertiary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormSheet(
    formState: ProductFormState,
    categories: List<com.minipos.domain.model.Category>,
    isEditing: Boolean,
    productId: String,
    variants: List<com.minipos.domain.model.ProductVariant>,
    onFieldChange: (ProductFormState.() -> ProductFormState) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onScanBarcode: () -> Unit,
    onGenerateBarcode: () -> Unit,
    onAddVariant: () -> Unit,
    onEditVariant: (com.minipos.domain.model.ProductVariant) -> Unit,
    onDeleteVariant: (com.minipos.domain.model.ProductVariant) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) stringResource(R.string.edit_product_title) else stringResource(R.string.add_product_title)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = { v -> onFieldChange { copy(name = v) } },
                        label = { Text(stringResource(R.string.product_name_required)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = formState.sku,
                            onValueChange = { v -> onFieldChange { copy(sku = v) } },
                            label = { Text("SKU") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                        )
                        OutlinedTextField(
                            value = formState.barcode,
                            onValueChange = { v -> onFieldChange { copy(barcode = v) } },
                            label = { Text(stringResource(R.string.product_barcode_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = onGenerateBarcode, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.generate_barcode_cd), tint = AppColors.Accent, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = onScanBarcode, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.scan_barcode_cd), tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                        )
                    }
                }
                // Barcode preview
                if (formState.barcode.length == 13) {
                    item {
                        val barcodeBitmap = remember(formState.barcode) {
                            try { BarcodeGenerator.generateBarcodeBitmap(formState.barcode, width = 300, height = 100, showText = true) }
                            catch (_: Exception) { null }
                        }
                        if (barcodeBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    bitmap = barcodeBitmap.asImageBitmap(),
                                    contentDescription = "Barcode preview",
                                    modifier = Modifier.height(60.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }
                }
                item {
                    // Category dropdown as filter chips
                    Text(stringResource(R.string.category_label), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        item {
                            FilterChip(
                                selected = formState.categoryId == null,
                                onClick = { onFieldChange { copy(categoryId = null) } },
                                label = { Text(stringResource(R.string.no_category_chip)) },
                            )
                        }
                        items(categories) { cat ->
                            FilterChip(
                                selected = formState.categoryId == cat.id,
                                onClick = { onFieldChange { copy(categoryId = cat.id) } },
                                label = { Text(cat.name) },
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formState.costPrice,
                            onValueChange = { v -> onFieldChange { copy(costPrice = v.filter { it.isDigit() }) } },
                            label = { Text(stringResource(R.string.cost_price_label)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            suffix = { Text(stringResource(R.string.currency_symbol)) },
                            shape = RoundedCornerShape(8.dp),
                        )
                        OutlinedTextField(
                            value = formState.sellingPrice,
                            onValueChange = { v -> onFieldChange { copy(sellingPrice = v.filter { it.isDigit() }) } },
                            label = { Text(stringResource(R.string.selling_price_label)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            suffix = { Text(stringResource(R.string.currency_symbol)) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formState.unit,
                            onValueChange = { v -> onFieldChange { copy(unit = v) } },
                            label = { Text(stringResource(R.string.unit_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                        )
                        OutlinedTextField(
                            value = formState.minStock,
                            onValueChange = { v -> onFieldChange { copy(minStock = v.filter { it.isDigit() }) } },
                            label = { Text(stringResource(R.string.min_stock_label)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.track_inventory))
                        Switch(
                            checked = formState.trackInventory,
                            onCheckedChange = { v -> onFieldChange { copy(trackInventory = v) } },
                        )
                    }
                }
                // Has Variants toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.has_variants_label))
                        Switch(
                            checked = formState.hasVariants,
                            onCheckedChange = { v -> onFieldChange { copy(hasVariants = v) } },
                        )
                    }
                }
                // Variants list
                if (formState.hasVariants) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.variants_label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                            )
                            TextButton(onClick = onAddVariant) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.add_variant))
                            }
                        }
                    }
                    if (variants.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_variants_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextTertiary,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    } else {
                        items(variants) { variant ->
                            VariantItem(
                                variant = variant,
                                onEdit = { onEditVariant(variant) },
                                onDelete = { onDeleteVariant(variant) },
                            )
                        }
                    }
                }
                item {
                    ProductImagePicker(
                        mainImagePath = formState.imagePath,
                        additionalImages = formState.additionalImages,
                        onMainImageChanged = { path -> onFieldChange { copy(imagePath = path) } },
                        onAdditionalImagesChanged = { images -> onFieldChange { copy(additionalImages = images) } },
                        productId = productId,
                    )
                }
                if (formState.error != null) {
                    item {
                        Text(formState.error, color = AppColors.Error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !formState.isSaving,
                shape = RoundedCornerShape(8.dp),
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.save_btn))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_btn_label)) }
        },
    )
}

@Composable
private fun VariantItem(
    variant: ProductVariant,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_variant_title)) },
            text = { Text(stringResource(R.string.delete_variant_confirm, variant.variantName)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.delete_label), color = AppColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel_btn_label)) }
            },
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Style,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AppColors.Accent,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    variant.variantName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                val details = buildString {
                    append("SKU: ${variant.sku}")
                    variant.sellingPrice?.let { append(" · ${CurrencyFormatter.format(it)}") }
                }
                Text(
                    details,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_label), tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun VariantFormDialog(
    formState: VariantFormState,
    isEditing: Boolean,
    onFieldChange: (VariantFormState.() -> VariantFormState) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) stringResource(R.string.edit_variant_title) else stringResource(R.string.add_variant_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.variantName,
                    onValueChange = { v -> onFieldChange { copy(variantName = v) } },
                    label = { Text(stringResource(R.string.variant_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.sku,
                        onValueChange = { v -> onFieldChange { copy(sku = v) } },
                        label = { Text("SKU") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                    )
                    OutlinedTextField(
                        value = formState.barcode,
                        onValueChange = { v -> onFieldChange { copy(barcode = v) } },
                        label = { Text(stringResource(R.string.product_barcode_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.costPrice,
                        onValueChange = { v -> onFieldChange { copy(costPrice = v.filter { it.isDigit() }) } },
                        label = { Text(stringResource(R.string.cost_price_label)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        suffix = { Text(stringResource(R.string.currency_symbol)) },
                        shape = RoundedCornerShape(8.dp),
                    )
                    OutlinedTextField(
                        value = formState.sellingPrice,
                        onValueChange = { v -> onFieldChange { copy(sellingPrice = v.filter { it.isDigit() }) } },
                        label = { Text(stringResource(R.string.selling_price_label)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        suffix = { Text(stringResource(R.string.currency_symbol)) },
                        shape = RoundedCornerShape(8.dp),
                    )
                }
                OutlinedTextField(
                    value = formState.attributes,
                    onValueChange = { v -> onFieldChange { copy(attributes = v) } },
                    label = { Text(stringResource(R.string.variant_attributes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text(stringResource(R.string.variant_attributes_hint)) },
                )
                if (formState.error != null) {
                    Text(formState.error, color = AppColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !formState.isSaving,
                shape = RoundedCornerShape(8.dp),
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.save_btn))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_btn_label)) }
        },
    )
}
