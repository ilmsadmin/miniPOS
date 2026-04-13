package com.minipos.ui.inventory

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils
import com.minipos.domain.model.StockMovementType
import com.minipos.domain.model.StockOverviewItem
import com.minipos.domain.model.StockHistoryItem
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

import com.minipos.ui.components.*

@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.showAdjustDialog && state.selectedProduct != null) {
        StockAdjustDialog(
            productName = state.selectedProduct!!.name,
            suppliers = state.suppliers,
            error = state.adjustError,
            onDismiss = { viewModel.dismissAdjustDialog() },
            onAdjust = { amount, type, supplierId -> viewModel.adjustStock(amount, type, supplierId) },
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
            MiniPosTopBar(
                title = stringResource(R.string.inventory_manage_title),
                onBack = onBack,
            )

            // Tab Row
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = AppColors.Surface,
                contentColor = AppColors.Primary,
            ) {
                Tab(
                    selected = state.selectedTab == InventoryTab.OVERVIEW,
                    onClick = { viewModel.selectTab(InventoryTab.OVERVIEW) },
                    text = { Text(stringResource(R.string.tab_overview)) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                Tab(
                    selected = state.selectedTab == InventoryTab.STOCK_CHECK,
                    onClick = { viewModel.selectTab(InventoryTab.STOCK_CHECK) },
                    text = { Text(stringResource(R.string.tab_stock_check)) },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                Tab(
                    selected = state.selectedTab == InventoryTab.HISTORY,
                    onClick = { viewModel.selectTab(InventoryTab.HISTORY) },
                    text = { Text(stringResource(R.string.tab_history)) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (state.selectedTab) {
                    InventoryTab.OVERVIEW -> OverviewTab(state = state)
                    InventoryTab.STOCK_CHECK -> StockCheckTab(state = state, viewModel = viewModel)
                    InventoryTab.HISTORY -> HistoryTab(state = state, viewModel = viewModel)
                }
            }
        }
    }
}

// ==================== OVERVIEW TAB ====================

@Composable
private fun OverviewTab(state: InventoryState) {
    val summary = state.summary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Summary cards grid
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard(
                    label = stringResource(R.string.stat_total_products),
                    value = summary.totalProducts.toString(),
                    icon = Icons.Default.Category,
                    color = AppColors.Primary,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label = stringResource(R.string.stat_low_stock),
                    value = summary.lowStockCount.toString(),
                    icon = Icons.Default.Warning,
                    color = AppColors.Warning,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label = stringResource(R.string.stat_out_of_stock),
                    value = summary.outOfStockCount.toString(),
                    icon = Icons.Default.RemoveShoppingCart,
                    color = AppColors.Error,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Stock value card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                    .background(AppColors.SecondaryContainer)
                    .padding(16.dp),
            ) {
                    Text(stringResource(R.string.inventory_value), style = MaterialTheme.typography.titleSmall, color = AppColors.SecondaryDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        CurrencyFormatter.format(summary.totalStockValue),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.SecondaryDark,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column {
                            Text(stringResource(R.string.stock_in_label), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            Text(
                                "+${summary.totalStockIn.toLong()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.Secondary,
                            )
                        }
                        Column {
                            Text(stringResource(R.string.stock_out_label), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            Text(
                                "-${summary.totalStockOut.toLong()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.Error,
                            )
                        }
                    }
            }
        }

        // Bar chart - Top 10 products by stock quantity
        if (state.overviewItems.isNotEmpty()) {
            item {
                StockBarChart(
                    title = stringResource(R.string.top_products_stock),
                    items = state.overviewItems.sortedByDescending { it.currentStock }.take(10),
                )
            }
        }

        // Low-stock products warning list
        val lowStockItems = state.overviewItems.filter { it.currentStock in 0.01..it.minStock.toDouble() }
        if (lowStockItems.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.low_stock_warning),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Warning,
                )
            }
            items(lowStockItems) { item ->
                OverviewProductCard(item = item, statusColor = AppColors.Warning)
            }
        }

        // Out-of-stock products
        val outOfStockItems = state.overviewItems.filter { it.currentStock <= 0 }
        if (outOfStockItems.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.out_of_stock_warning),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Error,
                )
            }
            items(outOfStockItems) { item ->
                OverviewProductCard(item = item, statusColor = AppColors.Error)
            }
        }

        // Spacer at bottom
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StockBarChart(title: String, items: List<StockOverviewItem>) {
    if (items.isEmpty()) return

    val chartEntryModelProducer = remember(items) {
        ChartEntryModelProducer(
            items.mapIndexed { index, item -> entryOf(index.toFloat(), item.currentStock.toFloat()) }
        )
    }
    val productNames = remember(items) { items.map { it.productName.take(10) } }

    val bottomAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        productNames.getOrElse(value.toInt()) { "" }
    }

    Card(
        shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Chart(
                chart = columnChart(
                    columns = listOf(
                        lineComponent(
                            color = AppColors.Primary,
                            thickness = 16.dp,
                            shape = Shapes.roundedCornerShape(topLeftPercent = 20, topRightPercent = 20),
                        )
                    ),
                ),
                chartModelProducer = chartEntryModelProducer,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = bottomAxisFormatter,
                    labelRotationDegrees = -45f,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        }
    }
}

