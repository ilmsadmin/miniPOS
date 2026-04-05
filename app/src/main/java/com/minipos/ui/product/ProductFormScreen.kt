package com.minipos.ui.product

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minipos.R
import com.minipos.core.barcode.BarcodeGenerator
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.Category
import com.minipos.domain.model.ProductVariant
import com.minipos.ui.components.*
import com.minipos.ui.scanner.BarcodeScannerScreen
import com.minipos.core.utils.ImageHelper
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

// ═══════════════════════════════════════════════════════════════
// PRODUCT FORM SCREEN — Full-screen form matching HTML mock
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: String? = null,
    onBack: () -> Unit,
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val variantFormState by viewModel.variantFormState.collectAsState()

    // Initialize form on first composition
    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProductForEdit(productId)
        } else {
            viewModel.showCreateForm()
        }
    }

    val isEditing = productId != null

    // Toast state
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showToast by remember { mutableStateOf(false) }

    // Listen for save success — navigate back
    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            onBack()
        }
    }

    // Barcode scanner overlay
    if (state.showBarcodeScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { value, _ -> viewModel.onBarcodeScanned(value) },
            onClose = { viewModel.dismissBarcodeScanner() },
            title = stringResource(R.string.pf_scan_barcode),
        )
        return
    }

    // Category bottom sheet state
    var showCategorySheet by remember { mutableStateOf(false) }

    // Delete confirm dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && isEditing) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_product_title)) },
            text = { Text(stringResource(R.string.delete_product_confirm, formState.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val product = state.editingProduct
                        if (product != null) {
                            viewModel.deleteProduct(product)
                            onBack()
                        }
                    },
                ) {
                    Text(stringResource(R.string.delete_label), color = AppColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel_btn_label))
                }
            },
        )
    }

    // Category sheet
    if (showCategorySheet) {
        CategoryPickerSheet(
            categories = state.categories,
            selectedCategoryId = formState.categoryId,
            onSelect = { categoryId ->
                viewModel.updateFormField { copy(categoryId = categoryId) }
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false },
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

    Scaffold(
        containerColor = AppColors.Background,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Top Bar ──
                ProductFormTopBar(
                    isEditing = isEditing,
                    isSaving = formState.isSaving,
                    onBack = onBack,
                    onSave = {
                        viewModel.saveProduct()
                    },
                    onSaveSuccess = onBack,
                )

                // ── Page Body (scrollable) ──
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 32.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    // ── Photo Upload Area ──
                    item {
                        PhotoUploadArea(
                            imagePath = formState.imagePath,
                            productId = productId ?: "",
                            onImageChanged = { path ->
                                viewModel.updateFormField { copy(imagePath = path) }
                            },
                        )
                    }

                    // ══════════════════════════════════
                    // SECTION: Basic Information
                    // ══════════════════════════════════
                    item {
                        FormSectionLabel(stringResource(R.string.pf_section_basic))
                    }

                    // Product Name
                    item {
                        FormField(
                            label = stringResource(R.string.pf_product_name),
                            required = true,
                        ) {
                            FormInput(
                                value = formState.name,
                                onValueChange = { viewModel.updateFormField { copy(name = it) } },
                                placeholder = stringResource(R.string.pf_product_name_hint),
                            )
                        }
                    }

                    // SKU + Barcode Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // SKU
                            Box(modifier = Modifier.weight(1f)) {
                                FormField(
                                    label = stringResource(R.string.pf_sku),
                                ) {
                                    FormInput(
                                        value = formState.sku,
                                        onValueChange = { viewModel.updateFormField { copy(sku = it) } },
                                        placeholder = stringResource(R.string.pf_sku_hint),
                                    )
                                }
                            }
                            // Barcode with scan + generate buttons
                            Box(modifier = Modifier.weight(1f)) {
                                FormField(
                                    label = stringResource(R.string.pf_barcode),
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            FormInput(
                                                value = formState.barcode,
                                                onValueChange = { viewModel.updateFormField { copy(barcode = it) } },
                                                placeholder = stringResource(R.string.pf_barcode_hint),
                                            )
                                        }
                                        // Generate barcode button
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                                                .border(
                                                    1.dp,
                                                    AppColors.Border,
                                                    RoundedCornerShape(MiniPosTokens.RadiusLg),
                                                )
                                                .background(AppColors.Surface)
                                                .clickable { viewModel.generateBarcode() },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = stringResource(R.string.pf_generate_barcode),
                                                tint = AppColors.Accent,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        // Scan icon button
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                                                .border(
                                                    1.dp,
                                                    AppColors.Border,
                                                    RoundedCornerShape(MiniPosTokens.RadiusLg),
                                                )
                                                .background(AppColors.Surface)
                                                .clickable { viewModel.showBarcodeScanner() },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Default.QrCodeScanner,
                                                contentDescription = stringResource(R.string.pf_scan_barcode),
                                                tint = AppColors.TextSecondary,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Barcode preview (if EAN-13)
                    if (formState.barcode.length == 13) {
                        item {
                            val barcodeBitmap = remember(formState.barcode) {
                                try {
                                    BarcodeGenerator.generateBarcodeBitmap(
                                        formState.barcode,
                                        width = 300,
                                        height = 100,
                                        showText = true,
                                    )
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            if (barcodeBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
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

                    // Category + Unit Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Category (read-only tap to open sheet)
                            Box(modifier = Modifier.weight(1f)) {
                                val categoryName = remember(formState.categoryId, state.categories) {
                                    state.categories.find { it.id == formState.categoryId }?.name
                                }
                                FormField(
                                    label = stringResource(R.string.pf_category),
                                    required = true,
                                ) {
                                    FormInput(
                                        value = categoryName ?: "",
                                        onValueChange = {},
                                        placeholder = stringResource(R.string.pf_category_hint),
                                        readOnly = true,
                                        onClick = { showCategorySheet = true },
                                    )
                                }
                            }
                            // Unit
                            Box(modifier = Modifier.weight(1f)) {
                                FormField(
                                    label = stringResource(R.string.pf_unit),
                                ) {
                                    FormInput(
                                        value = formState.unit,
                                        onValueChange = { viewModel.updateFormField { copy(unit = it) } },
                                        placeholder = stringResource(R.string.pf_unit_hint),
                                    )
                                }
                            }
                        }
                    }

                    // ══════════════════════════════════
                    // SECTION: Pricing
                    // ══════════════════════════════════
                    item {
                        FormSectionLabel(stringResource(R.string.pf_section_pricing))
                    }

                    // Selling Price + Cost Price Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                FormField(
                                    label = stringResource(R.string.pf_selling_price),
                                    required = true,
                                ) {
                                    FormInput(
                                        value = formState.sellingPrice,
                                        onValueChange = {
                                            viewModel.updateFormField { copy(sellingPrice = it.filter { c -> c.isDigit() }) }
                                        },
                                        placeholder = "0",
                                        keyboardType = KeyboardType.Number,
                                        textStyle = TextStyle(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AppColors.Accent,
                                            fontSize = 14.sp,
                                        ),
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FormField(
                                    label = stringResource(R.string.pf_cost_price),
                                ) {
                                    FormInput(
                                        value = formState.costPrice,
                                        onValueChange = {
                                            viewModel.updateFormField { copy(costPrice = it.filter { c -> c.isDigit() }) }
                                        },
                                        placeholder = "0",
                                        keyboardType = KeyboardType.Number,
                                    )
                                }
                            }
                        }
                    }

                    // Tax Rate
                    item {
                        FormField(
                            label = stringResource(R.string.pf_tax),
                        ) {
                            FormInput(
                                value = formState.taxRate,
                                onValueChange = {
                                    viewModel.updateFormField { copy(taxRate = it.filter { c -> c.isDigit() || c == '.' }) }
                                },
                                placeholder = "0",
                                keyboardType = KeyboardType.Number,
                            )
                        }
                        FormHint(stringResource(R.string.pf_tax_hint))
                    }

                    // ══════════════════════════════════
                    // SECTION: Stock
                    // ══════════════════════════════════
                    item {
                        FormSectionLabel(stringResource(R.string.pf_section_stock))
                    }

                    // Min Stock (Low Alert) Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                FormField(
                                    label = stringResource(R.string.pf_low_stock_alert),
                                ) {
                                    FormInput(
                                        value = formState.minStock,
                                        onValueChange = {
                                            viewModel.updateFormField { copy(minStock = it.filter { c -> c.isDigit() }) }
                                        },
                                        placeholder = "0",
                                        keyboardType = KeyboardType.Number,
                                    )
                                }
                                FormHint(stringResource(R.string.pf_low_stock_hint))
                            }
                        }
                    }

                    // ══════════════════════════════════
                    // SECTION: Description
                    // ══════════════════════════════════
                    item {
                        FormSectionLabel(stringResource(R.string.pf_section_description))
                    }

                    item {
                        FormTextArea(
                            value = formState.description,
                            onValueChange = { viewModel.updateFormField { copy(description = it) } },
                            placeholder = stringResource(R.string.pf_description_hint),
                        )
                    }

                    // ══════════════════════════════════
                    // Track Inventory Toggle
                    // ══════════════════════════════════
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        ActiveToggleRow(
                            label = stringResource(R.string.pf_active_label),
                            description = stringResource(R.string.pf_active_desc),
                            isActive = formState.trackInventory,
                            onToggle = {
                                viewModel.updateFormField { copy(trackInventory = !trackInventory) }
                            },
                        )
                    }

                    // ══════════════════════════════════
                    // Has Variants Toggle
                    // ══════════════════════════════════
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        ActiveToggleRow(
                            label = stringResource(R.string.pf_has_variants),
                            description = stringResource(R.string.pf_has_variants_desc),
                            isActive = formState.hasVariants,
                            onToggle = {
                                viewModel.updateFormField { copy(hasVariants = !hasVariants) }
                            },
                        )
                    }

                    // ══════════════════════════════════
                    // SECTION: Variants (if enabled)
                    // ══════════════════════════════════
                    if (formState.hasVariants) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FormSectionLabel(stringResource(R.string.pf_section_variants))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                                        .background(MiniPosGradients.primary())
                                        .clickable { viewModel.showCreateVariantForm() }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = stringResource(R.string.pf_add_variant),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }

                        if (state.variants.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.pf_no_variants_hint),
                                    fontSize = 12.sp,
                                    color = AppColors.TextTertiary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        } else {
                            items(state.variants) { variant ->
                                VariantCard(
                                    variant = variant,
                                    onEdit = { viewModel.showEditVariantForm(variant) },
                                    onDelete = { viewModel.deleteVariant(variant) },
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // ══════════════════════════════════
                    // Delete Button (only in edit mode)
                    // ══════════════════════════════════
                    if (isEditing) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            DeleteProductButton(
                                onClick = { showDeleteDialog = true },
                            )
                        }
                    }

                    // ── Error message ──
                    if (formState.error != null) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = formState.error ?: "",
                                color = AppColors.Error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // Toast overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                MiniPosToast(
                    message = toastMessage ?: "",
                    visible = showToast,
                    onDismiss = { showToast = false },
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TOP BAR — matches HTML .topbar
// Back button + title + gradient "Save" pill
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProductFormTopBar(
    isEditing: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSaveSuccess: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back button (44dp circle, transparent bg)
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back_cd),
                tint = AppColors.TextSecondary,
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Title
        Text(
            text = if (isEditing) {
                stringResource(R.string.pf_edit_title)
            } else {
                stringResource(R.string.pf_add_title)
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )

        // Save pill button (gradient primary, rounded full)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                .background(MiniPosGradients.primary())
                .clickable(enabled = !isSaving, onClick = onSave)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.pf_save),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PHOTO UPLOAD AREA — matches HTML .photo-upload
// Dashed border + camera icon + hint text
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PhotoUploadArea(
    imagePath: String?,
    productId: String,
    onImageChanged: (String?) -> Unit,
) {
    val context = LocalContext.current
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // Camera capture
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraFile != null) {
            val uri = android.net.Uri.fromFile(tempCameraFile!!)
            val savedPath = ImageHelper.processAndSaveImage(context, uri, productId, 0)
            if (savedPath != null) {
                onImageChanged(savedPath)
            }
            tempCameraFile?.delete()
        }
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val savedPath = ImageHelper.processAndSaveImage(context, it, productId, 0)
            if (savedPath != null) {
                onImageChanged(savedPath)
            }
        }
    }

    fun launchCamera() {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val file = ImageHelper.createTempImageFile(context)
        tempCameraFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraLauncher.launch(uri)
    }

    fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    // Source selection dialog (Camera / Gallery)
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            icon = { Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = AppColors.Primary) },
            title = { Text(stringResource(R.string.pick_image_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceDialog = false
                                launchCamera()
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.SurfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(stringResource(R.string.take_photo), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.take_photo_desc), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceDialog = false
                                launchGallery()
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.SurfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = AppColors.Secondary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(stringResource(R.string.pick_from_gallery), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.pick_from_gallery_desc), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    if (imagePath != null) {
        // ── Show selected image with remove & change buttons ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                .border(2.dp, AppColors.BorderLight, RoundedCornerShape(MiniPosTokens.RadiusLg))
                .clickable { showImageSourceDialog = true },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(imagePath))
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.pf_select_photo),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusLg)),
                contentScale = ContentScale.Crop,
            )

            // Remove button (top-right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AppColors.Error.copy(alpha = 0.85f))
                    .clickable {
                        ImageHelper.deleteImage(imagePath)
                        onImageChanged(null)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_delete),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    } else {
        // ── Dashed border placeholder (matching screenshot) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                .border(
                    width = 2.dp,
                    color = AppColors.BorderLight,
                    shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
                )
                .background(AppColors.InputBackground)
                .clickable { showImageSourceDialog = true },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Rounded.AddAPhoto,
                    contentDescription = stringResource(R.string.pf_select_photo),
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.pf_photo_hint),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextTertiary,
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// FORM SECTION LABEL — matches HTML .sec-label
// Uppercase, 12sp, extra bold, tertiary color, 0.8sp tracking
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = AppColors.TextTertiary,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
    )
}

