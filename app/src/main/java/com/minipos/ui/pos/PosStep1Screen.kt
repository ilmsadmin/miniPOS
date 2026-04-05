package com.minipos.ui.pos

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.CartItem
import com.minipos.domain.model.Customer
import com.minipos.domain.model.Discount
import com.minipos.domain.model.Product
import com.minipos.domain.model.ProductVariant
import com.minipos.ui.components.*
import com.minipos.ui.scanner.BarcodeScannerScreen
import com.minipos.ui.scanner.ImageViewerScreen
import java.io.File

// ═══════════════════════════════════════
// POS STEP 1 — ALL-IN-ONE SCREEN
// Products grid + Bottom Sheet cart
// Matches pos-home.html mock design
// ═══════════════════════════════════════

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

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showToast by remember { mutableStateOf(false) }

    // Bottom sheet state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCartSheet by remember { mutableStateOf(false) }

    if (state.showBarcodeScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { value, _ -> viewModel.onBarcodeScanned(value) },
            onClose = { viewModel.dismissBarcodeScanner() },
            title = stringResource(R.string.scan_product_barcode_title),
        )
        return
    }

    if (state.showVariantPicker && state.variantPickerProduct != null) {
        val pickerProduct = state.variantPickerProduct!!
        VariantPickerDialog(
            product = pickerProduct,
            variants = state.variantPickerVariants,
            onSelectVariant = { variant -> viewModel.addVariantToCart(pickerProduct, variant) },
            onDismiss = { viewModel.dismissVariantPicker() },
        )
    }

    LaunchedEffect(state.stockError) {
        state.stockError?.let { error ->
            snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Short)
            viewModel.clearStockError()
        }
    }

    // ── Cart Bottom Sheet ──
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = sheetState,
            containerColor = AppColors.Surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AppColors.BorderLight),
                )
            },
        ) {
            CartSheetContent(
                cart = cart,
                customers = state.recentCustomers,
                customerSearchResults = state.customerSearchResults,
                customerSearchQuery = state.customerSearchQuery,
                showCreateCustomerForm = state.showCreateCustomerForm,
                showOrderDiscount = state.showOrderDiscount,
                onSearchCustomer = { viewModel.searchCustomer(it) },
                onSelectCustomer = { viewModel.selectCustomer(it) },
                onToggleCreateCustomerForm = { viewModel.toggleCreateCustomerForm() },
                onQuickCreateCustomer = { name, phone -> viewModel.quickCreateCustomer(name, phone) },
                onUpdateQuantity = { index, qty -> viewModel.updateQuantity(index, qty) },
                onRemoveItem = { index -> viewModel.removeItem(index) },
                onClearCart = { viewModel.clearCart() },
                onShowOrderDiscount = { viewModel.showOrderDiscountDialog() },
                onDismissOrderDiscount = { viewModel.dismissOrderDiscountDialog() },
                onApplyOrderDiscount = { viewModel.setOrderDiscount(it) },
                onCheckout = {
                    showCartSheet = false
                    onNext()
                },
            )
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header: brand + search + scan ──
                PosHeader(
                    onBack = onBack,
                    searchQuery = state.searchQuery,
                    onSearch = { viewModel.search(it) },
                    onScanBarcode = { viewModel.showBarcodeScanner() },
                )

                // ── Category Chips ──
                LazyRow(
                    modifier = Modifier.padding(bottom = 10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        PosChip(
                            label = stringResource(R.string.filter_all_products),
                            selected = state.selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                        )
                    }
                    items(state.categories) { category ->
                        PosChip(
                            label = "${category.icon ?: ""} ${category.name}",
                            selected = state.selectedCategory?.id == category.id,
                            onClick = { viewModel.selectCategory(category) },
                        )
                    }
                }

                // ── Product Grid ──
                if (state.products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.SurfaceElevated),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = AppColors.TextTertiary,
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.no_products_pos),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextSecondary,
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 4.dp,
                            bottom = if (cart.isEmpty()) 16.dp else 88.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.products) { product ->
                            val qty = cart.items
                                .filter { it.product.id == product.id }
                                .sumOf { it.quantity }
                                .toInt()
                            val availableStock = viewModel.cartHolder.getAvailableStock(product.id)
                            ProductCard(
                                product = product,
                                quantity = qty,
                                availableStock = availableStock,
                                onClick = {
                                    viewModel.addToCart(product)
                                    toastMessage = product.name
                                    showToast = true
                                },
                            )
                        }
                    }
                }
            }

            // ── Toast ──
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp),
            ) {
                MiniPosToast(
                    message = "✓  ${toastMessage ?: ""}",
                    visible = showToast,
                    onDismiss = { showToast = false },
                )
            }

            // ── Cart Bar (animated slide-up) ──
            AnimatedVisibility(
                visible = !cart.isEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                ) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                CartBar(
                    itemCount = cart.totalQuantity.toInt(),
                    total = cart.subtotal,
                    onClick = { showCartSheet = true },
                )
            }
        }
    }
}

