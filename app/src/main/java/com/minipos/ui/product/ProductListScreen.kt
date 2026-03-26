package com.minipos.ui.product

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onBack: () -> Unit,
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val formState by viewModel.formState.collectAsState()

    if (state.showForm) {
        ProductFormSheet(
            formState = formState,
            categories = state.categories,
            isEditing = state.editingProduct != null,
            onFieldChange = { viewModel.updateFormField(it) },
            onSave = { viewModel.saveProduct() },
            onDismiss = { viewModel.dismissForm() },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sản phẩm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateForm() }) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm sản phẩm")
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
                placeholder = { Text("Tìm sản phẩm…") },
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
                        label = { Text("Tất cả") },
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
                        Text("Chưa có sản phẩm", style = MaterialTheme.typography.titleMedium, color = AppColors.TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.showCreateForm() }, shape = RoundedCornerShape(8.dp)) {
                            Text("Thêm sản phẩm đầu tiên")
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
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xóa sản phẩm?") },
            text = { Text("Bạn có chắc muốn xóa \"${product.name}\"?") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Xóa", color = AppColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Hủy") }
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
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = AppColors.Primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "SKU: ${product.sku} · ${product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    CurrencyFormatter.format(product.sellingPrice),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary,
                )
                if (product.costPrice > 0) {
                    Text(
                        "Vốn: ${CurrencyFormatter.format(product.costPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextTertiary,
                    )
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = AppColors.TextTertiary, modifier = Modifier.size(20.dp))
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
    onFieldChange: (ProductFormState.() -> ProductFormState) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Sửa sản phẩm" else "Thêm sản phẩm") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = { v -> onFieldChange { copy(name = v) } },
                        label = { Text("Tên sản phẩm *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                    )
                }
                item {
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
                            label = { Text("Mã vạch") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
                item {
                    // Category dropdown as filter chips
                    Text("Danh mục", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        item {
                            FilterChip(
                                selected = formState.categoryId == null,
                                onClick = { onFieldChange { copy(categoryId = null) } },
                                label = { Text("Không") },
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
                            label = { Text("Giá vốn") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            suffix = { Text("đ") },
                            shape = RoundedCornerShape(8.dp),
                        )
                        OutlinedTextField(
                            value = formState.sellingPrice,
                            onValueChange = { v -> onFieldChange { copy(sellingPrice = v.filter { it.isDigit() }) } },
                            label = { Text("Giá bán *") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            suffix = { Text("đ") },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formState.unit,
                            onValueChange = { v -> onFieldChange { copy(unit = v) } },
                            label = { Text("Đơn vị") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                        )
                        OutlinedTextField(
                            value = formState.minStock,
                            onValueChange = { v -> onFieldChange { copy(minStock = v.filter { it.isDigit() }) } },
                            label = { Text("Tồn kho tối thiểu") },
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
                        Text("Theo dõi tồn kho")
                        Switch(
                            checked = formState.trackInventory,
                            onCheckedChange = { v -> onFieldChange { copy(trackInventory = v) } },
                        )
                    }
                }
                if (formState.error != null) {
                    item {
                        Text(formState.error!!, color = AppColors.Error, style = MaterialTheme.typography.bodySmall)
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
                    Text("Lưu")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}
