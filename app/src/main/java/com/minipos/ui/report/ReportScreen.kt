package com.minipos.ui.report

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        MiniPosFilterChip(
                            label = periodLabels[index],
                            selected = state.selectedTab == index,
                            onClick = {
                                if (index < 3) viewModel.selectTab(index)
                                // "Custom" chip: show toast/snackbar for now
                            },
                        )
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
                            onClick = { /* TODO */ },
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                    item {
                        DetailReportItem(
                            icon = Icons.Rounded.CreditScore,
                            label = stringResource(R.string.report_debt),
                            onClick = { /* TODO */ },
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                    item {
                        DetailReportItem(
                            icon = Icons.Rounded.TrendingUp,
                            label = stringResource(R.string.report_profit),
                            onClick = { /* TODO */ },
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                    item {
                        DetailReportItem(
                            icon = Icons.Rounded.Download,
                            label = stringResource(R.string.report_export_excel),
                            onClick = { /* TODO */ },
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
    val maxVal = data.maxOfOrNull { it.amount } ?: 1.0

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        ) {
            // Dashed lines
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (140.dp * i) / 3)
                        .height(1.dp)
                        .background(AppColors.BorderLight.copy(alpha = 0.3f)),
                )
            }

            // Bars
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                data.forEachIndexed { index, item ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val pct = if (maxVal > 0) (item.amount / maxVal).toFloat() else 0f
                        val barHeight = (128.dp * pct).coerceAtLeast(4.dp)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (index % 2 == 0) MiniPosGradients.primary()
                                    else MiniPosGradients.accent()
                                ),
                        )
                        Spacer(Modifier.height(4.dp))
                        // Show label every 2 hours to avoid crowding
                        if (index % 2 == 0) {
                            Text(
                                item.hour,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextTertiary,
                            )
                        }
                    }
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
