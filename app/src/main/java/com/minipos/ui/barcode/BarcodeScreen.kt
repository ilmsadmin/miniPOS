package com.minipos.ui.barcode

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.barcode.BarcodeGenerator
import com.minipos.R
import com.minipos.core.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.barcode_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (state.selectedCount > 0) {
                        TextButton(onClick = { viewModel.deselectAll() }) {
                            Text(stringResource(R.string.deselect_all), color = AppColors.TextSecondary)
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.selectedCount > 0) {
                BottomActionBar(
                    selectedCount = state.selectedCount,
                    isGenerating = state.isGenerating,
                    hasAllBarcodes = state.products.filter { it.isSelected }.all { it.hasBarcode || it.generatedBarcode != null },
                    onGenerate = { viewModel.generateBarcodes() },
                    onPreview = { viewModel.showBarcodePreview() },
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.search(it) },
                placeholder = { Text(stringResource(R.string.search_product_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            // Filter chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(BarcodeFilterMode.entries) { mode ->
                    FilterChip(
                        selected = state.filterMode == mode,
                        onClick = { viewModel.setFilterMode(mode) },
                        label = {
                            val count = when (mode) {
                                BarcodeFilterMode.ALL -> state.products.size
                                BarcodeFilterMode.NO_BARCODE -> state.products.count { !it.hasBarcode && it.generatedBarcode == null }
                                BarcodeFilterMode.HAS_BARCODE -> state.products.count { it.hasBarcode || it.generatedBarcode != null }
                            }
                            Text("${mode.label} ($count)")
                        },
                    )
                }
            }

            // Select all row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectAllFiltered() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.filteredProducts.isNotEmpty() && state.filteredProducts.all { it.isSelected },
                    onCheckedChange = { viewModel.selectAllFiltered() },
                )
                Text(
                    stringResource(R.string.select_all_items_format, state.filteredProducts.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                )
            }

            HorizontalDivider(color = AppColors.Surface, thickness = 1.dp)

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColors.TextTertiary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.no_products_found), style = MaterialTheme.typography.titleMedium, color = AppColors.TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.filteredProducts, key = { it.itemId }) { item ->
                        BarcodeProductCard(
                            item = item,
                            onToggle = { viewModel.toggleProduct(item.itemId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarcodeProductCard(
    item: BarcodeProductItem,
    onToggle: () -> Unit,
) {
    val barcode = item.currentBarcode
    val barcodePreview = remember(barcode) {
        barcode?.let {
            try { BarcodeGenerator.generateBarcodeBitmap(it, width = 200, height = 60, showText = true) }
            catch (_: Exception) { null }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isSelected) 3.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) AppColors.PrimaryContainer else AppColors.Surface
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
            )

            Column(modifier = Modifier.weight(1f)) {
                if (item.variant != null) {
                    // Show parent product name in smaller text
                    Text(
                        item.product.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Show variant name as main title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "↳ ${item.variant.variantName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AppColors.Accent.copy(alpha = 0.15f),
                        ) {
                            Text(
                                stringResource(R.string.variant_label_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.Accent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                } else {
                    Text(
                        item.product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "SKU: ${item.displaySku}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
                if (barcode != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AppColors.Secondary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            barcode,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Secondary,
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.no_barcode_yet),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Warning,
                    )
                }
            }

            // Barcode mini preview
            if (barcodePreview != null) {
                Image(
                    bitmap = barcodePreview.asImageBitmap(),
                    contentDescription = "Barcode",
                    modifier = Modifier
                        .width(80.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    selectedCount: Int,
    isGenerating: Boolean,
    hasAllBarcodes: Boolean,
    onGenerate: () -> Unit,
    onPreview: () -> Unit,
) {
    Surface(
        shadowElevation = 8.dp,
        color = AppColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.selected_count, selectedCount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.Primary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!hasAllBarcodes) {
                    Button(
                        onClick = onGenerate,
                        enabled = !isGenerating,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.generate_barcodes), maxLines = 1)
                    }
                }

                Button(
                    onClick = onPreview,
                    enabled = !isGenerating && hasAllBarcodes,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.barcode_preview), maxLines = 1)
                }
            }
        }
    }
}

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
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text(stringResource(R.string.barcode_preview)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    },
                )
            },
            bottomBar = {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.share_pdf_label))
                            }
                            OutlinedButton(
                                onClick = onShareImage,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.share_image_label))
                            }
                        }
                        Button(
                            onClick = onPrintBluetooth,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isPrinting,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            if (isPrinting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.printing))
                            } else {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.print_via_bluetooth))
                            }
                        }
                    }
                }
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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

@SuppressLint("MissingPermission")
@Composable
private fun PrinterPickerDialog(
    devices: List<android.bluetooth.BluetoothDevice>,
    onSelect: (android.bluetooth.BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_printer_title)) },
        text = {
            if (devices.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(48.dp), tint = AppColors.TextTertiary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.no_printer_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                    )
                }
            } else {
                LazyColumn {
                    items(devices) { device ->
                        val unknownName = stringResource(R.string.unknown_name)
                        val deviceName = try { device.name ?: unknownName } catch (_: Exception) { unknownName }
                        ListItem(
                            headlineContent = { Text(deviceName) },
                            supportingContent = { Text(device.address, style = MaterialTheme.typography.bodySmall) },
                            leadingContent = { Icon(Icons.Default.Print, contentDescription = null, tint = AppColors.Primary) },
                            modifier = Modifier.clickable { onSelect(device) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}