@Composable
private fun OverviewProductCard(item: StockOverviewItem, statusColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .background(statusColor.copy(alpha = 0.06f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "SKU: ${item.productSku} · Min: ${item.minStock}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${item.currentStock.toLong()} ${item.productUnit}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = statusColor,
            )
            Text(
                CurrencyFormatter.formatCompact(item.stockValue),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
            )
        }
    }
}

// ==================== STOCK CHECK TAB ====================

@Composable
private fun StockCheckTab(state: InventoryState, viewModel: InventoryViewModel) {
    val filteredItems = viewModel.filteredStockCheckItems

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        MiniPosSearchBar(
            value = state.stockCheckSearch,
            onValueChange = { viewModel.updateStockCheckSearch(it) },
            placeholder = stringResource(R.string.search_stock),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // Quick stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val totalProducts = state.stockCheckItems.size
            val lowStock = state.stockCheckItems.count { it.currentStock > 0 && it.currentStock <= it.product.minStock }
            val outOfStock = state.stockCheckItems.count { it.currentStock <= 0 }

            MiniStatChip(label = stringResource(R.string.filter_all_stock), value = totalProducts.toString(), color = AppColors.Primary)
            MiniStatChip(label = stringResource(R.string.filter_low), value = lowStock.toString(), color = AppColors.Warning)
            MiniStatChip(label = stringResource(R.string.filter_out), value = outOfStock.toString(), color = AppColors.Error)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColors.TextTertiary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (state.stockCheckSearch.isNotEmpty()) stringResource(R.string.stock_check_not_found) else stringResource(R.string.stock_check_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextSecondary,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredItems) { item ->
                    StockCheckProductCard(item = item, onAdjust = { viewModel.showAdjustDialog(item.product) })
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun MiniStatChip(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun StockCheckProductCard(item: ProductStock, onAdjust: () -> Unit) {
    val isLow = item.currentStock > 0 && item.currentStock <= item.product.minStock
    val isOut = item.currentStock <= 0

    Card(
        shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOut -> AppColors.Error.copy(alpha = 0.06f)
                isLow -> AppColors.Warning.copy(alpha = 0.06f)
                else -> AppColors.Surface
            }
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "SKU: ${item.product.sku} · ${stringResource(R.string.unit)}: ${item.product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${item.currentStock.toLong()} ${item.product.unit}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isOut -> AppColors.Error
                            isLow -> AppColors.Warning
                            else -> AppColors.Secondary
                        },
                    )
                    Text(
                        "Min: ${item.product.minStock}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = onAdjust,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = AppColors.PrimaryContainer,
                        contentColor = AppColors.Primary,
                    ),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.adjust_cd), modifier = Modifier.size(18.dp))
                }
            }

            // Stock level progress bar
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (item.currentStock / (item.product.minStock * 3.0).coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    isOut -> AppColors.Error
                    isLow -> AppColors.Warning
                    else -> AppColors.Secondary
                },
                trackColor = AppColors.Border,
            )
        }
    }
}

// ==================== HISTORY TAB ====================