// ═══════════════════════════════════════
// CART BOTTOM SHEET CONTENT
// Replaces Step 2 (quantity/price) + Step 3 (customer)
// ═══════════════════════════════════════

@Composable
private fun CartSheetContent(
    cart: com.minipos.domain.model.Cart,
    customers: List<Customer>,
    customerSearchResults: List<Customer>,
    customerSearchQuery: String,
    showCreateCustomerForm: Boolean,
    showOrderDiscount: Boolean,
    onSearchCustomer: (String) -> Unit,
    onSelectCustomer: (Customer?) -> Unit,
    onToggleCreateCustomerForm: () -> Unit,
    onQuickCreateCustomer: (String, String?) -> Unit,
    onUpdateQuantity: (Int, Double) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearCart: () -> Unit,
    onShowOrderDiscount: () -> Unit,
    onDismissOrderDiscount: () -> Unit,
    onApplyOrderDiscount: (Discount?) -> Unit,
    onCheckout: () -> Unit,
) {
    var showCustomerPicker by remember { mutableStateOf(false) }

    if (showOrderDiscount) {
        OrderDiscountDialog(
            current = cart.orderDiscount,
            onDismiss = onDismissOrderDiscount,
            onApply = { discount ->
                onApplyOrderDiscount(discount)
                onDismissOrderDiscount()
            },
        )
    }

    if (showCustomerPicker) {
        CustomerPickerSheet(
            customers = customers,
            searchResults = customerSearchResults,
            searchQuery = customerSearchQuery,
            showCreateForm = showCreateCustomerForm,
            selectedCustomer = cart.customer,
            onSearch = onSearchCustomer,
            onSelect = {
                onSelectCustomer(it)
                showCustomerPicker = false
            },
            onToggleCreateForm = onToggleCreateCustomerForm,
            onQuickCreate = { name, phone ->
                onQuickCreateCustomer(name, phone)
                showCustomerPicker = false
            },
            onDismiss = { showCustomerPicker = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.pos_cart_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "(${cart.totalQuantity.toInt()})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = AppColors.TextTertiary,
                )
            }
            TextButton(
                onClick = onClearCart,
                colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Error),
            ) {
                Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.pos_clear_cart), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ── Cart Items ──
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        ) {
            itemsIndexed(cart.items) { index, item ->
                CartSheetItem(
                    item = item,
                    onQuantityChange = { onUpdateQuantity(index, it) },
                    onRemove = { onRemoveItem(index) },
                )
                if (index < cart.items.lastIndex) {
                    HorizontalDivider(
                        color = AppColors.BorderLight,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }

        // ── Customer Row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Background)
                .clickable { showCustomerPicker = true }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AppColors.PrimaryLight, AppColors.Primary),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        cart.customer?.name ?: stringResource(R.string.pos_walk_in_customer),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        stringResource(R.string.pos_customer_label),
                        fontSize = 11.sp,
                        color = AppColors.TextTertiary,
                    )
                }
            }
            Text(
                stringResource(R.string.pos_change_customer),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.PrimaryLight,
            )
        }

        // ── Summary + Checkout ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, AppColors.Primary.copy(alpha = 0.04f)),
                    ),
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            SheetSummaryRow(stringResource(R.string.subtotal), CurrencyFormatter.format(cart.subtotal))
            if (cart.taxAmount > 0) {
                SheetSummaryRow(stringResource(R.string.tax), CurrencyFormatter.format(cart.taxAmount))
            }

            // Discount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.discount), fontSize = 13.sp, color = AppColors.TextSecondary)
                if (cart.orderDiscount != null) {
                    Text(
                        "-${CurrencyFormatter.format(cart.orderDiscountAmount)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Success,
                        modifier = Modifier.clickable { onShowOrderDiscount() },
                    )
                } else {
                    TextButton(
                        onClick = onShowOrderDiscount,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp),
                    ) {
                        Text(
                            stringResource(R.string.pos_add_discount),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.pos_grand_total),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = AppColors.TextPrimary,
                )
                Text(
                    CurrencyFormatter.format(cart.grandTotal),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = AppColors.TextPrimary,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Checkout button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
                    .background(
                        Brush.linearGradient(
                            listOf(AppColors.Primary, AppColors.Accent, AppColors.PrimaryLight),
                        ),
                    )
                    .clickable(enabled = !cart.isEmpty()) { onCheckout() },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Payments, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.pos_checkout_btn) + " — " + CurrencyFormatter.format(cart.grandTotal),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ═══════════════════════════════════════
