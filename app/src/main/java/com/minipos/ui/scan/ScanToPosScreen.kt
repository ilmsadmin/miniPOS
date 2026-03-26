package com.minipos.ui.scan

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.ui.scanner.BarcodeScannerScreen
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanToPosScreen(
    onBack: () -> Unit,
    onGoToPos: () -> Unit,
    viewModel: ScanToPosViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Auto-dismiss success message
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            delay(2000)
            viewModel.clearSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isScanning) {
            // Full-screen camera scanner
            BarcodeScannerScreen(
                onBarcodeScanned = { value, _ ->
                    viewModel.onBarcodeScanned(value)
                },
                onClose = {
                    if (state.scannedProducts.isEmpty()) {
                        onBack()
                    } else {
                        viewModel.stopScanning()
                    }
                },
                title = "Quét mã vạch sản phẩm",
            )

            // Scanned products count badge
            if (state.scannedProducts.isNotEmpty()) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 18.dp, end = 60.dp),
                    containerColor = AppColors.Primary,
                    contentColor = Color.White,
                ) {
                    Text(
                        "${state.scannedProducts.size}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        } else {
            // Product list view
            ScannedProductListView(
                state = state,
                onBack = onBack,
                onResumeScanning = { viewModel.resumeScanning() },
                onToggleSelection = { viewModel.toggleSelection(it) },
                onUpdateQuantity = { idx, qty -> viewModel.updateQuantity(idx, qty) },
                onRemoveProduct = { viewModel.removeProduct(it) },
                onClearError = { viewModel.clearError() },
                onTransferToPos = {
                    if (viewModel.transferToPos()) {
                        onGoToPos()
                    }
                },
                selectedCount = viewModel.selectedCount,
                selectedTotalQuantity = viewModel.selectedTotalQuantity,
                selectedTotalAmount = viewModel.selectedTotalAmount,
            )
        }

        // Success snackbar overlay
        AnimatedVisibility(
            visible = state.successMessage != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (state.isScanning) 70.dp else 80.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppColors.Secondary,
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(state.successMessage ?: "", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Error snackbar overlay
        AnimatedVisibility(
            visible = state.errorMessage != null && !state.isScanning,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppColors.Error,
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(
                        state.errorMessage ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannedProductListView(
    state: ScanToPosState,
    onBack: () -> Unit,
    onResumeScanning: () -> Unit,
    onToggleSelection: (Int) -> Unit,
    onUpdateQuantity: (Int, Int) -> Unit,
    onRemoveProduct: (Int) -> Unit,
    onClearError: () -> Unit,
    onTransferToPos: () -> Unit,
    selectedCount: Int,
    selectedTotalQuantity: Int,
    selectedTotalAmount: Double,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sản phẩm đã quét", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.scannedProducts.size} sản phẩm",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                },
                actions = {
                    // Scan more button
                    FilledTonalButton(
                        onClick = onResumeScanning,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AppColors.Primary.copy(alpha = 0.1f),
                            contentColor = AppColors.Primary,
                        ),
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quét tiếp")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Surface),
            )
        },
        bottomBar = {
            // Transfer to POS bottom bar
            if (state.scannedProducts.isNotEmpty()) {
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
                        // Summary row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    "Đã chọn: $selectedCount SP · $selectedTotalQuantity đơn vị",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary,
                                )
                                Text(
                                    CurrencyFormatter.format(selectedTotalAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Primary,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Transfer button
                        Button(
                            onClick = onTransferToPos,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                            enabled = selectedCount > 0,
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chuyển sang POS", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        if (state.scannedProducts.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = AppColors.TextTertiary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Chưa quét sản phẩm nào",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Nhấn \"Quét tiếp\" để bắt đầu quét mã vạch sản phẩm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextTertiary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onResumeScanning,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bắt đầu quét")
                }
            }
        } else {
            // Error banner (e.g. "not found" from last scan)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Error banner
                if (state.errorMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.ErrorContainer),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    state.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.Error,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Đóng", modifier = Modifier.size(16.dp), tint = AppColors.Error)
                                }
                            }
                        }
                    }
                }

                // Product items
                itemsIndexed(state.scannedProducts) { index, item ->
                    ScannedProductCard(
                        item = item,
                        onToggleSelection = { onToggleSelection(index) },
                        onUpdateQuantity = { qty -> onUpdateQuantity(index, qty) },
                        onRemove = { onRemoveProduct(index) },
                    )
                }

                // Bottom spacing
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun ScannedProductCard(
    item: ScannedProduct,
    onToggleSelection: () -> Unit,
    onUpdateQuantity: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) AppColors.Surface else AppColors.SurfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isSelected) 2.dp else 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // X remove button – top right corner
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .padding(4.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Xóa",
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                // Product image with checkbox overlay
                Box(
                    modifier = Modifier.size(72.dp),
                ) {
                    // Image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.SurfaceVariant)
                            .clickable(onClick = onToggleSelection),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item.product.imagePath != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(item.product.imagePath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.product.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = AppColors.TextTertiary,
                            )
                        }
                    }

                    // Checkbox overlay – top left of image
                    Checkbox(
                        checked = item.isSelected,
                        onCheckedChange = { onToggleSelection() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-6).dp, y = (-6).dp)
                            .size(24.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = AppColors.Primary,
                            uncheckedColor = AppColors.TextTertiary,
                            checkmarkColor = Color.White,
                        ),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Product info + quantity controls
                Column(modifier = Modifier.weight(1f)) {
                    // Product name
                    Text(
                        item.product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = AppColors.TextPrimary,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Barcode line
                    if (item.product.barcode != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = AppColors.TextTertiary,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                item.product.barcode,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextTertiary,
                            )
                        }
                    }

                    // SKU line
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tag,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = AppColors.TextTertiary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            item.product.sku,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextTertiary,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Price + Stock row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                CurrencyFormatter.format(item.product.sellingPrice),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary,
                            )
                            if (item.product.trackInventory && item.currentStock != Double.MAX_VALUE) {
                                Text(
                                    "Tồn kho: ${item.currentStock.toLong()} ${item.product.unit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.currentStock < item.quantity) AppColors.Error else AppColors.TextTertiary,
                                )
                            }
                        }

                        // Quantity stepper
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AppColors.SurfaceVariant,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            ) {
                                Surface(
                                    modifier = Modifier.size(28.dp),
                                    shape = CircleShape,
                                    color = if (item.quantity > 1) AppColors.Primary.copy(alpha = 0.12f) else Color.Transparent,
                                    onClick = { if (item.quantity > 1) onUpdateQuantity(item.quantity - 1) },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Giảm",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (item.quantity > 1) AppColors.Primary else AppColors.TextTertiary,
                                        )
                                    }
                                }

                                Text(
                                    "${item.quantity}",
                                    modifier = Modifier.widthIn(min = 28.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = AppColors.TextPrimary,
                                )

                                Surface(
                                    modifier = Modifier.size(28.dp),
                                    shape = CircleShape,
                                    color = AppColors.Primary.copy(alpha = 0.12f),
                                    onClick = { onUpdateQuantity(item.quantity + 1) },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Tăng",
                                            modifier = Modifier.size(16.dp),
                                            tint = AppColors.Primary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Subtotal if quantity > 1
                    if (item.quantity > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Thành tiền: ${CurrencyFormatter.format(item.product.sellingPrice * item.quantity)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
