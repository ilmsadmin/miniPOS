package com.minipos.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.theme.AppLanguage
import com.minipos.core.theme.ThemeMode
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.CartItem
import com.minipos.domain.model.Customer
import com.minipos.domain.model.Discount
import com.minipos.domain.model.Product
import com.minipos.domain.model.ProductVariant
import com.minipos.domain.model.UserRole
import com.minipos.ui.components.*
import com.minipos.ui.navigation.Screen
import com.minipos.ui.pos.PosStep1ViewModel
import com.minipos.ui.scanner.BarcodeScannerScreen
import com.minipos.ui.scanner.ImageViewerScreen
import com.minipos.ui.settings.ChangePinBottomSheet
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit = {},
    onSwitchUser: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    posViewModel: PosStep1ViewModel = hiltViewModel(),
) {
    val homeState by homeViewModel.state.collectAsState()
    val posState by posViewModel.state.collectAsState()
    val cart by posViewModel.cartHolder.cart.collectAsState()
    val stockVersion by posViewModel.cartHolder.stockVersion.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showToast by remember { mutableStateOf(false) }

    // Bottom sheet cart state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCartSheet by remember { mutableStateOf(false) }

    // Profile sheet state
    var showProfileSheet by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    // Logout confirm dialog
    if (showLogoutConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            icon = { Icon(Icons.Rounded.Logout, contentDescription = null, tint = AppColors.Error) },
            title = { Text(stringResource(R.string.logout_title)) },
            text = { Text(stringResource(R.string.logout_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirm = false; onLogout() }) {
                    Text(stringResource(R.string.logout_btn), color = AppColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = AppColors.Surface,
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        homeViewModel.refreshCurrentUser()
        homeViewModel.refreshDashboard()
        posViewModel.refreshStock()
    }

    // Profile sheet
    if (showProfileSheet) {
        HomeProfileSheet(
            userName = homeState.userName,
            userRole = homeState.userRole,
            store = homeState.store,
            currentUserHasPin = homeState.currentUserHasPin,
            isDark = run {
                val mode by homeViewModel.themeManager.themeMode.collectAsState()
                val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
                when (mode) {
                    ThemeMode.SYSTEM -> isSystemDark
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
            },
            onToggleDarkMode = {
                val current = homeViewModel.themeManager.themeMode.value
                val isSystemDark = false // resolved above — flip current effective state
                val effectiveDark = when (current) {
                    ThemeMode.SYSTEM -> false // toggling from system = go to explicit
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                homeViewModel.themeManager.setThemeMode(
                    if (effectiveDark) ThemeMode.LIGHT else ThemeMode.DARK
                )
            },
            onChangePinClick = {
                showProfileSheet = false
                homeViewModel.showChangePinSheet()
            },
            onNavigateToSettings = {
                showProfileSheet = false
                onNavigate(Screen.Settings.route)
            },
            onSwitchUser = {
                showProfileSheet = false
                onSwitchUser()
            },
            onLogout = {
                showProfileSheet = false
                showLogoutConfirm = true
            },
            onDismiss = { showProfileSheet = false },
        )
    }

    // Change PIN sheet (triggered from Profile Sheet)
    if (homeState.showChangePinSheet) {
        ChangePinBottomSheet(
            hasExistingPin = homeState.currentUserHasPin,
            pinVerified = homeState.pinVerified,
            pinVerifyError = homeState.pinVerifyError,
            onVerifyPin = { homeViewModel.verifyCurrentPin(it) },
            onSaveNewPin = { homeViewModel.saveNewPin(it) },
            onDismiss = { homeViewModel.dismissChangePinSheet() },
        )
    }

    // Barcode scanner overlay
    if (posState.showBarcodeScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { value, _ -> posViewModel.onBarcodeScanned(value) },
            onClose = { posViewModel.dismissBarcodeScanner() },
            title = stringResource(R.string.scan_product_barcode_title),
        )
        return
    }

    // Variant picker dialog
    if (posState.showVariantPicker && posState.variantPickerProduct != null) {
        val pickerProduct = posState.variantPickerProduct!!
        val pickerStock = posViewModel.cartHolder.getAvailableStock(pickerProduct.id)
        HomeVariantPickerDialog(
            product = pickerProduct,
            variants = posState.variantPickerVariants,
            availableStock = pickerStock,
            variantStockMap = posState.variantStockMap,
            isSharedStock = posState.isSharedStock,
            onSelectVariant = { variant -> posViewModel.addVariantToCart(pickerProduct, variant) },
            onDismiss = { posViewModel.dismissVariantPicker() },
        )
    }

    LaunchedEffect(posState.stockError) {
        posState.stockError?.let { error ->
            snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Short)
            posViewModel.clearStockError()
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
            HomeCartSheetContent(
                cart = cart,
                customers = posState.recentCustomers,
                customerSearchResults = posState.customerSearchResults,
                customerSearchQuery = posState.customerSearchQuery,
                showCreateCustomerForm = posState.showCreateCustomerForm,
                showOrderDiscount = posState.showOrderDiscount,
                onSearchCustomer = { posViewModel.searchCustomer(it) },
                onSelectCustomer = { posViewModel.selectCustomer(it) },
                onToggleCreateCustomerForm = { posViewModel.toggleCreateCustomerForm() },
                onQuickCreateCustomer = { name, phone -> posViewModel.quickCreateCustomer(name, phone) },
                onUpdateQuantity = { index, qty -> posViewModel.updateQuantity(index, qty) },
                onRemoveItem = { index -> posViewModel.removeItem(index) },
                onClearCart = { posViewModel.clearCart() },
                onShowOrderDiscount = { posViewModel.showOrderDiscountDialog() },
                onDismissOrderDiscount = { posViewModel.dismissOrderDiscountDialog() },
                onApplyOrderDiscount = { posViewModel.setOrderDiscount(it) },
                onCheckout = {
                    showCartSheet = false
                    onNavigate(Screen.PosStep4.route)
                },
            )
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val cartEmptyHint = stringResource(R.string.toast_cart_empty_hint)
            HomeBottomNav(
                onNavigate = onNavigate,
                onPosClick = {
                    if (!cart.isEmpty()) {
                        showCartSheet = true
                    } else {
                        // Provide feedback so the user knows what to do
                        toastMessage = cartEmptyHint
                        showToast = true
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header ──
                val currentThemeMode by homeViewModel.themeManager.themeMode.collectAsState()
                val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
                val isDark = when (currentThemeMode) {
                    com.minipos.core.theme.ThemeMode.SYSTEM -> isSystemDark
                    com.minipos.core.theme.ThemeMode.LIGHT -> false
                    com.minipos.core.theme.ThemeMode.DARK -> true
                }
                HomeHeader(
                    storeName = homeState.storeName.ifEmpty { "Mini POS" },
                    userName = homeState.userName,
                    todayOrders = homeState.todayOrders,
                    isDarkMode = isDark,
                    searchQuery = posState.searchQuery,
                    onSearch = { posViewModel.search(it) },
                    onScanBarcode = { posViewModel.showBarcodeScanner() },
                    onAvatarClick = { showProfileSheet = true },
                    onOrdersClick = { onNavigate(Screen.OrderList.route) },
                    onDarkModeClick = {
                        homeViewModel.themeManager.setThemeMode(
                            if (isDark) com.minipos.core.theme.ThemeMode.LIGHT
                            else com.minipos.core.theme.ThemeMode.DARK,
                        )
                    },
                )

                // ── Category Chips ──
                LazyRow(
                    modifier = Modifier.padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        HomeChip(
                            label = stringResource(R.string.filter_all_products),
                            selected = posState.selectedCategory == null,
                            onClick = { posViewModel.selectCategory(null) },
                        )
                    }
                    items(posState.categories) { category ->
                        HomeChip(
                            label = category.name,
                            selected = posState.selectedCategory?.id == category.id,
                            onClick = { posViewModel.selectCategory(category) },
                            icon = categoryIconFromName(category.icon),
                        )
                    }
                }

                // ── Product Grid ──
                if (posState.allProducts.isEmpty() && homeState.setupGuideVisible) {
                    // ── Setup Guide (first-time empty state) ──
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        SetupGuideCard(
                            categoryCount = homeState.setupCategoryCount,
                            productCount = homeState.setupProductCount,
                            supplierCount = homeState.setupSupplierCount,
                            orderCount = homeState.setupOrderCount,
                            onNavigateCategory = { onNavigate(Screen.CategoryForm.createRoute()) },
                            onNavigateProduct = { onNavigate(Screen.ProductForm.createRoute()) },
                            onNavigateSupplier = { onNavigate(Screen.SupplierForm.createRoute()) },
                            onDismiss = { homeViewModel.dismissSetupGuide() },
                        )
                    }
                } else if (posState.products.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
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
                            top = 0.dp,
                            bottom = if (cart.isEmpty()) 8.dp else 88.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(posState.products, key = { "${it.id}_$stockVersion" }) { product ->
                            val qty = cart.items
                                .filter { it.product.id == product.id }
                                .sumOf { it.quantity }
                                .toInt()
                            val availableStock = posViewModel.cartHolder.getAvailableStock(product.id)
                            HomeProductCard(
                                product = product,
                                quantity = qty,
                                availableStock = availableStock,
                                onClick = {
                                    posViewModel.addToCart(product)
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
                    message = toastMessage ?: stringResource(R.string.toast_added),
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
                HomeCartBar(
                    itemCount = cart.totalQuantity.toInt(),
                    total = cart.subtotal,
                    onClick = { showCartSheet = true },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────
@Composable
private fun HomeHeader(
    storeName: String,
    userName: String,
    todayOrders: Int = 0,
    isDarkMode: Boolean = false,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onScanBarcode: () -> Unit,
    onAvatarClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onDarkModeClick: () -> Unit = {},
) {
    // Gradient brush for brand text
    val brandTextGradient = Brush.linearGradient(
        listOf(AppColors.TextPrimary, AppColors.BrandBlueLight),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Background),
    ) {
        // ── Brand row — height 48dp, padding: 0 8dp 0 20dp (matches HTML .header) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Brand icon + name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                // Brand icon — logo with shadow
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            ambientColor = AppColors.BrandNavy.copy(alpha = 0.4f),
                            spotColor = AppColors.BrandNavy.copy(alpha = 0.4f),
                        )
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.app_logo_circle),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                }
                // Brand text — gradient fill matching .brand-text CSS
                Text(
                    text = storeName,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = brandTextGradient,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }

            // Actions: theme toggle + notification badge + avatar
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Theme / dark mode toggle button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDarkModeClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = if (isDarkMode) "Switch to light mode" else "Switch to dark mode",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Notification bell with badge
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOrdersClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Receipt,
                            contentDescription = "Today's orders",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    // Red badge — top-right, fixed circle (show today's order count)
                    if (todayOrders > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-2).dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(AppColors.Error)
                                .border(1.5.dp, AppColors.Background, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (todayOrders > 99) "99+" else todayOrders.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                lineHeight = 9.sp,
                            )
                        }
                    }
                }

                // Avatar with glow ring
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.5.dp, AppColors.AccentGlow, CircleShape)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(AppColors.Accent, AppColors.Primary)),
                        )
                        .clickable(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (userName.isNotEmpty()) userName.first().uppercase() else "A",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
            }
        }

        // ── Search row — scan btn INSIDE the search bar (matches HTML .search-bar) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(AppColors.InputBackground, RoundedCornerShape(MiniPosTokens.Radius2xl))
                    .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.Radius2xl))
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = AppColors.TextTertiary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(stringResource(R.string.product_search_hint), color = AppColors.TextTertiary, fontSize = 15.sp)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearch,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = AppColors.TextPrimary,
                            fontSize = 15.sp,
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Close, "Clear",
                        tint = AppColors.TextTertiary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onSearch("") },
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                // Scan button — inside the search bar, 36dp (matches HTML .scan-btn)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = AppColors.PrimaryGlow,
                            spotColor = AppColors.PrimaryGlow,
                        )
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AppColors.Primary, AppColors.Accent)))
                        .clickable(onClick = onScanBarcode),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.QrCodeScanner,
                        contentDescription = stringResource(R.string.scan_barcode_cd),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Category Chip
