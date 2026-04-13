package com.minipos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.ui.components.*
import com.minipos.ui.navigation.Screen

// ═══════════════════════════════════════
// DATA MODELS
// ═══════════════════════════════════════

private data class ManagementMenuItem(
    val icon: ImageVector,
    val titleRes: Int,
    val countFormat: Int,
    val route: String,
    val gradientColors: List<Color>,
)

private data class QuickStatItem(
    val icon: ImageVector,
    val labelRes: Int,
    val iconColor: @Composable () -> Color,
)

// ═══════════════════════════════════════
// SCREEN
// ═══════════════════════════════════════

@Composable
fun StoreManagementScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: StoreManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Menu items matching the HTML design exactly
    val menuItems = listOf(
        ManagementMenuItem(
            icon = Icons.Rounded.Category,
            titleRes = R.string.mgmt_categories,
            countFormat = R.string.mgmt_categories_count,
            route = Screen.CategoryList.route,
            gradientColors = listOf(Color(0xFFFF8A65), Color(0xFFFF5252)), // mi-cat
        ),
        ManagementMenuItem(
            icon = Icons.Rounded.Sell,
            titleRes = R.string.mgmt_products,
            countFormat = R.string.mgmt_products_count,
            route = Screen.ProductList.route,
            gradientColors = listOf(Color(0xFF0E9AA0), Color(0xFF2EC4B6)), // mi-prod
        ),
        ManagementMenuItem(
            icon = Icons.Rounded.Group,
            titleRes = R.string.mgmt_customers,
            countFormat = R.string.mgmt_customers_count,
            route = Screen.CustomerList.route,
            gradientColors = listOf(Color(0xFF14B8B0), Color(0xFF5AEDC5)), // mi-cust
        ),
        ManagementMenuItem(
            icon = Icons.Rounded.LocalShipping,
            titleRes = R.string.mgmt_suppliers,
            countFormat = R.string.mgmt_suppliers_count,
            route = Screen.SupplierList.route,
            gradientColors = listOf(Color(0xFFFFD54F), Color(0xFFF9A825)), // mi-supp
        ),
    )

    // Quick stat items matching the HTML design
    val quickStatItems = listOf(
        QuickStatItem(
            icon = Icons.Rounded.Inventory2,
            labelRes = R.string.mgmt_stat_total_products,
            iconColor = { AppColors.PrimaryLight },
        ),
        QuickStatItem(
            icon = Icons.Rounded.Group,
            labelRes = R.string.mgmt_stat_customers,
            iconColor = { AppColors.Accent },
        ),
        QuickStatItem(
            icon = Icons.Rounded.Category,
            labelRes = R.string.mgmt_stat_categories,
            iconColor = { AppColors.IconFood },
        ),
        QuickStatItem(
            icon = Icons.Rounded.LocalShipping,
            labelRes = R.string.mgmt_stat_suppliers,
            iconColor = { AppColors.Warning },
        ),
    )

    // Map stat values in order: products, customers, categories, suppliers
    val statValues = listOf(
        state.productCount,
        state.customerCount,
        state.categoryCount,
        state.supplierCount,
    )

    // Map count values for menu items: categories, products, customers, suppliers
    val menuCounts = listOf(
        state.categoryCount,
        state.productCount,
        state.customerCount,
        state.supplierCount,
    )

    Scaffold(
        containerColor = AppColors.Background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            MiniPosTopBar(
                title = stringResource(R.string.store_management_title),
                onBack = onBack,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                // ─── Menu List ───
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp),
                ) {
                    menuItems.forEachIndexed { index, item ->
                        ManagementMenuCard(
                            item = item,
                            count = menuCounts[index],
                            onClick = { onNavigate(item.route) },
                        )
                    }
                }

                // ─── Quick Stats Section ───
                SectionTitle(
                    title = stringResource(R.string.mgmt_quick_stats_title),
                    icon = Icons.Rounded.Insights,
                )

                // 2×2 Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    for (row in 0..1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (col in 0..1) {
                                val idx = row * 2 + col
                                QuickStatCard(
                                    icon = quickStatItems[idx].icon,
                                    value = statValues[idx],
                                    label = stringResource(quickStatItems[idx].labelRes),
                                    iconColor = quickStatItems[idx].iconColor(),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ═══════════════════════════════════════
// MANAGEMENT MENU CARD
// Matches .menu-item in the HTML design:
// 48dp gradient icon box, title (15sp bold),
// count subtitle (12sp tertiary), chevron arrow
// ═══════════════════════════════════════

@Composable
private fun ManagementMenuCard(
    item: ManagementMenuItem,
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Gradient icon container (48dp, r-lg = 16dp)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                .background(Brush.linearGradient(item.gradientColors)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title + count
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(item.titleRes),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(item.countFormat, count),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextTertiary,
            )
        }

        // Chevron arrow
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ═══════════════════════════════════════
// QUICK STAT CARD
// Matches .qs-card in the HTML design:
// Icon (24dp colored) + value (20sp, 900 weight) + label (10sp tertiary)
// ═══════════════════════════════════════

@Composable
private fun QuickStatCard(
    icon: ImageVector,
    value: Int,
    label: String,
    iconColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = value.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = AppColors.TextPrimary,
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextTertiary,
            )
        }
    }
}