@Composable
private fun HistoryTab(state: InventoryState, viewModel: InventoryViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MiniPosFilterChip(
                label = stringResource(R.string.history_all),
                selected = state.historyFilterType == "all",
                onClick = { viewModel.setHistoryFilterType("all") },
            )
            MiniPosFilterChip(
                label = stringResource(R.string.history_stock_in),
                selected = state.historyFilterType == "in",
                onClick = { viewModel.setHistoryFilterType("in") },
            )
            MiniPosFilterChip(
                label = stringResource(R.string.history_stock_out),
                selected = state.historyFilterType == "out",
                onClick = { viewModel.setHistoryFilterType("out") },
            )
        }

        // Date range display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${DateUtils.formatDate(state.historyStartTime)} - ${DateUtils.formatDate(state.historyEndTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val filteredItems = viewModel.filteredHistoryItems

        if (state.historyLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColors.TextTertiary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.no_stock_history), style = MaterialTheme.typography.titleMedium, color = AppColors.TextSecondary)
                }
            }
        } else {
            var selectedHistoryItem by remember { mutableStateOf<StockHistoryItem?>(null) }

            // Detail bottom sheet
            if (selectedHistoryItem != null) {
                HistoryDetailDialog(
                    item = selectedHistoryItem!!,
                    onDismiss = { selectedHistoryItem = null },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filteredItems) { item ->
                    HistoryItemCard(
                        item = item,
                        onClick = { selectedHistoryItem = item },
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(item: StockHistoryItem, onClick: () -> Unit) {
    val isIncoming = item.type in listOf(
        StockMovementType.PURCHASE_IN, StockMovementType.RETURN_IN, StockMovementType.ADJUSTMENT_IN
    )
    val typeInfo = getMovementTypeInfoData(item.type)

    Card(
        shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Type icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(typeInfo.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(typeInfo.icon, contentDescription = null, tint = typeInfo.color, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(typeInfo.labelRes), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                if (!item.notes.isNullOrBlank()) {
                    Text(item.notes, style = MaterialTheme.typography.bodySmall, color = AppColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Quantity + time
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isIncoming) "+" else "-"}${item.quantity.toLong()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncoming) AppColors.Secondary else AppColors.Error,
                )
                Text(
                    "${item.quantityBefore.toLong()} → ${item.quantityAfter.toLong()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextTertiary,
                    fontSize = 11.sp,
                )
                Text(
                    DateUtils.formatDateTime(item.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextTertiary,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

private data class MovementTypeInfo(val icon: ImageVector, val color: Color, val labelRes: Int)

@Composable
private fun getMovementTypeInfoData(type: StockMovementType): MovementTypeInfo {
    return when (type) {
        StockMovementType.PURCHASE_IN -> MovementTypeInfo(Icons.Default.ShoppingCart, AppColors.Secondary, R.string.movement_purchase_in)
        StockMovementType.SALE_OUT -> MovementTypeInfo(Icons.Default.PointOfSale, AppColors.Error, R.string.movement_sale_out)
        StockMovementType.RETURN_IN -> MovementTypeInfo(Icons.AutoMirrored.Filled.AssignmentReturn, AppColors.Info, R.string.movement_return_in)
        StockMovementType.RETURN_OUT -> MovementTypeInfo(Icons.AutoMirrored.Filled.Undo, AppColors.Warning, R.string.movement_return_out)
        StockMovementType.ADJUSTMENT_IN -> MovementTypeInfo(Icons.Default.AddCircle, AppColors.Secondary, R.string.movement_adjustment_in)
        StockMovementType.ADJUSTMENT_OUT -> MovementTypeInfo(Icons.Default.RemoveCircle, AppColors.Error, R.string.movement_adjustment_out)
        StockMovementType.DAMAGE_OUT -> MovementTypeInfo(Icons.Default.BrokenImage, AppColors.Error, R.string.movement_damage_out)
        StockMovementType.TRANSFER -> MovementTypeInfo(Icons.Default.SwapHoriz, AppColors.Info, R.string.movement_transfer)
    }
}

@Composable
private fun HistoryDetailDialog(
    item: StockHistoryItem,
    onDismiss: () -> Unit,
) {
    val typeInfo = getMovementTypeInfoData(item.type)
    val isIncoming = item.type in listOf(
        StockMovementType.PURCHASE_IN, StockMovementType.RETURN_IN, StockMovementType.ADJUSTMENT_IN
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_btn)) }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(typeInfo.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(typeInfo.icon, contentDescription = null, tint = typeInfo.color, modifier = Modifier.size(24.dp))
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(typeInfo.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.quantity_unit_format, if (isIncoming) "+" else "-", item.quantity.toLong()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncoming) AppColors.Secondary else AppColors.Error,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                HorizontalDivider(color = AppColors.Divider)
                Spacer(modifier = Modifier.height(8.dp))

                // Product
                DetailRow(
                    icon = Icons.Default.Inventory2,
                    label = stringResource(R.string.detail_product),
                    value = item.productName,
                )

                // SKU
                DetailRow(
                    icon = Icons.Default.Tag,
                    label = "SKU",
                    value = item.productSku,
                )

                // Stock change
                DetailRow(
                    icon = Icons.Default.SwapVert,
                    label = stringResource(R.string.detail_stock_change),
                    value = "${item.quantityBefore.toLong()} → ${item.quantityAfter.toLong()}",
                )

                // Unit cost
                if (item.unitCost != null && item.unitCost > 0) {
                    DetailRow(
                        icon = Icons.Default.AttachMoney,
                        label = stringResource(R.string.detail_unit_cost),
                        value = CurrencyFormatter.format(item.unitCost),
                    )
                    DetailRow(
                        icon = Icons.Default.Calculate,
                        label = stringResource(R.string.detail_total_value),
                        value = CurrencyFormatter.format(item.unitCost * item.quantity),
                    )
                }

                // Supplier
                if (!item.supplierName.isNullOrBlank()) {
                    DetailRow(
                        icon = Icons.Default.LocalShipping,
                        label = stringResource(R.string.detail_supplier),
                        value = item.supplierName,
                    )
                }

                // Reference
                if (!item.referenceId.isNullOrBlank()) {
                    val refLabel = when (item.referenceType?.lowercase()) {
                        "order" -> stringResource(R.string.ref_order)
                        "purchase" -> stringResource(R.string.ref_purchase)
                        "return" -> stringResource(R.string.ref_return)
                        else -> stringResource(R.string.ref_other)
                    }
                    DetailRow(
                        icon = Icons.Default.Receipt,
                        label = refLabel,
                        value = item.referenceId,
                    )
                }

                // Notes
                if (!item.notes.isNullOrBlank()) {
                    DetailRow(
                        icon = Icons.AutoMirrored.Filled.Notes,
                        label = stringResource(R.string.detail_notes),
                        value = item.notes,
                    )
                }

                // Created by
                DetailRow(
                    icon = Icons.Default.Person,
                    label = stringResource(R.string.detail_performed_by),
                    value = item.createdBy,
                )

                // Timestamp
                DetailRow(
                    icon = Icons.Default.AccessTime,
                    label = stringResource(R.string.detail_time),
                    value = DateUtils.formatDateTime(item.createdAt),
                )

                // Movement ID
                DetailRow(
                    icon = Icons.Default.Fingerprint,
                    label = stringResource(R.string.detail_transaction_id),
                    value = item.id.take(12) + "...",
                )
            }
        },
    )
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = AppColors.TextTertiary,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextTertiary,
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary,
            )
        }
    }
}

// ==================== SHARED COMPONENTS ====================

@Composable
private fun SummaryCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockAdjustDialog(
    productName: String,
    suppliers: List<com.minipos.domain.model.Supplier>,
    error: String?,
    onDismiss: () -> Unit,
    onAdjust: (Double, StockMovementType, String?) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(StockMovementType.PURCHASE_IN) }
    var selectedSupplierId by remember { mutableStateOf<String?>(null) }

    val types = listOf(
        StockMovementType.PURCHASE_IN to stringResource(R.string.movement_purchase_in_label),
        StockMovementType.ADJUSTMENT_IN to stringResource(R.string.movement_adj_in_label),
        StockMovementType.ADJUSTMENT_OUT to stringResource(R.string.movement_adj_out_label),
        StockMovementType.DAMAGE_OUT to stringResource(R.string.movement_damage_label),
        StockMovementType.RETURN_IN to stringResource(R.string.movement_return_in_label),
    )

    val isPurchaseIn = selectedType == StockMovementType.PURCHASE_IN

    val supplierNone = stringResource(R.string.supplier_none)
    val supplierSelectLabel = stringResource(R.string.supplier_select_label)
    val supplierSelectTitle = stringResource(R.string.supplier_select)

    MiniPosBottomSheet(
        visible = true,
        title = stringResource(R.string.adjust_stock_title),
        onDismiss = onDismiss,
        footer = {
            BottomSheetPrimaryButton(
                text = stringResource(R.string.confirm_btn),
                icon = Icons.Filled.Check,
                onClick = {
                    val qty = amount.toDoubleOrNull()
                    if (qty != null && qty > 0) onAdjust(qty, selectedType, selectedSupplierId)
                },
            )
        },
    ) {
        Text(productName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
        Spacer(Modifier.height(12.dp))

        // Type selection
        types.forEach { (type, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedType == type,
                    onClick = {
                        selectedType = type
                        if (type != StockMovementType.PURCHASE_IN) {
                            selectedSupplierId = null
                        }
                    },
                )
                Text(label, fontSize = 14.sp, color = AppColors.TextPrimary)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Supplier selection - only for PURCHASE_IN
        if (isPurchaseIn) {
            val supplierItems = buildList {
                add(SelectListItem(id = "__none__", name = supplierNone, icon = Icons.Filled.Block, iconTint = AppColors.TextTertiary))
                suppliers.forEach { supplier ->
                    add(SelectListItem(id = supplier.id, name = supplier.name, icon = Icons.Filled.Business, iconTint = AppColors.Primary))
                }
            }
            MiniPosSelectBox(
                label = supplierSelectLabel,
                title = supplierSelectTitle,
                items = supplierItems,
                selectedId = selectedSupplierId ?: "__none__",
                placeholder = supplierNone,
                onSelect = { item ->
                    selectedSupplierId = if (item.id == "__none__") null else item.id
                },
            )
            Spacer(Modifier.height(12.dp))
        }

        BottomSheetField(
            label = stringResource(R.string.quantity_label),
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            placeholder = "0",
            keyboardType = KeyboardType.Number,
        )

        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error, color = AppColors.Error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
    }
}
