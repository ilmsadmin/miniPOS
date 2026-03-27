package com.minipos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshDashboard()
    }

    Scaffold(
        containerColor = AppColors.Background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // ──── Header with gradient ────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(AppColors.Primary, Color(0xFF1E40AF)),
                        ),
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                state.storeName.ifEmpty { "miniPOS" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.home_greeting, state.userName),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Revenue summary cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Today revenue
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.15f),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        stringResource(R.string.today_revenue),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    CurrencyFormatter.format(state.todayRevenue),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }

                        // Today orders
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.15f),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        stringResource(R.string.home_orders_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.home_orders_count, state.todayOrders),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ──── Low stock alert ────
            if (state.lowStockCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable { onNavigate(Screen.InventoryOverview.route) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AppColors.Accent.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AppColors.AccentDark,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.home_low_stock_alert),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.AccentDark,
                            )
                            Text(
                                stringResource(R.string.home_low_stock_msg, state.lowStockCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary,
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AppColors.AccentDark,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ──── Quick Actions (2x2 grid) ────
            Text(
                stringResource(R.string.home_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HomeActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.QrCodeScanner,
                        title = stringResource(R.string.home_scan_barcode),
                        subtitle = stringResource(R.string.home_scan_and_sell),
                        gradientColors = listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)),
                        onClick = { onNavigate(Screen.ScanToPos.route) },
                    )
                    HomeActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ShoppingCart,
                        title = stringResource(R.string.home_pos),
                        subtitle = stringResource(R.string.home_create_order),
                        gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
                        onClick = { onNavigate(Screen.PosStep1.route) },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HomeActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Store,
                        title = stringResource(R.string.home_management),
                        subtitle = stringResource(R.string.home_your_store),
                        gradientColors = listOf(Color(0xFF059669), Color(0xFF047857)),
                        onClick = { onNavigate(Screen.StoreManagement.route) },
                    )
                    HomeActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.home_settings),
                        subtitle = stringResource(R.string.home_customize),
                        gradientColors = listOf(Color(0xFF64748B), Color(0xFF475569)),
                        onClick = { onNavigate(Screen.Settings.route) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ──── Upcoming Features Card ────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFDBEAFE),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    Icons.Default.NewReleases,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                stringResource(R.string.home_coming_soon),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary,
                            )
                            Text(
                                stringResource(R.string.home_new_features_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    UpcomingFeatureRow(
                        icon = Icons.Default.CloudUpload,
                        iconColor = Color(0xFF059669),
                        title = stringResource(R.string.home_gdrive_title),
                        description = stringResource(R.string.home_gdrive_desc),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    UpcomingFeatureRow(
                        icon = Icons.Default.AutoAwesome,
                        iconColor = Color(0xFF8B5CF6),
                        title = stringResource(R.string.home_ai_title),
                        description = stringResource(R.string.home_ai_desc),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    UpcomingFeatureRow(
                        icon = Icons.Default.SyncAlt,
                        iconColor = Color(0xFFE11D48),
                        title = stringResource(R.string.home_p2p_title),
                        description = stringResource(R.string.home_p2p_desc),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun UpcomingFeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
            )
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun HomeActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .aspectRatio(1.2f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors)),
        ) {
            // Decorative circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = (-20).dp, y = (-20).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            icon,
                            contentDescription = title,
                            modifier = Modifier.size(26.dp),
                            tint = Color.White,
                        )
                    }
                }

                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