// ─────────────────────────────────────────────────────────────
@Composable
private fun HomeChip(label: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
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
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
        } else if (icon != null) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────
// Product Card
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeProductCard(
    product: Product,
    quantity: Int,
    availableStock: Double?,
    onClick: () -> Unit,
) {
    val isOutOfStock = product.trackInventory && (availableStock ?: 0.0) <= 0.0
    val isLowStock = product.trackInventory && !isOutOfStock && (availableStock ?: 0.0) <= 5.0
    var showImageViewer by remember { mutableStateOf(false) }

    if (showImageViewer && product.imagePath != null) {
        Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            ImageViewerScreen(images = listOf(product.imagePath!!), initialIndex = 0, onClose = { showImageViewer = false })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isOutOfStock) Modifier.graphicsLayer { alpha = 0.4f } else Modifier)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(
                width = if (quantity > 0) 1.5.dp else 1.dp,
                color = when {
                    quantity > 0 -> AppColors.Primary.copy(alpha = 0.6f)
                    else -> AppColors.Border
                },
                shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
            )
            .combinedClickable(
                onClick = { if (!isOutOfStock) onClick() },
                onLongClick = { if (product.imagePath != null) showImageViewer = true },
            ),
    ) {
        Column {
            // Thumbnail
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
                        Icons.Rounded.Inventory2, null,
                        modifier = Modifier.size(38.dp),
                        tint = when {
                            quantity > 0 -> AppColors.Primary.copy(alpha = 0.6f)
                            else -> AppColors.TextTertiary
                        },
                    )
                }

                // Quantity badge — top left (pop animation via remember)
                if (quantity > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                            .shadow(4.dp, CircleShape, ambientColor = AppColors.PrimaryGlow, spotColor = AppColors.PrimaryGlow)
                            .clip(CircleShape)
                            .background(AppColors.Primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$quantity",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }

                // Stock badge — top right (always show for tracked products)
                if (product.trackInventory) {
                    val stockInt = availableStock?.toInt() ?: 0
                    val (badgeBg, badgeTextColor, badgeText) = when {
                        isOutOfStock -> Triple(
                            AppColors.Error.copy(alpha = 0.88f),
                            Color.White,
                            stringResource(R.string.badge_out_of_stock),
                        )
                        isLowStock -> Triple(
                            AppColors.WarningSoft,
                            AppColors.Warning,
                            stringResource(R.string.badge_remaining_stock, stockInt),
                        )
                        else -> Triple(
                            Color.Black.copy(alpha = 0.55f),
                            Color.White,
                            stringResource(R.string.badge_stock_count, stockInt),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .background(badgeBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = badgeTextColor)
                    }
                }

                // Variants dot — bottom right
                if (product.hasVariants) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(AppColors.Accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Style, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
            }

            // Product info
            Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp)) {
                Text(
                    text = product.name,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp,
                    minLines = 2,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = CurrencyFormatter.format(product.sellingPrice),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = if (isOutOfStock) {
                            androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppColors.TextTertiary,
                            )
                        } else {
                            androidx.compose.ui.text.TextStyle(
                                brush = Brush.linearGradient(listOf(AppColors.Accent, AppColors.PrimaryLight)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        },
                    )
                }
                // Stock quantity row
                if (product.trackInventory) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Inventory2, null,
                            modifier = Modifier.size(11.dp),
                            tint = when {
                                isOutOfStock -> AppColors.Error
                                isLowStock -> AppColors.Warning
                                else -> AppColors.TextTertiary
                            },
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = when {
                                isOutOfStock -> stringResource(R.string.badge_out_of_stock)
                                else -> stringResource(R.string.badge_stock_count, availableStock?.toInt() ?: 0)
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                isOutOfStock -> AppColors.Error
                                isLowStock -> AppColors.Warning
                                else -> AppColors.TextTertiary
                            },
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Cart Bar
// ─────────────────────────────────────────────────────────────
@Composable
private fun HomeCartBar(itemCount: Int, total: Double, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(MiniPosTokens.Radius2xl),
                    ambientColor = AppColors.PrimaryGlow,
                    spotColor = AppColors.PrimaryGlow,
                )
                .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
                .background(Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight)))
                .clickable(onClick = onClick)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.cart_items_count, itemCount), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    CurrencyFormatter.format(total),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.3).sp,
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Bottom Navigation with Center FAB
// ─────────────────────────────────────────────────────────────
@Composable
private fun HomeBottomNav(
    onNavigate: (String) -> Unit,
    onPosClick: () -> Unit,
    currentRoute: String? = null,
) {
    // Pulse animation for FAB ring — plays 3 times then stops to save battery
    var pulseScale by remember { mutableFloatStateOf(1f) }
    var pulseAlpha by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        repeat(3) {
            animate(0.92f, 1.3f, animationSpec = tween(2500, easing = FastOutSlowInEasing)) { value, _ ->
                pulseScale = value
            }
        }
    }
    LaunchedEffect(Unit) {
        repeat(3) {
            animate(1f, 0f, animationSpec = tween(2500, easing = FastOutSlowInEasing)) { value, _ ->
                pulseAlpha = value
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(AppColors.Surface)
            .border(
                width = 1.dp,
                color = AppColors.Border,
                shape = RoundedCornerShape(0.dp),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom,
        ) {
            // Kho hàng
            NavItem(icon = Icons.Rounded.Warehouse, label = stringResource(R.string.nav_inventory), isActive = currentRoute == Screen.InventoryHub.route, onClick = { onNavigate(Screen.InventoryHub.route) })
            // Quản lý
            NavItem(icon = Icons.Rounded.ListAlt, label = stringResource(R.string.nav_management), isActive = currentRoute == Screen.StoreManagement.route, onClick = { onNavigate(Screen.StoreManagement.route) })
            // Spacer for FAB
            Spacer(modifier = Modifier.width(72.dp))
            // Báo cáo
            NavItem(icon = Icons.Rounded.BarChart, label = stringResource(R.string.nav_reports), isActive = currentRoute == Screen.Reports.route, onClick = { onNavigate(Screen.Reports.route) })
            // Thêm
            NavItem(icon = Icons.Rounded.MoreHoriz, label = stringResource(R.string.nav_more), isActive = currentRoute == Screen.Settings.route, onClick = { onNavigate(Screen.Settings.route) })
        }

        // FAB — center, floating above nav
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp),
            contentAlignment = Alignment.Center,
        ) {
            // Pulse ring
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .border(2.dp, AppColors.Primary.copy(alpha = 0.4f), CircleShape),
            )
            // FAB button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        ambientColor = AppColors.PrimaryGlow,
                        spotColor = AppColors.PrimaryGlow,
                    )
                    .clip(CircleShape)
                    .background(AppColors.Surface) // outer ring matching bg-card
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AppColors.Primary, AppColors.Accent, AppColors.PrimaryLight),
                        ),
                    )
                    .clickable(onClick = onPosClick),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.PointOfSale,
                        contentDescription = "POS",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        "POS",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.8.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
) {
    val iconTint by animateColorAsState(
        if (isActive) AppColors.Primary else AppColors.TextTertiary, tween(150), label = "navTint"
    )
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                .then(
                    if (isActive) Modifier.background(AppColors.Primary.copy(alpha = 0.12f))
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Text(label, fontSize = 11.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold, color = iconTint)
    }
}