// CART SHEET ITEM — Inline quantity
// ═══════════════════════════════════════

@Composable
private fun CartSheetItem(
    item: CartItem,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                .background(AppColors.SurfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            if (item.product.imagePath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(item.product.imagePath!!))
                        .crossfade(true)
                        .build(),
                    contentDescription = item.product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Rounded.Inventory2, null, tint = AppColors.TextTertiary, modifier = Modifier.size(26.dp))
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.variant != null) {
                Text(item.variant.variantName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColors.Accent)
            }
            Text(
                "${CurrencyFormatter.format(item.unitPrice)} × ${
                    if (item.quantity == item.quantity.toLong().toDouble()) item.quantity.toLong().toString()
                    else item.quantity.toString()
                }",
                fontSize = 12.sp,
                color = AppColors.TextTertiary,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                CurrencyFormatter.format(item.lineTotal),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .border(1.dp, AppColors.BorderLight, CircleShape)
                        .clickable {
                            if (item.quantity > 1) onQuantityChange(item.quantity - 1)
                            else onRemove()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (item.quantity <= 1) Icons.Rounded.Delete else Icons.Rounded.Remove,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = if (item.quantity <= 1) AppColors.Error else AppColors.TextPrimary,
                    )
                }
                Text(
                    if (item.quantity == item.quantity.toLong().toDouble()) item.quantity.toLong().toString()
                    else item.quantity.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.widthIn(min = 18.dp),
                    textAlign = TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .border(1.dp, AppColors.BorderLight, CircleShape)
                        .clickable { onQuantityChange(item.quantity + 1) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp), tint = AppColors.TextPrimary)
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// CUSTOMER PICKER (replaces Step 3)
// ═══════════════════════════════════════

