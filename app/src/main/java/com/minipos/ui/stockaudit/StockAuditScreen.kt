package com.minipos.ui.stockaudit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.StockOverviewItem
import com.minipos.ui.components.*

// ═══════════════════════════════════════════════════════
// STOCK AUDIT SCREEN
// Based on stock-adjust.html mock design
// ═══════════════════════════════════════════════════════

@Composable
fun StockAuditScreen(
    onBack: () -> Unit,
    viewModel: StockAuditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showBarcodeScanner by remember { mutableStateOf(false) }

    // Navigate back after save
    LaunchedEffect(state.saved) {
        if (state.saved) {
            onBack()
        }
    }

    // Toast
    if (state.toastMessage != null) {
        MiniPosToast(
            message = state.toastMessage!!,
            visible = true,
            onDismiss = { viewModel.dismissToast() },
        )
    }

    // Barcode scanner overlay
    if (showBarcodeScanner) {
        com.minipos.ui.scanner.BarcodeScannerScreen(
            onBarcodeScanned = { value, _ ->
                viewModel.addProductByBarcode(value)
                showBarcodeScanner = false
            },
            onClose = { showBarcodeScanner = false },
            title = stringResource(R.string.scan_barcode_to_add),
        )
        return
    }

    // Product picker dialog
    if (state.showProductSearch) {
        ProductPickerDialog(
            searchQuery = state.searchQuery,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            products = state.filteredSearchResults,
            onProductSelected = { viewModel.addProduct(it) },
            onDismiss = { viewModel.dismissProductSearch() },
            onScanClick = {
                viewModel.dismissProductSearch()
                showBarcodeScanner = true
            },
        )
    }

    Scaffold(
        containerColor = AppColors.Background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
        ) {
            // ── Top Bar ──
            MiniPosTopBar(
                title = stringResource(R.string.sa_title),
                onBack = onBack,
                actions = {
                    if (state.auditItems.isNotEmpty()) {
                        IconButton(onClick = { viewModel.exportAuditReport(context) }) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = stringResource(R.string.sa_export_report),
                                tint = AppColors.TextSecondary,
                            )
                        }
                    }
                },
            )

            // ── Scrollable body ──
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ── Type Tabs ──
                item {
                    AuditTypeTabs(
                        activeTab = state.activeTab,
                        onTabSelected = { viewModel.selectTab(it) },
                    )
                }

                // ── Info Banner ──
                item {
                    InfoBanner(activeTab = state.activeTab)
                }

                // ── Session Info ──
                item {
                    SessionInfoSection(
                        sessionCode = state.sessionCode,
                        auditDate = state.auditDate,
                        reason = state.reason,
                        onReasonChange = { viewModel.updateReason(it) },
                    )
                }

                // ── Products section label ──
                item {
                    SectionTitle(title = stringResource(R.string.sa_products_section))
                }

                // ── Search + Scan ──
                item {
                    ProductSearchRow(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        searchResults = state.filteredSearchResults,
                        onProductSelected = { product ->
                            viewModel.addProduct(product)
                        },
                        onScanClick = { showBarcodeScanner = true },
                    )
                }

                // ── Audit Items ──
                if (state.auditItems.isEmpty()) {
                    item {
                        EmptyAuditState()
                    }
                } else {
                    items(
                        items = state.auditItems,
                        key = { it.productId },
                    ) { item ->
                        AuditItemCard(
                            item = item,
                            activeTab = state.activeTab,
                            onRemove = { viewModel.removeProduct(item.productId) },
                            onActualQtyChange = { viewModel.updateActualQty(item.productId, it) },
                            onShelfLocationChange = { viewModel.updateShelfLocation(item.productId, it) },
                            onDiffReasonChange = { viewModel.updateDiffReason(item.productId, it) },
                        )
                    }
                }

                // ── Add product button ──
                item {
                    AddProductButton(
                        onClick = { viewModel.showProductSearch() },
                    )
                }

                // ── Notes ──
                item {
                    NotesSection(
                        notes = state.notes,
                        onNotesChange = { viewModel.updateNotes(it) },
                    )
                }

                // ── Summary ──
                if (state.auditItems.isNotEmpty()) {
                    item {
                        AuditSummary(
                            totalProducts = state.auditItems.size,
                            matchCount = viewModel.matchCount,
                            shortageCount = viewModel.shortageCount,
                            surplusCount = viewModel.surplusCount,
                            totalShortage = viewModel.totalShortage,
                            totalSurplus = viewModel.totalSurplus,
                            netDifference = viewModel.netDifference,
                            activeTab = state.activeTab,
                        )
                    }
                }

                // ── Confirm Button ──
                item {
                    ConfirmAuditButton(
                        activeTab = state.activeTab,
                        enabled = state.auditItems.isNotEmpty() && state.reason.isNotBlank() && !state.isSaving,
                        isSaving = state.isSaving,
                        onClick = {
                            viewModel.confirmAudit()
                        },
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// TYPE TABS — 3 pill buttons matching HTML .type-tabs
// ═══════════════════════════════════════════════════════

@Composable
private fun AuditTypeTabs(
    activeTab: AuditTabType,
    onTabSelected: (AuditTabType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.InputBackground)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TabPill(
            label = stringResource(R.string.sa_tab_audit),
            icon = Icons.Default.Inventory2,
            isActive = activeTab == AuditTabType.STOCK_AUDIT,
            onClick = { onTabSelected(AuditTabType.STOCK_AUDIT) },
            modifier = Modifier.weight(1f),
        )
        TabPill(
            label = stringResource(R.string.sa_tab_add_stock),
            icon = Icons.Default.AddCircle,
            isActive = activeTab == AuditTabType.ADD_STOCK,
            onClick = { onTabSelected(AuditTabType.ADD_STOCK) },
            modifier = Modifier.weight(1f),
        )
        TabPill(
            label = stringResource(R.string.sa_tab_remove_stock),
            icon = Icons.Default.RemoveCircle,
            isActive = activeTab == AuditTabType.REMOVE_STOCK,
            onClick = { onTabSelected(AuditTabType.REMOVE_STOCK) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        if (isActive) AppColors.Surface else Color.Transparent,
        label = "tabBg",
    )
    val textColor by animateColorAsState(
        if (isActive) AppColors.TextPrimary else AppColors.TextTertiary,
        label = "tabText",
    )

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ═══════════════════════════════════════════════════════
// INFO BANNER — matching HTML .info-banner
// ═══════════════════════════════════════════════════════

@Composable
private fun InfoBanner(activeTab: AuditTabType) {
    val message = when (activeTab) {
        AuditTabType.STOCK_AUDIT -> stringResource(R.string.sa_info_audit)
        AuditTabType.ADD_STOCK -> stringResource(R.string.sa_info_add)
        AuditTabType.REMOVE_STOCK -> stringResource(R.string.sa_info_remove)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.PrimaryContainer.copy(alpha = 0.3f))
            .border(
                1.dp,
                AppColors.Primary.copy(alpha = 0.15f),
                RoundedCornerShape(MiniPosTokens.RadiusLg),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = AppColors.PrimaryLight,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = AppColors.TextSecondary,
            lineHeight = 18.sp,
        )
    }
}

// ═══════════════════════════════════════════════════════
// SESSION INFO — code, date, reason
// ═══════════════════════════════════════════════════════

@Composable
private fun SessionInfoSection(
    sessionCode: String,
    auditDate: String,
    reason: String,
    onReasonChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Row: code + date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Session code (readonly)
            Column(modifier = Modifier.weight(1f)) {
                FieldLabel(text = stringResource(R.string.sa_session_code))
                ReadOnlyField(value = sessionCode)
            }
            // Audit date (readonly)
            Column(modifier = Modifier.weight(1f)) {
                FieldLabel(text = stringResource(R.string.sa_audit_date))
                ReadOnlyField(value = auditDate)
            }
        }
        // Reason
        Column {
            FieldLabel(
                text = stringResource(R.string.sa_reason_required),
                isRequired = true,
            )
            TextInputField(
                value = reason,
                onValueChange = onReasonChange,
                placeholder = stringResource(R.string.sa_reason_hint),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// PRODUCT SEARCH ROW — search bar + scan button
// ═══════════════════════════════════════════════════════

@Composable
private fun ProductSearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<StockOverviewItem>,
    onProductSelected: (StockOverviewItem) -> Unit,
    onScanClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Search bar
            MiniPosSearchBar(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.sa_search_hint),
                modifier = Modifier.weight(1f),
            )
            // Scan button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MiniPosGradients.primary())
                    .clickable { onScanClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // Search results dropdown
        if (searchResults.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                    .background(AppColors.Surface)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg)),
            ) {
                searchResults.forEachIndexed { index, product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProductSelected(product) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.productName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextPrimary,
                            )
                            Text(
                                text = "SKU: ${product.productSku} · Stock: ${product.currentStock.toLong()}",
                                fontSize = 11.sp,
                                color = AppColors.TextTertiary,
                            )
                        }
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    if (index < searchResults.lastIndex) {
                        HorizontalDivider(color = AppColors.Border)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// AUDIT ITEM CARD — matches .adj-item in HTML
// ═══════════════════════════════════════════════════════

@Composable
private fun AuditItemCard(
    item: AuditItem,
    activeTab: AuditTabType,
    onRemove: () -> Unit,
    onActualQtyChange: (String) -> Unit,
    onShelfLocationChange: (String) -> Unit,
    onDiffReasonChange: (DiffReason) -> Unit,
) {
    val diff = item.difference
    val hasInput = item.actualQty.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .animateContentSize()
            .padding(16.dp),
    ) {
        // ── Top: name + SKU + delete button ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "SKU: ${item.productSku}",
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                )
            }
            // Delete button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AppColors.Error.copy(alpha = 0.1f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = AppColors.Error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Stock comparison bar (for STOCK_AUDIT tab) ──
        if (activeTab == AuditTabType.STOCK_AUDIT) {
            StockComparisonBar(
                systemStock = item.systemStock,
                actualQty = item.actualQtyDouble,
                hasInput = hasInput,
            )
            Spacer(Modifier.height(8.dp))
        } else {
            // Show current stock for add/remove tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                    .background(AppColors.InputBackground)
                    .padding(8.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sa_system_stock),
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = item.systemStock.toLong().toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Input fields row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Qty input
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (activeTab) {
                        AuditTabType.STOCK_AUDIT -> stringResource(R.string.sa_actual_qty)
                        AuditTabType.ADD_STOCK -> stringResource(R.string.sa_qty_to_add)
                        AuditTabType.REMOVE_STOCK -> stringResource(R.string.sa_qty_to_remove)
                    },
                    fontSize = 10.sp,
                    color = AppColors.TextTertiary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(3.dp))
                NumberInputField(
                    value = item.actualQty,
                    onValueChange = onActualQtyChange,
                )
            }

            // Second field: shelf location (audit) or diff reason (if diff != 0)
            Column(modifier = Modifier.weight(1f)) {
                if (activeTab == AuditTabType.STOCK_AUDIT && item.isMatch) {
                    // When match, show shelf location
                    Text(
                        text = stringResource(R.string.sa_shelf_location),
                        fontSize = 10.sp,
                        color = AppColors.TextTertiary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(3.dp))
                    SmallTextInput(
                        value = item.shelfLocation,
                        onValueChange = onShelfLocationChange,
                        placeholder = stringResource(R.string.sa_shelf_hint),
                    )
                } else {
                    // Show diff reason selector
                    Text(
                        text = stringResource(R.string.sa_diff_reason),
                        fontSize = 10.sp,
                        color = AppColors.TextTertiary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(3.dp))
                    DiffReasonSelector(
                        selected = item.diffReason,
                        onSelected = onDiffReasonChange,
                    )
                }
            }
        }

        // ── Difference indicator ──
        if (hasInput && activeTab == AuditTabType.STOCK_AUDIT) {
            Spacer(Modifier.height(8.dp))
            DifferenceIndicator(diff = diff)
        }
    }
}