// ─────────────────────────────────────────────────────────────
// Variant Picker Dialog
// ─────────────────────────────────────────────────────────────
@Composable
private fun HomeVariantPickerDialog(
    product: Product,
    variants: List<ProductVariant>,
    availableStock: Double?,
    variantStockMap: Map<String, Double>,
    isSharedStock: Boolean = false,
    onSelectVariant: (ProductVariant) -> Unit,
    onDismiss: () -> Unit,
) {
    val isOutOfStock = product.trackInventory && (availableStock ?: 0.0) <= 0.0
    val isLowStock = product.trackInventory && !isOutOfStock && (availableStock ?: 0.0) <= 5.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.Radius2xl)),
        ) {
            // ── Gradient Header ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(AppColors.Primary, AppColors.Accent)),
                        RoundedCornerShape(topStart = MiniPosTokens.Radius2xl, topEnd = MiniPosTokens.Radius2xl),
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Column(modifier = Modifier.padding(end = 32.dp)) {
                    Text(
                        text = product.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.select_variant_hint),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    // Stock badge in header
                    if (product.trackInventory) {
                        Spacer(Modifier.height(8.dp))
                        val stockInt = availableStock?.toInt() ?: 0
                        val (badgeBg, badgeText) = when {
                            isOutOfStock -> Color.White.copy(alpha = 0.2f) to Color.White
                            isLowStock -> AppColors.WarningSoft to AppColors.Warning
                            else -> Color.White.copy(alpha = 0.2f) to Color.White
                        }
                        Row(
                            modifier = Modifier
                                .background(badgeBg, RoundedCornerShape(MiniPosTokens.RadiusSm))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Inventory2,
                                contentDescription = null,
                                tint = badgeText,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = when {
                                    isOutOfStock -> stringResource(R.string.badge_out_of_stock)
                                    isLowStock -> stringResource(R.string.badge_remaining_stock, stockInt)
                                    else -> stringResource(R.string.badge_stock_count, stockInt)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeText,
                            )
                        }
                    }
                }
                // Close button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            // ── Variant list ──
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Shared stock notice — when inventory is at product-level only
                if (isSharedStock && product.trackInventory) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.InfoSoft, RoundedCornerShape(MiniPosTokens.RadiusSm))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Rounded.Info, null, modifier = Modifier.size(14.dp), tint = AppColors.Info)
                            Text(
                                text = stringResource(R.string.shared_stock_notice),
                                fontSize = 11.sp,
                                color = AppColors.Info,
                            )
                        }
                    }
                }
                items(variants) { variant ->
                    // When stock is shared (product-level), we can't determine per-variant out-of-stock
                    val variantStock = variantStockMap[variant.id]
                    val hasPerVariantStock = !isSharedStock && variantStockMap.isNotEmpty()
                    val variantOutOfStock = hasPerVariantStock && product.trackInventory && (variantStock ?: 0.0) <= 0.0
                    val variantLowStock = hasPerVariantStock && product.trackInventory && !variantOutOfStock && (variantStock ?: 0.0) <= 5.0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                            .background(AppColors.SurfaceElevated)
                            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(MiniPosTokens.RadiusMd))
                            .then(if (variantOutOfStock) Modifier.graphicsLayer { alpha = 0.5f } else Modifier)
                            .clickable(enabled = !variantOutOfStock) { onSelectVariant(variant) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Variant icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(AppColors.Accent.copy(alpha = 0.1f), RoundedCornerShape(MiniPosTokens.RadiusSm)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Style, null, modifier = Modifier.size(20.dp), tint = AppColors.Accent)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        // Variant info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                variant.variantName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary,
                            )
                            if (variant.attributes != "{}" && variant.attributes.isNotBlank()) {
                                Text(
                                    variant.attributes,
                                    fontSize = 11.sp,
                                    color = AppColors.TextTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            // Per-variant stock (only when per-variant inventory exists)
                            if (product.trackInventory && hasPerVariantStock) {
                                val stockInt = variantStock?.toInt() ?: 0
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Inventory2, null,
                                        modifier = Modifier.size(10.dp),
                                        tint = when {
                                            variantOutOfStock -> AppColors.Error
                                            variantLowStock -> AppColors.Warning
                                            else -> AppColors.TextTertiary
                                        },
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = when {
                                            variantOutOfStock -> stringResource(R.string.badge_out_of_stock)
                                            variantLowStock -> stringResource(R.string.badge_remaining_stock, stockInt)
                                            else -> stringResource(R.string.badge_stock_count, stockInt)
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when {
                                            variantOutOfStock -> AppColors.Error
                                            variantLowStock -> AppColors.Warning
                                            else -> AppColors.TextTertiary
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Price
                        Text(
                            CurrencyFormatter.format(variant.sellingPrice ?: product.sellingPrice),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.Secondary,
                        )
                    }
                }
            }

            // ── Footer with Cancel button ──
            HorizontalDivider(color = AppColors.Divider)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.cancel_btn_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextSecondary,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CART BOTTOM SHEET CONTENT
// Replaces separate POS Step 2 (quantity/price) + Step 3 (customer)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HomeCartSheetContent(
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
        HomeOrderDiscountDialog(
            current = cart.orderDiscount,
            onDismiss = onDismissOrderDiscount,
            onApply = { discount ->
                onApplyOrderDiscount(discount)
                onDismissOrderDiscount()
            },
        )
    }

    if (showCustomerPicker) {
        HomeCustomerPickerSheet(
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
                HomeCartSheetItem(
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
            HomeSheetSummaryRow(stringResource(R.string.subtotal), CurrencyFormatter.format(cart.subtotal))
            if (cart.taxAmount > 0) {
                HomeSheetSummaryRow(stringResource(R.string.tax), CurrencyFormatter.format(cart.taxAmount))
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

// ─── Cart Sheet Item ───
@Composable
private fun HomeCartSheetItem(
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

// ─── Sheet Summary Row ───
@Composable
private fun HomeSheetSummaryRow(label: String, value: String) {
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

// ─── Customer Picker Dialog ───
@Composable
private fun HomeCustomerPickerSheet(
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
                HomeQuickCreateCustomerCard(onCancel = onToggleCreateForm, onCreate = onQuickCreate)
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
private fun HomeQuickCreateCustomerCard(
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

// ─────────────────────────────────────────────────────────────
// Profile Bottom Sheet
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeProfileSheet(
    userName: String,
    userRole: UserRole,
    store: com.minipos.domain.model.Store?,
    currentUserHasPin: Boolean,
    isDark: Boolean,
    onToggleDarkMode: () -> Unit,
    onChangePinClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSwitchUser: () -> Unit,
    onLogout: () -> Unit,
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
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.BorderLight),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            // ── Avatar + Identity ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Large avatar with gradient + glow
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = AppColors.AccentGlow,
                            spotColor = AppColors.AccentGlow,
                        )
                        .border(2.dp, AppColors.AccentGlow, CircleShape)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(AppColors.Accent, AppColors.Primary)),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (userName.isNotEmpty()) userName.first().uppercase() else "A",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                // Role badge
                val (roleLabel, roleBg, roleColor) = when (userRole) {
                    UserRole.OWNER -> Triple(
                        stringResource(R.string.role_owner),
                        AppColors.Primary.copy(alpha = 0.12f),
                        AppColors.Primary,
                    )
                    UserRole.MANAGER -> Triple(
                        stringResource(R.string.role_manager),
                        AppColors.Accent.copy(alpha = 0.12f),
                        AppColors.Accent,
                    )
                    UserRole.CASHIER -> Triple(
                        stringResource(R.string.role_cashier),
                        AppColors.Success.copy(alpha = 0.12f),
                        AppColors.Success,
                    )
                }
                Box(
                    modifier = Modifier
                        .background(roleBg, RoundedCornerShape(MiniPosTokens.RadiusFull))
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = roleLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = roleColor,
                        letterSpacing = 0.3.sp,
                    )
                }
            }

            // ── Store Info Card ──
            if (store != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                        .background(AppColors.SurfaceVariant)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                            .background(
                                Brush.linearGradient(
                                    listOf(AppColors.BrandNavy, AppColors.BrandBlue),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = store.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.TextPrimary,
                        )
                        if (!store.address.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = store.address,
                                fontSize = 12.sp,
                                color = AppColors.TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!store.phone.isNullOrBlank()) {
                            Text(
                                text = store.phone,
                                fontSize = 12.sp,
                                color = AppColors.TextTertiary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            HorizontalDivider(
                color = AppColors.Divider,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(8.dp))

            // ── Quick actions ──
            // Dark mode toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleDarkMode)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF0A0E1A), Color(0xFF334155))),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dark_mode_label),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        text = if (isDark) stringResource(R.string.toggle_on) else stringResource(R.string.toggle_off),
                        fontSize = 11.sp,
                        color = AppColors.TextTertiary,
                    )
                }
                Switch(
                    checked = isDark,
                    onCheckedChange = { onToggleDarkMode() },
                    colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Primary),
                )
            }

            HorizontalDivider(
                color = AppColors.Divider,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // Change PIN row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChangePinClick)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF00B894), Color(0xFF00CEC9))),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (currentUserHasPin) stringResource(R.string.change_pin_title) else stringResource(R.string.set_pin_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        text = if (currentUserHasPin) stringResource(R.string.change_pin_desc) else stringResource(R.string.set_pin_desc),
                        fontSize = 11.sp,
                        color = AppColors.TextTertiary,
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }

            HorizontalDivider(
                color = AppColors.Divider,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // Settings row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToSettings)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF0E9AA0), Color(0xFF2EC4B6))),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_label),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.settings_desc_short),
                        fontSize = 11.sp,
                        color = AppColors.TextTertiary,
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }

            HorizontalDivider(
                color = AppColors.Divider,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // ── Switch User row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSwitchUser)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF74B9FF))),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.SwitchAccount,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.switch_user),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.switch_user_desc),
                        fontSize = 11.sp,
                        color = AppColors.TextTertiary,
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }

            HorizontalDivider(
                color = AppColors.Divider,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // ── Logout row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLogout)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFFF7675))),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Logout,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.logout),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Error,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Order Discount Dialog ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeOrderDiscountDialog(    current: Discount?,
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
                    suffix = { Text(if (type == "percent") "%" else stringResource(R.string.currency_symbol)) },
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