@Composable
private fun CustomerPickerSheet(
    customers: List<Customer>,
    searchResults: List<Customer>,
    searchQuery: String,
    showCreateForm: Boolean,
    selectedCustomer: Customer?,
    onSearch: (String) -> Unit,
    onSelect: (Customer?) -> Unit,
    onToggleCreateForm: () -> Unit,
    onQuickCreate: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.7f)
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.Surface),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.step3_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, null, tint = AppColors.TextSecondary)
                }
            }

            // Walk-in option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(null) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (selectedCustomer == null) Icons.Rounded.CheckCircle else Icons.Rounded.Person,
                    null,
                    tint = if (selectedCustomer == null) AppColors.Primary else AppColors.TextTertiary,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.pos_walk_in_customer), fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.pos_skip_customer), fontSize = 12.sp, color = AppColors.TextSecondary)
                }
                if (selectedCustomer == null) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(AppColors.Primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            HorizontalDivider(color = AppColors.BorderLight, modifier = Modifier.padding(horizontal = 20.dp))

            if (showCreateForm) {
                QuickCreateCustomerCard(onCancel = onToggleCreateForm, onCreate = onQuickCreate)
            } else {
                MiniPosSearchBar(
                    value = searchQuery,
                    onValueChange = onSearch,
                    placeholder = stringResource(R.string.search_customer),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    trailingIcon = {
                        IconButton(onClick = onToggleCreateForm) {
                            Icon(Icons.Rounded.PersonAdd, null, tint = AppColors.Accent)
                        }
                    },
                )
            }

            val displayCustomers = if (searchQuery.isNotBlank()) searchResults else customers
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(displayCustomers) { customer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MiniPosTokens.RadiusSm))
                            .background(
                                if (selectedCustomer?.id == customer.id) AppColors.PrimaryContainer
                                else Color.Transparent,
                            )
                            .clickable { onSelect(customer) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (selectedCustomer?.id == customer.id) Icons.Rounded.CheckCircle
                            else Icons.Rounded.Person,
                            null,
                            tint = if (selectedCustomer?.id == customer.id) AppColors.Primary
                            else AppColors.TextTertiary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(customer.name, fontWeight = FontWeight.Medium)
                            if (!customer.phone.isNullOrBlank()) {
                                Text(customer.phone, fontSize = 12.sp, color = AppColors.TextSecondary)
                            }
                        }
                        Text(
                            stringResource(R.string.visit_count_format, customer.visitCount),
                            fontSize = 12.sp,
                            color = AppColors.TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCreateCustomerCard(
    onCancel: () -> Unit,
    onCreate: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.add_new_customer_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.customer_name_required)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.phone_number)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (name.isNotBlank()) onCreate(name, phone.ifBlank { null }) },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                ) { Text(stringResource(R.string.add_btn)) }
            }
        }
    }
}

@Composable
private fun SheetSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = AppColors.TextSecondary)
        Text(value, fontSize = 13.sp, color = AppColors.TextSecondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderDiscountDialog(
    current: Discount?,
    onDismiss: () -> Unit,
    onApply: (Discount?) -> Unit,
) {
    var type by remember { mutableStateOf(current?.type ?: "percent") }
    var value by remember { mutableStateOf(current?.value?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.order_discount_dialog_title)) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "percent", onClick = { type = "percent" }, label = { Text(stringResource(R.string.percent_label)) })
                    FilterChip(selected = type == "fixed", onClick = { type = "fixed" }, label = { Text(stringResource(R.string.fixed_amount_label)) })
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(if (type == "percent") stringResource(R.string.percent_field) else stringResource(R.string.amount_field)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    suffix = { Text(if (type == "percent") "%" else "đ") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = value.toDoubleOrNull()
                if (v != null && v > 0) onApply(Discount(type, v))
                else onApply(null)
            }) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = { onApply(null) }) { Text(stringResource(R.string.remove_discount)) }
        },
    )
}

// ─────────────────────────────────────
// Header
// ─────────────────────────────────────
@Composable
private fun PosHeader(
    onBack: () -> Unit,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onScanBarcode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .statusBarsPadding()
            .padding(bottom = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.SurfaceVariant)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AppColors.TextSecondary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusSm))
                        .background(Brush.linearGradient(listOf(AppColors.Primary, AppColors.Accent))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PointOfSale, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mini POS", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.TextPrimary)
            }
        }

        // Search + Scan row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(AppColors.InputBackground, RoundedCornerShape(MiniPosTokens.Radius2xl))
                    .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.Radius2xl))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = AppColors.TextTertiary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(stringResource(R.string.product_search_hint), color = AppColors.TextTertiary, fontSize = 15.sp)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearch,
                        textStyle = androidx.compose.ui.text.TextStyle(color = AppColors.TextPrimary, fontSize = 15.sp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Rounded.Close, "Clear",
                        tint = AppColors.TextTertiary,
                        modifier = Modifier.size(18.dp).clickable { onSearch("") },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AppColors.Primary, AppColors.Accent)))
                    .clickable(onClick = onScanBarcode),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = stringResource(R.string.scan_barcode_cd), tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ─────────────────────────────────────
// Category Chip
// ─────────────────────────────────────
@Composable
private fun PosChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(if (selected) AppColors.Primary else Color.Transparent, tween(150), label = "bg")
    val textColor by animateColorAsState(if (selected) Color.White else AppColors.TextSecondary, tween(150), label = "text")
    val borderColor by animateColorAsState(if (selected) AppColors.Primary else AppColors.BorderLight, tween(150), label = "border")

    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(MiniPosTokens.RadiusFull))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────