// ═══════════════════════════════════════════════════════
// STOCK COMPARISON BAR — system → actual
// ═══════════════════════════════════════════════════════

@Composable
private fun StockComparisonBar(
    systemStock: Double,
    actualQty: Double,
    hasInput: Boolean,
) {
    val actualColor = when {
        !hasInput -> AppColors.PrimaryLight
        actualQty == systemStock -> AppColors.PrimaryLight
        actualQty < systemStock -> AppColors.Error
        else -> AppColors.Success
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .background(AppColors.InputBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // System stock
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.sa_system_stock),
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = systemStock.toLong().toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
            )
        }
        // Arrow
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
        // Actual count
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = stringResource(R.string.sa_actual_count),
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (hasInput) actualQty.toLong().toString() else "—",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = actualColor,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// DIFFERENCE INDICATOR — bottom of each card
// ═══════════════════════════════════════════════════════

@Composable
private fun DifferenceIndicator(diff: Double) {
    val intDiff = diff.toInt()
    val (icon, text, color) = when {
        intDiff == 0 -> Triple(
            Icons.Default.CheckCircle,
            stringResource(R.string.sa_match),
            AppColors.Success,
        )
        intDiff < 0 -> Triple(
            Icons.Default.TrendingDown,
            stringResource(R.string.sa_shortage, kotlin.math.abs(intDiff)),
            AppColors.Error,
        )
        else -> Triple(
            Icons.Default.TrendingUp,
            stringResource(R.string.sa_surplus, intDiff),
            AppColors.Success,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
        )
    }
}

