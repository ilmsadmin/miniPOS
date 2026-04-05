package com.minipos.ui.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils
import com.minipos.domain.model.StockHistoryItem
import com.minipos.ui.components.*

// ═══════════════════════════════════════════════════════
// INVENTORY HUB SCREEN
// Based on inventory.html mock design
// ═══════════════════════════════════════════════════════

@Composable
fun InventoryHubScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: InventoryHubViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
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
                title = stringResource(R.string.inv_hub_title),
                onBack = onBack,
            )

            // Always render content layout to avoid layout shift.
            // Show a subtle linear progress on top while loading for the first time.
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // ─── Quick Actions Grid (2×2) — always visible ───
                    QuickActionsGrid(onNavigate = onNavigate)

                    Spacer(modifier = Modifier.height(20.dp))

                    // ─── Stock Overview Stats ───
                    SectionTitle(
                        title = stringResource(R.string.inv_hub_overview),
                        icon = Icons.Rounded.Insights,
                    )
                    StockOverviewStats(state = state)

                    Spacer(modifier = Modifier.height(20.dp))

                    // ─── Recent Purchases ───
                    SectionTitle(
                        title = stringResource(R.string.inv_hub_recent_purchase),
                        icon = Icons.Rounded.LocalShipping,
                    )
                    RecentPurchaseList(items = state.recentPurchases)

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Thin loading indicator at the top — only on first load
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.isLoading && !state.hasLoadedOnce,
                    enter = fadeIn(animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(300)),
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppColors.Primary,
                        trackColor = AppColors.Primary.copy(alpha = 0.12f),
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// QUICK ACTIONS GRID
// ═══════════════════════════════════════════════════════

private data class QuickAction(
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int,
    val gradientColors: List<Color>,
    val route: String,
)

@Composable
private fun QuickActionsGrid(onNavigate: (String) -> Unit) {
    val actions = listOf(
        QuickAction(
            icon = Icons.Rounded.Inventory,
            titleRes = R.string.inv_hub_purchase_in,
            descRes = R.string.inv_hub_purchase_in_desc,
            gradientColors = listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE)),
            route = com.minipos.ui.navigation.Screen.PurchaseOrder.route,
        ),
        QuickAction(
            icon = Icons.Rounded.History,
            titleRes = R.string.inv_hub_order_history,
            descRes = R.string.inv_hub_order_history_desc,
            gradientColors = listOf(Color(0xFF00D2FF), Color(0xFF3B9FDB)),
            route = com.minipos.ui.navigation.Screen.OrderList.route,
        ),
        QuickAction(
            icon = Icons.Rounded.Warehouse,
            titleRes = R.string.inv_hub_stock_mgmt,
            descRes = R.string.inv_hub_stock_mgmt_desc,
            gradientColors = listOf(Color(0xFFFF8A65), Color(0xFFFF5252)),
            route = com.minipos.ui.navigation.Screen.StockManagement.route,
        ),
        QuickAction(
            icon = Icons.Rounded.QrCode2,
            titleRes = R.string.inv_hub_barcode,
            descRes = R.string.inv_hub_barcode_desc,
            gradientColors = listOf(Color(0xFFFFD54F), Color(0xFFF9A825)),
            route = com.minipos.ui.navigation.Screen.BarcodeManagement.route,
        ),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (col in 0..1) {
                    val idx = row * 2 + col
                    QuickActionCard(
                        action = actions[idx],
                        onClick = { onNavigate(actions[idx].route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    action: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                .background(Brush.linearGradient(action.gradientColors)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                action.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(action.titleRes),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
            Text(
                text = stringResource(action.descRes),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextTertiary,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// STOCK OVERVIEW STATS
// ═══════════════════════════════════════════════════════

@Composable
private fun StockOverviewStats(state: InventoryHubState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Low stock
        StatCard(
            icon = Icons.Rounded.Warning,
            iconColor = AppColors.Warning,
            value = state.summary.lowStockCount.toString(),
            label = stringResource(R.string.inv_hub_low_stock),
            modifier = Modifier.weight(1f),
        )
        // Out of stock
        StatCard(
            icon = Icons.Rounded.Error,
            iconColor = AppColors.Error,
            value = state.summary.outOfStockCount.toString(),
            label = stringResource(R.string.inv_hub_out_of_stock),
            modifier = Modifier.weight(1f),
        )
        // Stock value
        StatCard(
            icon = Icons.Rounded.AccountBalanceWallet,
            iconColor = AppColors.Accent,
            value = CurrencyFormatter.formatCompact(state.summary.totalStockValue),
            label = stringResource(R.string.inv_hub_stock_value),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = AppColors.TextPrimary,
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextTertiary,
            maxLines = 1,
        )
    }
}

// ═══════════════════════════════════════════════════════
// RECENT PURCHASE LIST
// ═══════════════════════════════════════════════════════

@Composable
private fun RecentPurchaseList(items: List<StockHistoryItem>) {
    if (items.isEmpty()) {
        // Empty state
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                .background(AppColors.Surface)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Rounded.Inventory2,
                contentDescription = null,
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.inv_hub_no_recent),
                fontSize = 13.sp,
                color = AppColors.TextSecondary,
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                RecentPurchaseItem(item = item)
            }
        }
    }
}

@Composable
private fun RecentPurchaseItem(item: StockHistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                .background(AppColors.SurfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Inventory2,
                contentDescription = null,
                tint = AppColors.PrimaryLight,
                modifier = Modifier.size(22.dp),
            )
        }

        // Info
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
                text = buildString {
                    if (!item.supplierName.isNullOrEmpty()) {
                        append(stringResource(R.string.inv_hub_supplier_prefix, item.supplierName))
                        append(" — ")
                    }
                    append(DateUtils.formatDate(item.createdAt))
                },
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Amount
        val totalValue = (item.unitCost ?: 0.0) * item.quantity
        if (totalValue > 0) {
            Text(
                text = CurrencyFormatter.formatCompact(totalValue),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.Accent,
            )
        } else {
            Text(
                text = "+${item.quantity.toInt()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.Accent,
            )
        }

        // Arrow
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}
