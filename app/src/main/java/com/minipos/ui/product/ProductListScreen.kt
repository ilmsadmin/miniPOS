package com.minipos.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minipos.core.theme.AppColors
import com.minipos.R
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.Product
import com.minipos.ui.scanner.BarcodeScannerScreen
import com.minipos.ui.scanner.ImageViewerScreen
import java.io.File
import com.minipos.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onBack: () -> Unit,
    onNavigateToForm: (String?) -> Unit = {},
    onNavigateToStockManagement: () -> Unit = {},
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Refresh data when screen resumes (e.g., coming back from ProductFormScreen)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Build category lookup map for display
    val categoryMap = remember(state.categories) {
        state.categories.associateBy { it.id }
    }

    if (state.showForm) {
        // Navigate to full-screen product form instead of dialog
        LaunchedEffect(state.showForm) {
            val editingId = state.editingProduct?.id
            viewModel.dismissForm()
            onNavigateToForm(editingId)
        }
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
        containerColor = AppColors.Background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ── Top Bar: back + title + gradient Add button ──
            MiniPosTopBar(
                title = stringResource(R.string.products_title),
                onBack = onBack,
                actions = {
                    // Gradient circle add button (40dp) matching HTML .add-btn
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MiniPosGradients.primary())
                            .clickable { onNavigateToForm(null) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_product_cd),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
            )

            // ── Page body ──
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // ── Search + Scan row ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiniPosSearchBar(
                            value = state.searchQuery,
                            onValueChange = { viewModel.search(it) },
                            placeholder = stringResource(R.string.product_search_hint),
                            modifier = Modifier.weight(1f),
                        )
                        // Scan barcode button (44dp circle)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.dp, AppColors.Border, CircleShape)
                                .background(AppColors.Surface)
                                .clickable { viewModel.showBarcodeScanner() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = stringResource(R.string.prod_scan_barcode),
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }

                // ── Category filter chips ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProductFilterChip(
                            label = stringResource(R.string.filter_all),
                            isActive = state.selectedCategory == null,
                            onClick = { viewModel.filterByCategory(null) },
                        )
                        state.categories.forEach { category ->
                            ProductFilterChip(
                                label = category.name,
                                isActive = state.selectedCategory?.id == category.id,
                                onClick = { viewModel.filterByCategory(category) },
                            )
                        }
                    }
                }

                // ── Product count ──
                item {
                    Text(
                        text = stringResource(R.string.prod_showing_count, state.products.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextTertiary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                // ── Stock Management tip banner ──
                if (!state.isLoading && state.products.isNotEmpty()) {
                    val lowOrOutCount = state.products.count { product ->
                        if (!product.trackInventory) return@count false
                        val stock = (state.stockMap[product.id] ?: 0.0).toLong().toInt()
                        stock <= product.minStock
                    }
                    item {
                        StockManagementBanner(
                            lowOrOutCount = lowOrOutCount,
                            onClick = onNavigateToStockManagement,
                        )
                    }
                }

                // ── Loading / Empty / List ──
                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = AppColors.Primary)
                        }
                    }
                } else if (state.products.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.SurfaceElevated),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = AppColors.TextTertiary,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.no_products),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextSecondary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MiniPosGradientButton(
                                text = stringResource(R.string.add_first_product),
                                onClick = { onNavigateToForm(null) },
                                modifier = Modifier.width(200.dp),
                                height = 44.dp,
                            )
                        }
                    }
                } else {
                    items(state.products) { product ->
                        val currentStock = state.stockMap[product.id] ?: 0.0
                        val categoryName = product.categoryId?.let { categoryMap[it]?.name }

                        ProductCard(
                            product = product,
                            currentStock = currentStock,
                            categoryName = categoryName,
                            onClick = { onNavigateToForm(product.id) },
                            onDelete = { viewModel.deleteProduct(product) },
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// STOCK MANAGEMENT BANNER — guides new users
// ═══════════════════════════════════════════════

@Composable
private fun StockManagementBanner(
    lowOrOutCount: Int,
    onClick: () -> Unit,
) {
    val hasIssue = lowOrOutCount > 0
    val bannerBg = if (hasIssue) AppColors.WarningSoft else AppColors.InfoSoft
    val accentColor = if (hasIssue) AppColors.Warning else AppColors.PrimaryLight
    val icon = if (hasIssue) Icons.Rounded.Warning else Icons.Rounded.Inventory2

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(bannerBg)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (hasIssue) stringResource(R.string.prod_stock_banner_warning, lowOrOutCount)
                       else stringResource(R.string.prod_stock_banner_title),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.prod_stock_banner_desc),
                fontSize = 11.sp,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        // Arrow CTA
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ArrowForward,
                contentDescription = stringResource(R.string.prod_stock_banner_title),
                tint = accentColor,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════
// FILTER CHIP (matching HTML .chip)
// ═══════════════════════════════════════════════

@Composable
private fun ProductFilterChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isActive) {
        Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight))
    } else {
        Brush.linearGradient(listOf(AppColors.Surface, AppColors.Surface))
    }
    val borderColor = if (isActive) Color.Transparent else AppColors.Border
    val textColor = if (isActive) Color.White else AppColors.TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
            .background(bgColor)
            .then(
                if (!isActive) Modifier.border(
                    1.dp,
                    borderColor,
                    RoundedCornerShape(MiniPosTokens.RadiusFull)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

// ═══════════════════════════════════════════════
// PRODUCT CARD (matching HTML .prod)
// ═══════════════════════════════════════════════

/**
 * Maps a category name to its gradient colors and icon.
 */
@Composable
private fun getCategoryStyle(categoryName: String?): Triple<List<Color>, List<Color>, ImageVector> {
    return when (categoryName?.lowercase()) {
        "nước uống", "đồ uống", "drinks" -> Triple(
            listOf(Color(0xFF4BB8F0), Color(0xFF2196F3)),
            listOf(Color(0xFF4BB8F0), Color(0xFF2196F3)),
            Icons.Rounded.LocalCafe,
        )
        "thực phẩm", "food" -> Triple(
            listOf(Color(0xFFFF8A65), Color(0xFFF44336)),
            listOf(Color(0xFFFF8A65), Color(0xFFF44336)),
            Icons.Rounded.Restaurant,
        )
        "bánh kẹo", "snack", "snacks" -> Triple(
            listOf(Color(0xFFFFD54F), Color(0xFFFFB300)),
            listOf(Color(0xFFFFD54F), Color(0xFFFFB300)),
            Icons.Rounded.Cookie,
        )
        "gia dụng", "đồ dùng", "household" -> Triple(
            listOf(Color(0xFF81C784), Color(0xFF388E3C)),
            listOf(Color(0xFF81C784), Color(0xFF388E3C)),
            Icons.Rounded.CleaningServices,
        )
        "sữa", "dairy" -> Triple(
            listOf(Color(0xFFCE93D8), Color(0xFF8E24AA)),
            listOf(Color(0xFFCE93D8), Color(0xFF8E24AA)),
            Icons.Rounded.WaterDrop,
        )
        else -> Triple(
            listOf(Color(0xFF90A4AE), Color(0xFF546E7A)),
            listOf(Color(0xFF90A4AE), Color(0xFF546E7A)),
            Icons.Rounded.Inventory2,
        )
    }
}

@Composable
private fun ProductCard(
    product: Product,
    currentStock: Double,
    categoryName: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }

    // Full-screen image viewer (single image only)
    if (showImageViewer && product.imagePath != null) {
        Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            ImageViewerScreen(
                images = listOf(product.imagePath!!),
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

    val stockInt = currentStock.toLong().toInt()
    val isLow = product.trackInventory && stockInt in 1..product.minStock
    val isOut = product.trackInventory && stockInt <= 0

    val (gradientColors, _, categoryIcon) = getCategoryStyle(categoryName)

    // Card matching HTML .prod
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Category gradient icon (48dp) ──
        if (product.imagePath != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(product.imagePath))
                    .crossfade(true)
                    .build(),
                contentDescription = product.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                    .clickable { showImageViewer = true },
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    categoryIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Body: name + meta ──
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = product.sku,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextTertiary,
                )
                Text(
                    text = "·",
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                )
                Text(
                    text = categoryName ?: stringResource(R.string.prod_no_category),
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // ── Right: price + stock status ──
        Column(
            horizontalAlignment = Alignment.End,
        ) {
            // Price (accent color, 14sp, extra bold)
            Text(
                text = CurrencyFormatter.format(product.sellingPrice),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = AppColors.Accent,
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Stock status text (colored)
            when {
                isOut -> Text(
                    text = stringResource(R.string.prod_stock_out),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Error,
                )
                isLow -> Text(
                    text = stringResource(R.string.prod_stock_low, stockInt),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Warning,
                )
                product.trackInventory -> Text(
                    text = stringResource(R.string.prod_stock_ok, stockInt),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Success,
                )
            }
        }
    }
}
