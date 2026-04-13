package com.minipos.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.minipos.domain.model.Order
import com.minipos.domain.model.OrderStatus
import com.minipos.domain.model.PaymentMethod
import com.minipos.ui.components.*

@Composable
fun OrderListScreen(
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit,
    viewModel: OrderListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = AppColors.Background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ─── Top Bar ───
            MiniPosTopBar(
                title = stringResource(R.string.order_history_title),
                onBack = onBack,
            )

            // ─── Search Bar ───
            MiniPosSearchBar(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = stringResource(R.string.search_order_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Period Chips ───
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val periods = listOf(
                    PeriodFilter.TODAY to R.string.period_today,
                    PeriodFilter.LAST_7_DAYS to R.string.period_7_days,
                    PeriodFilter.LAST_30_DAYS to R.string.period_30_days,
                    PeriodFilter.CUSTOM to R.string.period_custom,
                )
                items(periods) { (period, labelRes) ->
                    PeriodChip(
                        label = stringResource(labelRes),
                        selected = state.periodFilter == period,
                        onClick = { viewModel.setPeriodFilter(period) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Content ───
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            } else if (state.dayGroups.isEmpty()) {
                // Empty state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(AppColors.SurfaceElevated),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = AppColors.TextTertiary,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.order_no_results),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.order_search_empty_desc),
                            fontSize = 13.sp,
                            color = AppColors.TextTertiary,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    state.dayGroups.forEach { dayGroup ->
                        // Day header
                        item(key = "header_${dayGroup.dateLabel}") {
                            DayHeader(dayGroup = dayGroup)
                        }
                        // Orders in this day
                        items(dayGroup.orders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                onClick = { onOrderClick(order.id) },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        // Day total
                        item(key = "total_${dayGroup.dateLabel}") {
                            Text(
                                text = stringResource(
                                    R.string.day_total_format,
                                    CurrencyFormatter.format(dayGroup.dayTotal),
                                    dayGroup.orders.size,
                                ),
                                fontSize = 12.sp,
                                color = AppColors.TextTertiary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 4.dp, bottom = 20.dp, top = 4.dp),
                            )
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ─── Period Chip ───
@Composable
private fun PeriodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (selected) AppColors.Primary else Color.Transparent
    val textColor = if (selected) Color.White else AppColors.TextSecondary
    val borderColor = if (selected) AppColors.Primary else AppColors.BorderLight

    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
            .background(bgColor)
            .then(
                if (!selected) Modifier.background(Color.Transparent)
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ─── Day Header ───
@Composable
private fun DayHeader(dayGroup: DayGroup) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (dayGroup.isToday) Icons.Rounded.Today else Icons.Rounded.Event,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = AppColors.TextTertiary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (dayGroup.isToday) {
                stringResource(R.string.day_today_prefix, dayGroup.dateLabel)
            } else {
                dayGroup.dateLabel
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextTertiary,
        )
    }
}

// ─── Order Card ───
@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        // Top row: code + amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                order.orderCode,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
            )
            Text(
                CurrencyFormatter.format(order.totalAmount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = AppColors.Accent,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mid row: customer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Customer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.TextSecondary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = order.customerName ?: stringResource(R.string.label_guest),
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom row: meta + status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = DateUtils.formatTime(order.createdAt),
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
            OrderStatusBadge(order.status)
        }
    }
}

// ─── Status Badge ───
@Composable
private fun OrderStatusBadge(status: OrderStatus) {
    val (bgColor, textColor, textRes) = when (status) {
        OrderStatus.COMPLETED -> Triple(AppColors.SuccessSoft, AppColors.Success, R.string.status_completed)
        OrderStatus.REFUNDED -> Triple(AppColors.WarningSoft, AppColors.Warning, R.string.status_refunded)
        OrderStatus.PARTIALLY_REFUNDED -> Triple(AppColors.WarningSoft, AppColors.Warning, R.string.status_partially_refunded)
        OrderStatus.CANCELLED -> Triple(AppColors.ErrorContainer, AppColors.Error, R.string.status_cancelled)
    }
    Text(
        stringResource(textRes).uppercase(),
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(MiniPosTokens.RadiusFull))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = textColor,
    )
}