// ═══════════════════════════════════════════════════════
// DIFF REASON SELECTOR — MiniPosSelectBoxCompact
// ═══════════════════════════════════════════════════════

@Composable
private fun DiffReasonSelector(
    selected: DiffReason,
    onSelected: (DiffReason) -> Unit,
) {
    val reasons = listOf(
        DiffReason.LOSS to stringResource(R.string.sa_reason_loss),
        DiffReason.DAMAGED to stringResource(R.string.sa_reason_damaged),
        DiffReason.THEFT to stringResource(R.string.sa_reason_theft),
        DiffReason.NOT_UPDATED to stringResource(R.string.sa_reason_not_updated),
        DiffReason.MISCOUNT to stringResource(R.string.sa_reason_miscount),
        DiffReason.OTHER to stringResource(R.string.sa_reason_other),
    )

    val items = reasons.map { (reason, label) ->
        SelectListItem(id = reason.name, name = label)
    }

    val title = stringResource(R.string.sa_diff_reason_title)

    MiniPosSelectBoxCompact(
        title = title,
        items = items,
        selectedId = selected.name,
        onSelect = { item ->
            val reason = DiffReason.valueOf(item.id)
            onSelected(reason)
        },
    )
}

// ═══════════════════════════════════════════════════════
// ADD PRODUCT BUTTON — dashed border style
// ═══════════════════════════════════════════════════════