// Product Card
// ─────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductCard(
    product: Product,
    quantity: Int,
    availableStock: Double?,
    onClick: () -> Unit,
) {
    val isOutOfStock = product.trackInventory && (availableStock ?: 0.0) <= 0.0
    val isLowStock = product.trackInventory && !isOutOfStock && (availableStock ?: 0.0) <= 5.0
    var showImageViewer by remember { mutableStateOf(false) }
    val allImages = remember(product) {
        buildList { product.imagePath?.let { add(it) }; addAll(product.additionalImages) }
    }

    if (showImageViewer && allImages.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            ImageViewerScreen(images = allImages, initialIndex = 0, onClose = { showImageViewer = false })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(
                width = if (quantity > 0) 1.5.dp else 1.dp,
                color = when {
                    isOutOfStock -> AppColors.Error.copy(alpha = 0.25f)
                    quantity > 0 -> AppColors.Primary.copy(alpha = 0.6f)
                    else -> AppColors.Border
                },
                shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
            )
            .combinedClickable(
                onClick = { if (!isOutOfStock) onClick() },
                onLongClick = { if (allImages.isNotEmpty()) showImageViewer = true },
            ),
    ) {
        Column {
            // ── Thumbnail ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(AppColors.SurfaceElevated),
                contentAlignment = Alignment.Center,
            ) {
                if (product.imagePath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(product.imagePath))
                            .crossfade(true)
                            .build(),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Rounded.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = when {
                            isOutOfStock -> AppColors.Error.copy(alpha = 0.35f)
                            quantity > 0 -> AppColors.Primary.copy(alpha = 0.6f)
                            else -> AppColors.TextTertiary
                        },
                    )
                }

                // Dim overlay for out-of-stock
                if (isOutOfStock) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                }

                // Quantity badge — top left
                if (quantity > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(5.dp)
                            .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                            .clip(CircleShape)
                            .background(AppColors.Primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$quantity", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }

                // Stock badge — top right
                if (product.trackInventory) {
                    when {
                        isOutOfStock -> Box(
                            modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).background(AppColors.Error.copy(alpha = 0.88f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
                        ) { Text("Hết", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                        isLowStock -> Box(
                            modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).background(AppColors.Warning.copy(alpha = 0.88f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
                        ) { Text("Còn ${availableStock?.toInt()}", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                        else -> {}
                    }
                }

                // Variants dot — bottom right
                if (product.hasVariants) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(16.dp).clip(CircleShape).background(AppColors.Accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Style, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
            }

            // ── Info ──
            Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp)) {
                Text(
                    text = product.name,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOutOfStock) AppColors.TextTertiary else AppColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp,
                    minLines = 2,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = CurrencyFormatter.format(product.sellingPrice),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOutOfStock) AppColors.TextTertiary else AppColors.Secondary,
                )
            }
        }
    }
}

// ─────────────────────────────────────
// Cart Bar
// ─────────────────────────────────────
@Composable
private fun CartBar(itemCount: Int, total: Double, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
                .background(Brush.linearGradient(listOf(AppColors.Primary, AppColors.Accent)))
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text("$itemCount sản phẩm", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(CurrencyFormatter.format(total), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = (-0.3).sp)
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────
// Variant Picker Dialog
// ─────────────────────────────────────
@Composable
private fun VariantPickerDialog(
    product: Product,
    variants: List<ProductVariant>,
    onSelectVariant: (ProductVariant) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
        containerColor = AppColors.Surface,
        title = {
            Column {
                Text(product.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Text(stringResource(R.string.select_variant_hint), fontSize = 12.sp, color = AppColors.TextSecondary)
            }
        },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(variants) { variant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MiniPosTokens.RadiusSm))
                            .background(AppColors.SurfaceElevated)
                            .clickable { onSelectVariant(variant) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Style, null, modifier = Modifier.size(24.dp), tint = AppColors.Accent)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(variant.variantName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            if (variant.attributes != "{}" && variant.attributes.isNotBlank()) {
                                Text(variant.attributes, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            CurrencyFormatter.format(variant.sellingPrice ?: product.sellingPrice),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Secondary,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_btn_label), color = AppColors.TextSecondary)
            }
        },
    )
}
