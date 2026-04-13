package com.minipos.ui.barcode

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.barcode.BarcodeGenerator
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.ui.components.*

// ═══════════════════════════════════════════════════════════════
// BARCODE SCREEN — New design matching HTML mock
// ═══════════════════════════════════════════════════════════════

@Composable
fun BarcodeScreen(
    onBack: () -> Unit,
    viewModel: BarcodeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show messages
    LaunchedEffect(state.message, state.error) {
        val msg = state.error ?: state.message
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // Preview dialog
    if (state.showPreview && state.previewBitmap != null) {
        BarcodeLabelPreviewDialog(
            bitmap = state.previewBitmap!!,
            isPrinting = state.isPrinting,
            onDismiss = { viewModel.dismissPreview() },
            onSharePdf = { viewModel.shareAsPdf(context) },
            onShareImage = { viewModel.shareAsImage(context) },
            onPrintBluetooth = { viewModel.showPrinterPicker(context) },
        )
    }

    // Printer picker dialog
    if (state.showPrinterPicker) {
        PrinterPickerDialog(
            devices = state.availablePrinters,
            onSelect = { viewModel.printViaBluetooth(context, it) },
            onDismiss = { viewModel.dismissPrinterPicker() },
        )
    }

    // Product picker dialog
    if (state.showProductPicker) {
        ProductPickerDialog(
            products = state.pickerProducts,
            searchQuery = state.pickerSearchQuery,
            onSearchChange = { viewModel.searchPicker(it) },
            onSelect = { viewModel.addProduct(it.itemId) },
            onDismiss = { viewModel.dismissProductPicker() },
        )
    }

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ── Top Bar ──
            MiniPosTopBar(
                title = stringResource(R.string.barcode_screen_title),
                onBack = onBack,
            )

            // ── Scrollable body ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                // ═══════════════════════════════════════
                // SECTION: Selected Products
                // ═══════════════════════════════════════
                SectionTitle(title = stringResource(R.string.barcode_selected_products))

                if (state.selectedProducts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.selectedProducts.forEach { item ->
                            SelectedProductTag(
                                name = item.displayName,
                                onRemove = { viewModel.removeProduct(item.itemId) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Add more products button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
                        .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                        .clickable { viewModel.showProductPicker() }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.AddCircle,
                        contentDescription = null,
                        tint = AppColors.TextTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.barcode_add_products),
                        color = AppColors.TextTertiary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = AppColors.TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ═══════════════════════════════════════
                // SECTION: Barcode Type
                // ═══════════════════════════════════════
                SectionTitle(title = stringResource(R.string.barcode_type_section))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Single row: EAN-13 and QR Code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BarcodeTypeCard(
                            type = BarcodeType.EAN_13,
                            selected = state.barcodeType == BarcodeType.EAN_13,
                            onClick = { viewModel.setBarcodeType(BarcodeType.EAN_13) },
                            modifier = Modifier.weight(1f),
                        )
                        BarcodeTypeCard(
                            type = BarcodeType.QR_CODE,
                            selected = state.barcodeType == BarcodeType.QR_CODE,
                            onClick = { viewModel.setBarcodeType(BarcodeType.QR_CODE) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ═══════════════════════════════════════
                // SECTION: Label Options
                // ═══════════════════════════════════════
                SectionTitle(title = stringResource(R.string.barcode_label_options))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Show product name toggle
                    LabelOptionToggle(
                        name = stringResource(R.string.barcode_show_name),
                        description = stringResource(R.string.barcode_show_name_desc),
                        checked = state.showProductName,
                        onToggle = { viewModel.toggleShowProductName() },
                    )

                    // Show price toggle
                    LabelOptionToggle(
                        name = stringResource(R.string.barcode_show_price),
                        description = stringResource(R.string.barcode_show_price_desc),
                        checked = state.showPrice,
                        onToggle = { viewModel.toggleShowPrice() },
                    )

                    // Labels per product counter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.barcode_labels_per_product),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            QuantityButton(
                                icon = Icons.Rounded.Remove,
                                onClick = { viewModel.changeLabelsPerProduct(-1) },
                            )
                            Text(
                                state.labelsPerProduct.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = AppColors.TextPrimary,
                                modifier = Modifier.widthIn(min = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            QuantityButton(
                                icon = Icons.Rounded.Add,
                                onClick = { viewModel.changeLabelsPerProduct(1) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ═══════════════════════════════════════
                // SECTION: Preview
                // ═══════════════════════════════════════
                SectionTitle(title = stringResource(R.string.barcode_preview_section))

                BarcodePreviewCard(
                    product = state.previewProduct,
                    showName = state.showProductName,
                    showPrice = state.showPrice,
                    barcodeType = state.barcodeType,
                )

                Spacer(Modifier.height(16.dp))

                // ═══════════════════════════════════════
                // ACTION BUTTONS
                // ═══════════════════════════════════════
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Save button (secondary)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .background(
                                AppColors.InputBackground,
                                RoundedCornerShape(MiniPosTokens.Radius2xl)
                            )
                            .border(
                                1.dp,
                                AppColors.Border,
                                RoundedCornerShape(MiniPosTokens.Radius2xl)
                            )
                            .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
                            .clickable(enabled = state.selectedCount > 0) {
                                viewModel.saveBarcodesAsImage(context)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Save,
                                contentDescription = null,
                                tint = AppColors.TextPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.barcode_action_save),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppColors.TextPrimary,
                            )
                        }
                    }

                    // Print button (primary gradient)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
                            .background(MiniPosGradients.primary())
                            .clickable(enabled = state.selectedCount > 0) {
                                viewModel.printLabels(context)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Print,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.barcode_action_print),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// COMPONENTS
// ═══════════════════════════════════════════════════════════════

/** Product tag chip with remove button */
@Composable
private fun SelectedProductTag(
    name: String,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(
                AppColors.Primary.copy(alpha = 0.1f),
                RoundedCornerShape(MiniPosTokens.RadiusFull)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.PrimaryLight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Rounded.Close,
            contentDescription = stringResource(R.string.barcode_removed),
            tint = AppColors.PrimaryLight,
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
        )
    }
}

/** Barcode type card (2x2 grid item) */
@Composable
private fun BarcodeTypeCard(
    type: BarcodeType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        if (selected) AppColors.Primary else AppColors.Border,
        label = "bcTypeBorder"
    )
    val bgColor by animateColorAsState(
        if (selected) AppColors.Primary.copy(alpha = 0.08f) else AppColors.Surface,
        label = "bcTypeBg"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            type.label,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) AppColors.PrimaryLight else AppColors.TextPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(type.descriptionRes),
            fontSize = 10.sp,
            color = AppColors.TextTertiary,
        )
    }
}

/** Label option toggle row */
@Composable
private fun LabelOptionToggle(
    name: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
            Text(
                description,
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = AppColors.InputBackground,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** Quantity +/- button */
@Composable
private fun QuantityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(AppColors.InputBackground, CircleShape)
            .border(1.dp, AppColors.Border, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Barcode preview card showing visual barcode or QR code */
@Composable
private fun BarcodePreviewCard(
    product: BarcodeProductItem?,
    showName: Boolean,
    showPrice: Boolean,
    barcodeType: BarcodeType = BarcodeType.EAN_13,
) {
    val isQr = barcodeType == BarcodeType.QR_CODE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (product == null) {
            // Empty state
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(AppColors.InputBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.QrCode,
                    contentDescription = null,
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.barcode_no_preview),
                fontSize = 13.sp,
                color = AppColors.TextTertiary,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            val barcode = product.currentBarcode

            if (isQr) {
                // QR Code preview
                val qrBitmap = remember(barcode) {
                    barcode?.let {
                        try {
                            BarcodeGenerator.generateQrCodeBitmap(it, width = 200, height = 200)
                        } catch (_: Exception) { null }
                    }
                }

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.FillBounds,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(AppColors.InputBackground, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.QrCode,
                            contentDescription = null,
                            tint = AppColors.TextTertiary,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
            } else {
                // EAN-13 barcode preview
                val barcodePreview = remember(barcode) {
                    barcode?.let {
                        try {
                            BarcodeGenerator.generateBarcodeBitmap(it, width = 400, height = 120, showText = false)
                        } catch (_: Exception) { null }
                    }
                }

                if (barcodePreview != null) {
                    Image(
                        bitmap = barcodePreview.asImageBitmap(),
                        contentDescription = "Barcode",
                        modifier = Modifier
                            .width(200.dp)
                            .height(60.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.FillBounds,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(60.dp)
                            .background(AppColors.InputBackground, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.QrCode,
                            contentDescription = null,
                            tint = AppColors.TextTertiary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Barcode number
            if (barcode != null) {
                val formatted = if (!isQr && barcode.length == 13) {
                    "${barcode[0]} ${barcode.substring(1, 7)} ${barcode.substring(7, 13)}"
                } else barcode
                Text(
                    formatted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = if (isQr) 0.sp else 2.sp,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Product name
            if (showName) {
                Spacer(Modifier.height(4.dp))
                Text(
                    product.displayName,
                    fontSize = 12.sp,
                    color = AppColors.TextTertiary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Price
            if (showPrice) {
                Spacer(Modifier.height(4.dp))
                Text(
                    CurrencyFormatter.format(product.displayPrice),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = AppColors.Accent,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PRODUCT PICKER DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProductPickerDialog(
    products: List<BarcodeProductItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelect: (BarcodeProductItem) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.7f)
                .background(AppColors.Background, RoundedCornerShape(MiniPosTokens.RadiusXl))
                .clip(RoundedCornerShape(MiniPosTokens.RadiusXl)),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.barcode_pick_product_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = AppColors.TextSecondary,
                    )
                }
            }

            // Search
            MiniPosSearchBar(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = stringResource(R.string.barcode_pick_search),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Spacer(Modifier.height(8.dp))

            // Products list
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Inventory2,
                            contentDescription = null,
                            tint = AppColors.TextTertiary,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.barcode_pick_empty),
                            fontSize = 14.sp,
                            color = AppColors.TextTertiary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(products, key = { it.itemId }) { item ->
                        PickerProductItem(
                            item = item,
                            onSelect = { onSelect(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerProductItem(
    item: BarcodeProductItem,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .background(AppColors.Surface)
            .clickable(onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AppColors.PrimaryContainer, RoundedCornerShape(MiniPosTokens.RadiusSm)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.QrCode,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "SKU: ${item.displaySku}",
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
        }

        // Add icon
        Icon(
            Icons.Rounded.AddCircleOutline,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// BARCODE LABEL PREVIEW DIALOG (kept from old design)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun BarcodeLabelPreviewDialog(
    bitmap: android.graphics.Bitmap,
    isPrinting: Boolean,
    onDismiss: () -> Unit,
    onSharePdf: () -> Unit,
    onShareImage: () -> Unit,
    onPrintBluetooth: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Scaffold(
            containerColor = AppColors.Background,
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text(stringResource(R.string.barcode_preview)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background),
                )
            },
            bottomBar = {
                Surface(shadowElevation = 8.dp, color = AppColors.Surface) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onSharePdf,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.share_pdf_label))
                            }
                            OutlinedButton(
                                onClick = onShareImage,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.share_image_label))
                            }
                        }
                        MiniPosGradientButton(
                            text = if (isPrinting) stringResource(R.string.printing) else stringResource(R.string.print_via_bluetooth),
                            onClick = onPrintBluetooth,
                            enabled = !isPrinting,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Print,
                        )
                    }
                }
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(AppColors.Background),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Barcode labels preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(8.dp),
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PRINTER PICKER DIALOG (kept from old design)
// ═══════════════════════════════════════════════════════════════

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinterPickerDialog(
    devices: List<android.bluetooth.BluetoothDevice>,
    onSelect: (android.bluetooth.BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    MiniPosBottomSheet(
        visible = true,
        title = stringResource(R.string.select_printer_title),
        onDismiss = onDismiss,
    ) {
        if (devices.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AppColors.InputBackground, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.BluetoothDisabled,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = AppColors.TextTertiary,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.no_printer_found),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextSecondary,
                )
            }
        } else {
            // Device list
            devices.forEach { device ->
                val unknownName = stringResource(R.string.unknown_name)
                val deviceName = try { device.name ?: unknownName } catch (_: Exception) { unknownName }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                        .clickable { onSelect(device) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(AppColors.PrimaryLight.copy(alpha = 0.1f), RoundedCornerShape(MiniPosTokens.RadiusMd)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Print,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    // Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            deviceName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                        )
                        Text(
                            device.address,
                            fontSize = 11.sp,
                            color = AppColors.TextTertiary,
                        )
                    }
                    // Arrow
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = AppColors.TextTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                HorizontalDivider(
                    color = AppColors.Divider,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