@Composable
private fun AddProductButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(
                width = 2.dp,
                color = AppColors.BorderLight,
                shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.sa_add_product),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextTertiary,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// NOTES SECTION
// ═══════════════════════════════════════════════════════

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit,
) {
    Column {
        FieldLabel(text = stringResource(R.string.sa_notes))
        TextAreaField(
            value = notes,
            onValueChange = onNotesChange,
            placeholder = stringResource(R.string.sa_notes_hint),
        )
    }
}

// ═══════════════════════════════════════════════════════
// SUMMARY CARD — matching .summary in HTML
// ═══════════════════════════════════════════════════════

@Composable
private fun AuditSummary(
    totalProducts: Int,
    matchCount: Int,
    shortageCount: Int,
    surplusCount: Int,
    totalShortage: Double,
    totalSurplus: Double,
    netDifference: Double,
    activeTab: AuditTabType,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(16.dp),
    ) {
        // Total products
        SummaryRow(
            label = stringResource(R.string.sa_summary_total),
            value = stringResource(R.string.sa_products_count, totalProducts),
        )

        if (activeTab == AuditTabType.STOCK_AUDIT) {
            // Match
            SummaryRow(
                label = stringResource(R.string.sa_summary_match),
                value = stringResource(R.string.sa_items_count, matchCount),
                valueColor = AppColors.Success,
            )
            // Shortage
            SummaryRow(
                label = stringResource(R.string.sa_summary_shortage),
                value = "${shortageCount} SP (${totalShortage.toInt()})",
                valueColor = AppColors.Error,
            )
            // Surplus
            SummaryRow(
                label = stringResource(R.string.sa_summary_surplus),
                value = "${surplusCount} SP (+${totalSurplus.toInt()})",
                valueColor = AppColors.Success,
            )

            // Divider
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(
                thickness = 2.dp,
                color = AppColors.BorderLight,
            )
            Spacer(Modifier.height(8.dp))

            // Net difference
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.sa_summary_net),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = AppColors.TextPrimary,
                )
                Text(
                    text = if (netDifference >= 0) "+${netDifference.toInt()}" else "${netDifference.toInt()}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = if (netDifference < 0) AppColors.Error else AppColors.Success,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = AppColors.TextSecondary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = AppColors.TextSecondary,
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

// ═══════════════════════════════════════════════════════
// CONFIRM BUTTON — gradient pill
// ═══════════════════════════════════════════════════════

@Composable
private fun ConfirmAuditButton(
    activeTab: AuditTabType,
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
) {
    val text = when (activeTab) {
        AuditTabType.STOCK_AUDIT -> stringResource(R.string.sa_confirm_audit)
        AuditTabType.ADD_STOCK -> stringResource(R.string.sa_confirm_add)
        AuditTabType.REMOVE_STOCK -> stringResource(R.string.sa_confirm_remove)
    }

    val icon = Icons.Default.Verified

    MiniPosGradientButton(
        text = if (isSaving) "..." else text,
        onClick = onClick,
        icon = icon,
        enabled = enabled,
        height = 56.dp,
    )
}

// ═══════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════

@Composable
private fun EmptyAuditState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Inventory2,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.sa_no_products),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.sa_no_products_desc),
            fontSize = 12.sp,
            color = AppColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

