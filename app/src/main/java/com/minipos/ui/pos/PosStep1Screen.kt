package com.minipos.ui.pos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.Category
import com.minipos.domain.model.Product
import com.minipos.ui.scanner.BarcodeScannerScreen
import com.minipos.ui.scanner.ImageViewerScreen
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosStep1Screen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: PosStep1ViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val cart by viewModel.cartHolder.cart.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Full-screen barcode scanner overlay
    if (state.showBarcodeScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { value, _ -> viewModel.onBarcodeScanned(value) },
            onClose = { viewModel.dismissBarcodeScanner() },
            title = "Quét mã vạch sản phẩm",
        )
        return
    }

    // Show stock error as snackbar
    LaunchedEffect(state.stockError) {
        state.stockError?.let { error ->
            snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Short)
            viewModel.clearStockError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Bước 1: Chọn sản phẩm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
        bottomBar = {
            if (!cart.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = AppColors.Primary,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNext)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "${cart.itemCount} sản phẩm · ${cart.totalQuantity.toInt()} item",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Text(
                                CurrencyFormatter.format(cart.subtotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tiếp theo", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
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
                trailingIcon = {
                    IconButton(onClick = { viewModel.showBarcodeScanner() }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Quét mã vạch")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            // Category tabs
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("Tất cả") },
                    )
                }
                items(state.categories) { category ->
                    FilterChip(
                        selected = state.selectedCategory?.id == category.id,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text("${category.icon ?: ""} ${category.name}") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product grid
            if (state.products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Chưa có sản phẩm", color = AppColors.TextSecondary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.products) { product ->
                        val qty = cart.items.firstOrNull { it.product.id == product.id }?.quantity?.toInt() ?: 0
                        val availableStock = viewModel.cartHolder.getAvailableStock(product.id)
                        ProductGridItem(product = product, quantity = qty, availableStock = availableStock) {
                            viewModel.addToCart(product)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ProductGridItem(product: Product, quantity: Int, availableStock: Double?, onClick: () -> Unit) {
    val isOutOfStock = product.trackInventory && (availableStock ?: 0.0) <= 0.0
    var showImageViewer by remember { mutableStateOf(false) }

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = true,
                onClick = { if (!isOutOfStock) onClick() },
                onLongClick = { if (allImages.isNotEmpty()) showImageViewer = true },
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOutOfStock -> AppColors.ErrorContainer.copy(alpha = 0.3f)
                quantity > 0 -> AppColors.PrimaryContainer
                else -> AppColors.Surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (product.imagePath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(product.imagePath))
                            .crossfade(true)
                            .build(),
                        contentDescription = product.name,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = when {
                            isOutOfStock -> AppColors.Error
                            quantity > 0 -> AppColors.Primary
                            else -> AppColors.TextSecondary
                        },
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    product.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    CurrencyFormatter.format(product.sellingPrice),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Secondary,
                )
                // Show stock info
                if (product.trackInventory && availableStock != null) {
                    Text(
                        if (isOutOfStock) "Hết hàng" else "Kho: ${availableStock.toLong()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOutOfStock) AppColors.Error else AppColors.TextTertiary,
                    )
                }
            }
            if (quantity > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    containerColor = AppColors.Primary,
                ) {
                    Text("$quantity")
                }
            }
        }
    }
}