// ═══════════════════════════════════════════════════════════════
// FORM FIELD — matches HTML .field + .field-label
// Label row + content slot
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FormField(
    label: String,
    required: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        // Label row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary,
            )
            if (required) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "*",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Error,
                )
            }
        }
        // Input content
        content()
    }
}

// ═══════════════════════════════════════════════════════════════
// FORM INPUT — matches HTML .field-input
// 48dp height, 16dp radius, border, bg-input
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FormInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    textStyle: TextStyle? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val resolvedTextStyle = textStyle ?: TextStyle(
        fontSize = 14.sp,
        color = AppColors.TextPrimary,
    )

    val modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
        .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
        .background(AppColors.InputBackground)
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick)
            else Modifier
        )
        .padding(horizontal = 16.dp)

    if (readOnly || onClick != null) {
        // Read-only field (for category selector etc.)
        Box(
            modifier = modifier,
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = AppColors.TextTertiary,
                )
            } else {
                Text(
                    text = value,
                    style = resolvedTextStyle,
                )
            }
        }
    } else {
        // Editable field
        Box(
            modifier = modifier,
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = AppColors.TextTertiary,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = resolvedTextStyle,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(AppColors.Primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// FORM TEXTAREA — matches HTML .field-textarea
// min-height 80dp, 16dp radius, resizable vertically
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FormTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.InputBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontSize = 14.sp,
                color = AppColors.TextTertiary,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = AppColors.TextPrimary,
            ),
            cursorBrush = SolidColor(AppColors.Primary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// FORM HINT — matches HTML .field-hint
// 11sp, tertiary color, margin-top 4dp
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FormHint(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = AppColors.TextTertiary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

// ═══════════════════════════════════════════════════════════════
// ACTIVE TOGGLE ROW — matches HTML .toggle-row
// Card-like row with label + description + switch
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ActiveToggleRow(
    label: String = "",
    description: String = "",
    isActive: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.ifEmpty { stringResource(R.string.pf_active_label) },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
            Text(
                text = description.ifEmpty { stringResource(R.string.pf_active_desc) },
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Custom toggle matching HTML .toggle-sw
        Switch(
            checked = isActive,
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

// ═══════════════════════════════════════════════════════════════
// DELETE PRODUCT BUTTON — matches HTML .del-btn
// Full-width, 48dp, error border, error text + icon
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DeleteProductButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(1.dp, AppColors.Error, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = null,
                tint = AppColors.Error,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.pf_delete_product),
                color = AppColors.Error,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CATEGORY PICKER BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                    .background(AppColors.TextTertiary.copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.pf_select_category),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // "None" option
            CategoryPickerItem(
                name = stringResource(R.string.no_category),
                isSelected = selectedCategoryId == null,
                onClick = { onSelect(null) },
            )

            categories.forEach { category ->
                CategoryPickerItem(
                    name = category.name,
                    isSelected = category.id == selectedCategoryId,
                    onClick = { onSelect(category.id) },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CategoryPickerItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .then(
                if (isSelected) Modifier.background(AppColors.Primary.copy(alpha = 0.1f))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) AppColors.Primary else AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// VARIANT CARD — displays a variant with edit/delete actions
// ═══════════════════════════════════════════════════════════════

@Composable
private fun VariantCard(
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
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel_btn_label))
                }
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Variant icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                .background(AppColors.Accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Style,
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Variant info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = variant.variantName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "SKU: ${variant.sku}",
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                )
                variant.sellingPrice?.let { price ->
                    Text(
                        text = "·",
                        fontSize = 11.sp,
                        color = AppColors.TextTertiary,
                    )
                    Text(
                        text = CurrencyFormatter.format(price),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Accent,
                    )
                }
            }
        }

        // Delete button
        IconButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.delete_label),
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// VARIANT FORM DIALOG — create/edit variants
// ═══════════════════════════════════════════════════════════════

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
        title = {
            Text(
                if (isEditing) stringResource(R.string.edit_variant_title)
                else stringResource(R.string.add_variant_title),
                fontWeight = FontWeight.ExtraBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Variant Name
                FormField(
                    label = stringResource(R.string.variant_name_label),
                    required = true,
                ) {
                    FormInput(
                        value = formState.variantName,
                        onValueChange = { v -> onFieldChange { copy(variantName = v) } },
                        placeholder = stringResource(R.string.variant_name_label),
                    )
                }
                // SKU + Barcode
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        FormField(label = "SKU") {
                            FormInput(
                                value = formState.sku,
                                onValueChange = { v -> onFieldChange { copy(sku = v) } },
                                placeholder = "Auto",
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        FormField(label = stringResource(R.string.pf_barcode)) {
                            FormInput(
                                value = formState.barcode,
                                onValueChange = { v -> onFieldChange { copy(barcode = v) } },
                                placeholder = stringResource(R.string.pf_barcode_hint),
                            )
                        }
                    }
                }
                // Cost Price + Selling Price
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        FormField(label = stringResource(R.string.pf_cost_price)) {
                            FormInput(
                                value = formState.costPrice,
                                onValueChange = { v -> onFieldChange { copy(costPrice = v.filter { it.isDigit() }) } },
                                placeholder = "0",
                                keyboardType = KeyboardType.Number,
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        FormField(label = stringResource(R.string.pf_selling_price)) {
                            FormInput(
                                value = formState.sellingPrice,
                                onValueChange = { v -> onFieldChange { copy(sellingPrice = v.filter { it.isDigit() }) } },
                                placeholder = "0",
                                keyboardType = KeyboardType.Number,
                                textStyle = TextStyle(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.Accent,
                                    fontSize = 14.sp,
                                ),
                            )
                        }
                    }
                }
                // Attributes
                FormField(label = stringResource(R.string.variant_attributes_label)) {
                    FormInput(
                        value = formState.attributes,
                        onValueChange = { v -> onFieldChange { copy(attributes = v) } },
                        placeholder = stringResource(R.string.variant_attributes_hint),
                    )
                }
                // Error
                if (formState.error != null) {
                    Text(
                        text = formState.error ?: "",
                        color = AppColors.Error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                    .background(MiniPosGradients.primary())
                    .clickable(enabled = !formState.isSaving, onClick = onSave)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.pf_save),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_btn_label))
            }
        },
    )
}