// ═══════════════════════════════════════════════════════
// REUSABLE INPUT COMPONENTS
// ═══════════════════════════════════════════════════════

@Composable
private fun FieldLabel(
    text: String,
    isRequired: Boolean = false,
) {
    Row(
        modifier = Modifier.padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextSecondary,
        )
    }
}

@Composable
private fun ReadOnlyField(value: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.InputBackground)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
            color = AppColors.TextTertiary,
        )
    }
}

@Composable
private fun TextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.InputBackground)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(horizontal = 16.dp),
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
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = AppColors.TextPrimary,
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TextAreaField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.InputBackground)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(12.dp, 12.dp),
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusSm))
            .background(AppColors.InputBackground)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusSm))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                // Only allow numbers and decimal
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                    onValueChange(newValue)
                }
            },
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SmallTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusSm))
            .background(AppColors.InputBackground)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusSm))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontSize = 12.sp,
                color = AppColors.TextTertiary,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ═══════════════════════════════════════════════════════
// PRODUCT PICKER DIALOG — full-screen dialog to select products
// ═══════════════════════════════════════════════════════

@Composable
private fun ProductPickerDialog(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    products: List<StockOverviewItem>,
    onProductSelected: (StockOverviewItem) -> Unit,
    onDismiss: () -> Unit,
    onScanClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        title = {
            Text(
                text = stringResource(R.string.sa_add_product),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp),
            ) {
                // Search bar + scan button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniPosSearchBar(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = stringResource(R.string.sa_search_hint),
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MiniPosGradients.primary())
                            .clickable { onScanClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Product list
                if (products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.stock_mgmt_no_products),
                            fontSize = 14.sp,
                            color = AppColors.TextTertiary,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(
                            items = products,
                            key = { it.productId },
                        ) { product ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onProductSelected(product) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.productName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "SKU: ${product.productSku} · Stock: ${product.currentStock.toLong()}",
                                        fontSize = 11.sp,
                                        color = AppColors.TextTertiary,
                                    )
                                }
                                Icon(
                                    Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            HorizontalDivider(color = AppColors.Border)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_btn))
            }
        },
    )
}
