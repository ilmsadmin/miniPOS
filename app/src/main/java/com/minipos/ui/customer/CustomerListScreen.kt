package com.minipos.ui.customer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.Customer
import com.minipos.ui.components.*

// ═══════════════════════════════════════
// AVATAR GRADIENT PALETTE (matches mock c1–c5)
// ═══════════════════════════════════════

private val AvatarGradients = listOf(
    listOf(Color(0xFF6C5CE7), Color(0xFFA78BFA)), // c1 — purple
    listOf(Color(0xFF00D2FF), Color(0xFF4BB8F0)), // c2 — cyan
    listOf(Color(0xFFFF8A65), Color(0xFFF44336)), // c3 — orange-red
    listOf(Color(0xFF81C784), Color(0xFF388E3C)), // c4 — green
    listOf(Color(0xFFFFD54F), Color(0xFFFFB300)), // c5 — amber
)

private fun avatarGradient(index: Int): Brush {
    val colors = AvatarGradients[index % AvatarGradients.size]
    return Brush.linearGradient(colors)
}

// ═══════════════════════════════════════
// CUSTOMER LIST SCREEN
// ═══════════════════════════════════════

@Composable
fun CustomerListScreen(
    onBack: () -> Unit,
    onNavigateToForm: (String?) -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: CustomerListViewModel = hiltViewModel(),
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
            // ── Top bar with add button (matches mock topbar) ──
            MiniPosTopBar(
                title = stringResource(R.string.customer_title),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { onNavigateToForm(null) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MiniPosGradients.primary()),
                    ) {
                        Icon(
                            Icons.Rounded.PersonAdd,
                            contentDescription = stringResource(R.string.add_customer_cd),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            } else if (state.customers.isEmpty() && state.searchQuery.isBlank()) {
                // ── Empty state ──
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(AppColors.SurfaceElevated),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.People, contentDescription = null, modifier = Modifier.size(40.dp), tint = AppColors.TextTertiary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.no_customers), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        MiniPosGradientButton(
                            text = stringResource(R.string.add_customer_btn),
                            onClick = { onNavigateToForm(null) },
                            modifier = Modifier.width(200.dp),
                            height = 44.dp,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // ── Stats Row ──
                    item {
                        CustomerStatsRow(stats = state.stats)
                    }

                    // ── Search Bar ──
                    item {
                        MiniPosSearchBar(
                            value = state.searchQuery,
                            onValueChange = { viewModel.search(it) },
                            placeholder = stringResource(R.string.search_customer_hint),
                        )
                    }

                    // ── Customer list ──
                    items(state.customers) { customer ->
                        CustomerItem(
                            customer = customer,
                            colorIndex = state.customers.indexOf(customer),
                            onClick = { onNavigateToDetail(customer.id) },
                            onDelete = { viewModel.delete(customer) },
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// STATS ROW (3 cards)
// ═══════════════════════════════════════

@Composable
private fun CustomerStatsRow(stats: CustomerStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatMiniCard(
            value = stats.totalCustomers.toString(),
            label = stringResource(R.string.customer_stat_total),
            modifier = Modifier.weight(1f),
        )
        StatMiniCard(
            value = stats.newThisMonth.toString(),
            label = stringResource(R.string.customer_stat_new_month),
            modifier = Modifier.weight(1f),
        )
        StatMiniCard(
            value = stats.withDebt.toString(),
            label = stringResource(R.string.customer_stat_with_debt),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatMiniCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .then(
                Modifier
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            style = androidx.compose.ui.text.TextStyle(
                brush = MiniPosGradients.primary(),
            ),
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

// ═══════════════════════════════════════
// CUSTOMER LIST ITEM
// ═══════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomerItem(
    customer: Customer,
    colorIndex: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_customer_title)) },
            text = { Text(stringResource(R.string.delete_confirm_msg, customer.name)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.delete), color = AppColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .background(AppColors.Surface)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteConfirm = true },
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Avatar with gradient (matches mock .cust-avatar) ──
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarGradient(colorIndex)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = customer.initials,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Body: name + phone ──
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = customer.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
            if (!customer.phone.isNullOrBlank()) {
                Text(
                    text = customer.phone,
                    fontSize = 12.sp,
                    color = AppColors.TextTertiary,
                )
            }
        }

        // ── Right side: total + orders/debt (matches mock .cust-right) ──
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Total spent (compact format like "2.5M")
            if (customer.totalSpent > 0) {
                Text(
                    text = CurrencyFormatter.formatCompact(customer.totalSpent),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.Accent,
                )
            }

            if (customer.hasDebt) {
                // Debt badge (matches mock .cust-debt.has)
                DebtBadge(
                    text = stringResource(R.string.customer_debt_format, CurrencyFormatter.formatCompact(customer.debtAmount)),
                    hasDebt = true,
                )
            } else if (customer.visitCount > 0) {
                // Order count
                Text(
                    text = stringResource(R.string.customer_orders_format, customer.visitCount),
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                )
            }
        }
    }
}

// ═══════════════════════════════════════
// DEBT BADGE
// ═══════════════════════════════════════

@Composable
private fun DebtBadge(
    text: String,
    hasDebt: Boolean,
) {
    val bgColor = if (hasDebt) AppColors.Error.copy(alpha = 0.12f) else AppColors.Success.copy(alpha = 0.12f)
    val textColor = if (hasDebt) AppColors.Error else AppColors.Success

    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = textColor,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(MiniPosTokens.RadiusFull))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

// CustomerFormDialog removed — now uses full-screen CustomerFormScreen via navigation
