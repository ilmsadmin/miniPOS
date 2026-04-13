package com.minipos.ui.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils
import com.minipos.ui.components.*

@Composable
fun ReportScreen(
    onBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }

    // Info dialog for generic messages
    if (infoDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null; infoDialogMessage = null },
            title = { Text(infoDialogTitle ?: "") },
            text = { Text(infoDialogMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { infoDialogTitle = null; infoDialogMessage = null }) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

    // Detail report dialogs
    when (state.activeDetailReport) {
        DetailReportType.INVENTORY -> InventoryReportDialog(
            data = state.inventoryReport,
            isLoading = state.isDetailLoading,
            onDismiss = { viewModel.closeDetailReport() },
        )
        DetailReportType.DEBT -> DebtReportDialog(
            data = state.debtReport,
            isLoading = state.isDetailLoading,
            onDismiss = { viewModel.closeDetailReport() },
        )
        DetailReportType.PROFIT -> ProfitReportDialog(
            data = state.profitReport,
            isLoading = state.isDetailLoading,
            onDismiss = { viewModel.closeDetailReport() },
        )
        DetailReportType.NONE -> {}
    }

    // Export message snackbar
    LaunchedEffect(state.exportMessage) {
        state.exportMessage?.let { msg ->
            val text = when (msg) {
                "success" -> context.getString(R.string.report_export_success)
                else -> context.getString(R.string.report_export_error)
            }
            snackbarHostState.showSnackbar(text)
            viewModel.clearExportMessage()
        }
    }

    val periodLabels = listOf(
        stringResource(R.string.report_today),
        stringResource(R.string.report_this_week),
        stringResource(R.string.report_this_month),
        stringResource(R.string.report_custom),
    )

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                // Top bar
                MiniPosTopBar(
                    title = stringResource(R.string.reports_title),
                    onBack = onBack,
                )

                // Period filter chips
                LazyRow(
                    modifier = Modifier.padding(bottom = 16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(periodLabels.size) { index ->
                        if (index < 3) {
                            MiniPosFilterChip(
                                label = periodLabels[index],
                                selected = state.selectedTab == index,
                                onClick = { viewModel.selectTab(index) },
                            )
                        } else {
                            // "Custom" chip with lock icon to indicate coming soon
                            MiniPosFilterChip(
                                label = periodLabels[index],
                                selected = false,
                                icon = Icons.Rounded.Lock,
                                onClick = {
                                    infoDialogTitle = periodLabels[index]
                                    infoDialogMessage = context.getString(R.string.feature_coming_soon_msg)
                                },
                            )
                        }
                    }
                }

                // Scrollable body
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 32.dp,
                    ),
                ) {
                    // ─── KPI Grid (2x2) ───
                    item {
                        val (revenue, orders) = when (state.selectedTab) {
                            0 -> state.todayRevenue to state.todayOrders
                            1 -> state.weekRevenue to state.weekOrders
                            else -> state.monthRevenue to state.monthOrders
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            KpiCard(
                                value = CurrencyFormatter.formatCompact(revenue),
                                label = stringResource(R.string.report_revenue),
                                icon = Icons.Rounded.Payments,
                                iconColor = AppColors.Success,
                                trend = state.revenueTrend?.let { "${if (it >= 0) "+" else ""}${it.toInt()}%" },
                                trendUp = (state.revenueTrend ?: 0.0) >= 0,
                                modifier = Modifier.weight(1f),
                            )
                            KpiCard(
                                value = orders.toString(),
                                label = stringResource(R.string.report_orders),
                                icon = Icons.Rounded.Receipt,
                                iconColor = AppColors.Accent,
                                trend = state.ordersTrend?.let { "${if (it >= 0) "+" else ""}$it" },
                                trendUp = (state.ordersTrend ?: 0) >= 0,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            KpiCard(
                                value = state.productsSold.toString(),
                                label = stringResource(R.string.report_products_sold),
                                icon = Icons.Rounded.Sell,
                                iconColor = AppColors.PrimaryLight,
                                modifier = Modifier.weight(1f),
                            )
                            KpiCard(
                                value = state.customerCount.toString(),
                                label = stringResource(R.string.report_customers),
                                icon = Icons.Rounded.Group,
                                iconColor = AppColors.Warning,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    item { Spacer(Modifier.height(20.dp)) }

                    // ─── Hourly Revenue Chart ───
                    if (state.selectedTab == 0 && state.hourlyRevenue.any { it.amount > 0 }) {
                        item {
                            SectionTitle(
                                title = stringResource(R.string.report_revenue_by_hour),
                                icon = Icons.Rounded.ShowChart,
                            )
                        }
                        item {
                            HourlyRevenueChart(
                                data = state.hourlyRevenue,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }

                    // ─── Top Products ───
                    if (state.topProducts.isNotEmpty()) {
                        item {
                            SectionTitle(
                                title = stringResource(R.string.report_top_products),
                                icon = Icons.Rounded.EmojiEvents,
                            )
                        }
                        itemsIndexed(state.topProducts) { index, product ->
                            TopProductItem(
                                rank = index + 1,
                                name = product.name,
                                quantity = product.quantity,
                                amount = CurrencyFormatter.formatCompact(product.totalAmount),
                            )
                            if (index < state.topProducts.lastIndex) {
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }

                    // ─── Recent Orders (today only) ───
                    if (state.selectedTab == 0 && state.recentOrders.isNotEmpty()) {
                        item {
                            SectionTitle(
                                title = stringResource(R.string.report_today_orders),
                                icon = Icons.Rounded.Schedule,
                            )
                        }
                        items(state.recentOrders) { order ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                                    .background(AppColors.Surface)
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        order.orderCode,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = AppColors.TextPrimary,
                                    )
                                    Text(
                                        DateUtils.formatTime(order.createdAt),
                                        fontSize = 12.sp,
                                        color = AppColors.TextTertiary,
                                    )
                                }
                                Text(
                                    CurrencyFormatter.format(order.totalAmount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = AppColors.Primary,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }

                    // ─── Detail Reports ───
                    item {
                        SectionTitle(
                            title = stringResource(R.string.report_detail_reports),
                            icon = Icons.Rounded.Description,
                        )
                    }
                    item {
                        DetailReportItem(
                            icon = Icons.Rounded.Inventory,
                            label = stringResource(R.string.report_inventory),
                            onClick = { viewModel.openDetailReport(DetailReportType.INVENTORY) },
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                    item {
                        DetailReportItem(
                            icon = Icons.Rounded.CreditScore,
                            label = stringResource(R.string.report_debt),
                            onClick = { viewModel.openDetailReport(DetailReportType.DEBT) },
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                    item {
                        DetailReportItem(
                            icon = Icons.Rounded.TrendingUp,
                            label = stringResource(R.string.report_profit),
                            onClick = { viewModel.openDetailReport(DetailReportType.PROFIT) },
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                    item {
                        DetailReportItem(
                            icon = Icons.Rounded.Download,
                            label = stringResource(R.string.report_export_excel),
                            onClick = { viewModel.exportSalesReport(context) },
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// KPI Card — matching mock's .kpi
// ═══════════════════════════════════════

@Composable
private fun KpiCard(
    value: String,
    label: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    trend: String? = null,
    trendUp: Boolean = true,
) {
    Column(
        modifier = modifier
            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            if (trend != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (trendUp) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                        null,
                        tint = if (trendUp) AppColors.Success else AppColors.Error,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        trend,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (trendUp) AppColors.Success else AppColors.Error,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = AppColors.TextPrimary,
            letterSpacing = (-0.3).sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextTertiary,
        )
    }
}

// ═══════════════════════════════════════
// Hourly Revenue Chart — matching mock's .chart-card
// ═══════════════════════════════════════

@Composable
private fun HourlyRevenueChart(
    data: List<HourlyRevenue>,
    modifier: Modifier = Modifier,
) {
    val maxVal = remember(data) { data.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0 }
    val primaryColor = AppColors.Primary
    val accentColor = AppColors.Accent
    val gridColor = AppColors.BorderLight.copy(alpha = 0.4f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        // ── Chart area: Canvas for grid + bars ──
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        ) {
            val chartH = size.height
            val chartW = size.width
            val barCount = data.size
            if (barCount == 0) return@Canvas

            val totalGap = (barCount - 1) * 3.dp.toPx()
            val barW = ((chartW - totalGap) / barCount).coerceAtLeast(4f)

            // Draw 4 horizontal grid lines
            val gridLines = 4
            repeat(gridLines) { i ->
                val y = chartH * i / (gridLines - 1)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(chartW, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // Draw bars
            data.forEachIndexed { index, item ->
                val pct = (item.amount / maxVal).toFloat().coerceIn(0f, 1f)
                val barH = (chartH * pct).coerceAtLeast(if (item.amount > 0) 6.dp.toPx() else 0f)
                val x = index * (barW + 3.dp.toPx())
                val top = chartH - barH

                val color = if (index % 2 == 0) primaryColor else accentColor
                drawRoundRect(
                    color = color.copy(alpha = if (item.amount > 0) 1f else 0.15f),
                    topLeft = Offset(x, top),
                    size = androidx.compose.ui.geometry.Size(barW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── X-axis labels ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Show only every other label to avoid crowding
            data.forEachIndexed { index, item ->
                if (index % 2 == 0) {
                    Text(
                        text = item.hour,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextTertiary,
                        modifier = Modifier.weight(2f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                } else {
                    Spacer(Modifier.weight(2f))
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// Top Product Item — matching mock's .top-item
// ═══════════════════════════════════════

private val RankGold = Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000)))
private val RankSilver = Brush.linearGradient(listOf(Color(0xFFB0BEC5), Color(0xFF78909C)))
private val RankBronze = Brush.linearGradient(listOf(Color(0xFFBCAAA4), Color(0xFF8D6E63)))

@Composable
private fun TopProductItem(
    rank: Int,
    name: String,
    quantity: Int,
    amount: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(12.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rank badge
        val rankBg = when (rank) {
            1 -> RankGold
            2 -> RankSilver
            3 -> RankBronze
            else -> Brush.linearGradient(listOf(AppColors.SurfaceElevated, AppColors.SurfaceElevated))
        }
        val rankTextColor = if (rank <= 3) Color.White else AppColors.TextTertiary

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(rankBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                rank.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = rankTextColor,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(R.string.report_times_sold, quantity),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextTertiary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            amount,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.Accent,
        )
    }
}

// ═══════════════════════════════════════
// Detail Report Item — matching mock's .report-item
// ═══════════════════════════════════════

@Composable
private fun DetailReportItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AppColors.PrimaryLight,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ═══════════════════════════════════════
// Inventory Report Dialog
// ═══════════════════════════════════════

@Composable
private fun InventoryReportDialog(
    data: InventoryReportData,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
                .background(AppColors.Background)
                .padding(20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Inventory, null, tint = AppColors.PrimaryLight, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.report_inventory),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = AppColors.TextPrimary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, null, tint = AppColors.TextTertiary)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            } else {
                // Summary KPIs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailKpi(
                        label = stringResource(R.string.report_inv_total_products),
                        value = data.summary.totalProducts.toString(),
                        color = AppColors.Primary,
                        modifier = Modifier.weight(1f),
                    )
                    DetailKpi(
                        label = stringResource(R.string.report_inv_stock_value),
                        value = CurrencyFormatter.formatCompact(data.summary.totalStockValue),
                        color = AppColors.Success,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailKpi(
                        label = stringResource(R.string.report_inv_low_stock),
                        value = data.summary.lowStockCount.toString(),
                        color = AppColors.Warning,
                        modifier = Modifier.weight(1f),
                    )
                    DetailKpi(
                        label = stringResource(R.string.report_inv_out_of_stock),
                        value = data.summary.outOfStockCount.toString(),
                        color = AppColors.Error,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailKpi(
                        label = stringResource(R.string.report_inv_stock_in),
                        value = data.summary.totalStockIn.toInt().toString(),
                        color = AppColors.Success,
                        modifier = Modifier.weight(1f),
                    )
                    DetailKpi(
                        label = stringResource(R.string.report_inv_stock_out),
                        value = data.summary.totalStockOut.toInt().toString(),
                        color = AppColors.Error,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Stock overview list
                if (data.stockOverview.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.report_inv_stock_overview),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextTertiary,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(data.stockOverview.take(20)) { item ->
                            val stockColor = when {
                                item.currentStock <= 0 -> AppColors.Error
                                item.currentStock <= item.minStock -> AppColors.Warning
                                else -> AppColors.TextPrimary
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                                    .background(AppColors.Surface)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.productName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        item.productSku,
                                        fontSize = 11.sp,
                                        color = AppColors.TextTertiary,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${item.currentStock.toInt()} ${item.productUnit}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = stockColor,
                                    )
                                    Text(
                                        CurrencyFormatter.formatCompact(item.stockValue),
                                        fontSize = 11.sp,
                                        color = AppColors.TextTertiary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// Debt Report Dialog
// ═══════════════════════════════════════

@Composable
private fun DebtReportDialog(
    data: DebtReportData,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
                .background(AppColors.Background)
                .padding(20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CreditScore, null, tint = AppColors.PrimaryLight, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.report_debt),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = AppColors.TextPrimary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, null, tint = AppColors.TextTertiary)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            } else {
                // Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailKpi(
                        label = stringResource(R.string.report_debt_total),
                        value = CurrencyFormatter.formatCompact(data.totalDebt),
                        color = AppColors.Error,
                        modifier = Modifier.weight(1f),
                    )
                    DetailKpi(
                        label = stringResource(R.string.report_debt_customer_count),
                        value = data.customersWithDebt.size.toString(),
                        color = AppColors.Warning,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (data.customersWithDebt.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                null,
                                tint = AppColors.Success,
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.report_debt_no_debt),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextTertiary,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(data.customersWithDebt) { customer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                                    .background(AppColors.Surface)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    // Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(AppColors.PrimaryLight.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            customer.initials,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AppColors.PrimaryLight,
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            customer.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        customer.phone?.let {
                                            Text(it, fontSize = 11.sp, color = AppColors.TextTertiary)
                                        }
                                    }
                                }
                                Text(
                                    CurrencyFormatter.format(customer.debtAmount),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.Error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// Profit Report Dialog
// ═══════════════════════════════════════

@Composable
private fun ProfitReportDialog(
    data: ProfitReportData,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
                .background(AppColors.Background)
                .padding(20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TrendingUp, null, tint = AppColors.PrimaryLight, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.report_profit),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = AppColors.TextPrimary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, null, tint = AppColors.TextTertiary)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            } else {
                // Summary KPIs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailKpi(
                        label = stringResource(R.string.report_profit_total_revenue),
                        value = CurrencyFormatter.formatCompact(data.totalRevenue),
                        color = AppColors.Primary,
                        modifier = Modifier.weight(1f),
                    )
                    DetailKpi(
                        label = stringResource(R.string.report_profit_total_cost),
                        value = CurrencyFormatter.formatCompact(data.totalCost),
                        color = AppColors.Warning,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailKpi(
                        label = stringResource(R.string.report_profit_gross_profit),
                        value = CurrencyFormatter.formatCompact(data.grossProfit),
                        color = if (data.grossProfit >= 0) AppColors.Success else AppColors.Error,
                        modifier = Modifier.weight(1f),
                    )
                    DetailKpi(
                        label = stringResource(R.string.report_profit_margin),
                        value = "${data.margin.toInt()}%",
                        color = if (data.margin >= 0) AppColors.Success else AppColors.Error,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Profit by product
                if (data.productProfits.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.report_profit_by_product),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextTertiary,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(data.productProfits.take(20)) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                                    .background(AppColors.Surface)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "×${item.quantitySold} · ${item.margin.toInt()}%",
                                        fontSize = 11.sp,
                                        color = AppColors.TextTertiary,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        CurrencyFormatter.formatCompact(item.profit),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (item.profit >= 0) AppColors.Success else AppColors.Error,
                                    )
                                    Text(
                                        CurrencyFormatter.formatCompact(item.revenue),
                                        fontSize = 11.sp,
                                        color = AppColors.TextTertiary,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.report_profit_no_data),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// Shared: Detail KPI Card
// ═══════════════════════════════════════

@Composable
private fun DetailKpi(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusMd))
            .padding(12.dp),
    ) {
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = (-0.3).sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}