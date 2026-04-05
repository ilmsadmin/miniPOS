package com.minipos.ui.stockmanagement

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.StockOverviewItem
import com.minipos.ui.components.*

// ═══════════════════════════════════════════════════════
// STOCK MANAGEMENT SCREEN
// Based on stock-management.html mock design
// ═══════════════════════════════════════════════════════

@Composable
fun StockManagementScreen(
    onBack: () -> Unit,
    onNavigateToStockAudit: () -> Unit = {},
    viewModel: StockManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val items = viewModel.filteredItems

    // Toast
    if (state.toastMessage != null) {
        MiniPosToast(
            message = state.toastMessage!!,
            visible = true,
            onDismiss = { viewModel.dismissToast() },
        )
    }

    Scaffold(
        containerColor = AppColors.Background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ── Top Bar ──
            StockMgmtTopBar(
                onBack = onBack,
                onExport = {
                    viewModel.showToast(
                        // Mirroring the HTML toast: "Xuất báo cáo kho"
                        "Xuất báo cáo kho"
                    )
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
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Search + Sort row
                item {
                    SearchSortRow(
                        searchQuery = state.searchQuery,
                        onSearchChange = { viewModel.updateSearch(it) },
                        onSort = { viewModel.toggleSort() },
                    )
                }

                // Filter chips
                item {
                    FilterChipsRow(
                        activeFilter = state.activeFilter,
                        allCount = viewModel.allCount,
                        inStockCount = viewModel.inStockCount,
                        lowCount = viewModel.lowCount,
                        outCount = viewModel.outCount,
                        onFilterChange = { viewModel.setFilter(it) },
                    )
                }

                // Loading
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
                } else if (items.isEmpty()) {
                    // Empty state
                    item {
                        EmptyStockState()
                    }
                } else {
                    // Stock item cards
                    items(
                        items = items,
                        key = { it.productId },
                    ) { item ->
                        StockItemCard(item = item)
                    }
                }

                // Audit FAB - sticky at bottom
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    AuditFab(
                        onClick = onNavigateToStockAudit,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// TOP BAR
// ═══════════════════════════════════════════════════════

@Composable
private fun StockMgmtTopBar(
    onBack: () -> Unit,
    onExport: () -> Unit,
) {
    MiniPosTopBar(
        title = stringResource(R.string.stock_mgmt_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = onExport) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = stringResource(R.string.stock_mgmt_export_report),
                    tint = AppColors.TextSecondary,
                )
            }
        },
    )
}

// ═══════════════════════════════════════════════════════
// SEARCH + SORT ROW
// ═══════════════════════════════════════════════════════

@Composable
private fun SearchSortRow(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSort: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Search bar (flex:1 in HTML)
        MiniPosSearchBar(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = stringResource(R.string.stock_mgmt_search_hint),
            modifier = Modifier.weight(1f),
        )

        // Sort button (circle, 44dp)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(1.dp, AppColors.Border, CircleShape)
                .background(AppColors.Surface)
                .clickable(onClick = onSort),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Sort,
                contentDescription = stringResource(R.string.stock_mgmt_sort),
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// FILTER CHIPS ROW
// ═══════════════════════════════════════════════════════

@Composable
private fun FilterChipsRow(
    activeFilter: StockFilter,
    allCount: Int,
    inStockCount: Int,
    lowCount: Int,
    outCount: Int,
    onFilterChange: (StockFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StockFilterChip(
            label = stringResource(R.string.stock_mgmt_filter_all),
            count = allCount,
            isActive = activeFilter == StockFilter.ALL,
            chipCountColor = null, // default for "All"
            onClick = { onFilterChange(StockFilter.ALL) },
        )
        StockFilterChip(
            label = stringResource(R.string.stock_mgmt_filter_in_stock),
            count = inStockCount,
            isActive = activeFilter == StockFilter.IN_STOCK,
            chipCountColor = ChipCountColor(
                background = AppColors.SuccessSoft,
                text = AppColors.Success,
            ),
            onClick = { onFilterChange(StockFilter.IN_STOCK) },
        )
        StockFilterChip(
            label = stringResource(R.string.stock_mgmt_filter_low),
            count = lowCount,
            isActive = activeFilter == StockFilter.LOW,
            chipCountColor = ChipCountColor(
                background = AppColors.WarningSoft,
                text = AppColors.Warning,
            ),
            onClick = { onFilterChange(StockFilter.LOW) },
        )
        StockFilterChip(
            label = stringResource(R.string.stock_mgmt_filter_out),
            count = outCount,
            isActive = activeFilter == StockFilter.OUT,
            chipCountColor = ChipCountColor(
                background = AppColors.ErrorContainer,
                text = AppColors.Error,
            ),
            onClick = { onFilterChange(StockFilter.OUT) },
        )
    }
}

private data class ChipCountColor(
    val background: Color,
    val text: Color,
)

@Composable
private fun StockFilterChip(
    label: String,
    count: Int,
    isActive: Boolean,
    chipCountColor: ChipCountColor?,
    onClick: () -> Unit,
) {
    val bgColor = if (isActive) {
        Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight))
    } else {
        Brush.linearGradient(listOf(AppColors.Surface, AppColors.Surface))
    }
    val borderColor = if (isActive) Color.Transparent else AppColors.Border
    val textColor = if (isActive) Color.White else AppColors.TextSecondary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
            .background(bgColor)
            .then(
                if (!isActive) Modifier.border(1.dp, borderColor, RoundedCornerShape(MiniPosTokens.RadiusFull))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
        Spacer(Modifier.width(6.dp))
        // Count badge
        val countBg = when {
            isActive -> Color.White.copy(alpha = 0.2f)
            chipCountColor != null -> chipCountColor.background
            else -> AppColors.InputBackground
        }
        val countTextColor = when {
            isActive -> Color.White
            chipCountColor != null -> chipCountColor.text
            else -> AppColors.TextSecondary
        }
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 18.dp)
                .height(18.dp)
                .background(countBg, RoundedCornerShape(MiniPosTokens.RadiusFull))
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = countTextColor,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// STOCK ITEM CARD
// ═══════════════════════════════════════════════════════

private enum class StockStatus { OK, LOW, OUT }

private fun getStockStatus(item: StockOverviewItem): StockStatus {
    return when {
        item.currentStock <= 0 -> StockStatus.OUT
        item.currentStock <= item.minStock -> StockStatus.LOW
        else -> StockStatus.OK
    }
}

@Composable
private fun StockItemCard(item: StockOverviewItem) {
    val status = getStockStatus(item)
    val maxStock = (item.minStock * 3).coerceAtLeast(1) // Approximate max for progress bar
    val progress = (item.currentStock / maxStock).toFloat().coerceIn(0f, 1f)

    val statusColor = when (status) {
        StockStatus.OK -> AppColors.Success
        StockStatus.LOW -> AppColors.Warning
        StockStatus.OUT -> AppColors.Error
    }

    val statusBadgeText = when (status) {
        StockStatus.OK -> stringResource(R.string.stock_mgmt_badge_ok)
        StockStatus.LOW -> stringResource(R.string.stock_mgmt_badge_low)
        StockStatus.OUT -> stringResource(R.string.stock_mgmt_badge_out)
    }

    val statusBadgeBg = when (status) {
        StockStatus.OK -> AppColors.SuccessSoft
        StockStatus.LOW -> AppColors.WarningSoft
        StockStatus.OUT -> AppColors.ErrorContainer
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .animateContentSize()
            .padding(16.dp),
    ) {
        // Top row: product name + status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.productName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            // Badge
            Text(
                text = statusBadgeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor,
                letterSpacing = 0.3.sp,
                modifier = Modifier
                    .background(statusBadgeBg, RoundedCornerShape(MiniPosTokens.RadiusFull))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }

        // SKU line
        Spacer(Modifier.height(4.dp))
        Text(
            text = "SKU: ${item.productSku} · ${item.productUnit}",
            fontSize = 11.sp,
            color = AppColors.TextTertiary,
        )

        // Progress bar + quantity
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Progress bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppColors.InputBackground),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(statusColor),
                )
            }
            // Quantity text: "85/100"
            Text(
                text = "${item.currentStock.toLong()}/${maxStock}",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextSecondary,
            )
        }

        // Bottom row: cost price + stock value
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(
                    R.string.stock_mgmt_cost_prefix,
                    CurrencyFormatter.format(item.costPrice),
                ),
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
            Text(
                text = CurrencyFormatter.format(item.stockValue),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Accent,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════

@Composable
private fun EmptyStockState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Inventory2,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.stock_mgmt_no_products),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.stock_mgmt_no_products_desc),
            fontSize = 13.sp,
            color = AppColors.TextTertiary,
        )
    }
}

// ═══════════════════════════════════════════════════════
// AUDIT FAB (Gradient button at bottom, matching HTML)
// ═══════════════════════════════════════════════════════

@Composable
private fun AuditFab(onClick: () -> Unit) {
    MiniPosGradientButton(
        text = stringResource(R.string.stock_mgmt_create_audit),
        onClick = onClick,
        icon = Icons.Default.Inventory2,
        height = 52.dp,
    )
}
