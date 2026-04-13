package com.minipos.ui.purchase

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils
import com.minipos.domain.model.PurchaseOrder
import com.minipos.domain.model.PurchaseOrderItem
import com.minipos.domain.repository.InventoryRepository
import com.minipos.ui.components.MiniPosTokens
import com.minipos.ui.components.MiniPosTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════
// STATE + VIEWMODEL
// ═══════════════════════════════════════════════════════

data class PurchaseOrderDetailState(
    val isLoading: Boolean = true,
    val order: PurchaseOrder? = null,
    val items: List<PurchaseOrderItem> = emptyList(),
)

@HiltViewModel
class PurchaseOrderDetailViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["id"])

    private val _state = MutableStateFlow(PurchaseOrderDetailState())
    val state: StateFlow<PurchaseOrderDetailState> = _state

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val order = inventoryRepository.getPurchaseOrderById(orderId)
                val items = inventoryRepository.getPurchaseOrderItems(orderId)
                _state.update { it.copy(isLoading = false, order = order, items = items) }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// SCREEN
// ═══════════════════════════════════════════════════════

@Composable
fun PurchaseOrderDetailScreen(
    onBack: () -> Unit,
    viewModel: PurchaseOrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(containerColor = AppColors.Background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            MiniPosTopBar(
                title = state.order?.code ?: stringResource(R.string.purchase_detail_title),
                onBack = onBack,
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            } else if (state.order == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.purchase_detail_not_found),
                        color = AppColors.TextSecondary,
                    )
                }
            } else {
                val order = state.order!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // ── Header card ──
                    item {
                        HeaderCard(order = order)
                    }

                    // ── Items section label ──
                    item {
                        Text(
                            text = stringResource(R.string.purchase_detail_items_section).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextTertiary,
                            letterSpacing = 0.5.sp,
                        )
                    }

                    // ── Line items ──
                    if (state.items.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                                    .background(AppColors.Surface)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    stringResource(R.string.purchase_detail_no_items),
                                    color = AppColors.TextSecondary,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    } else {
                        items(state.items) { item ->
                            PurchaseOrderLineItem(item = item)
                        }
                    }

                    // ── Totals card ──
                    item {
                        TotalsCard(order = order, itemCount = state.items.size)
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// HEADER CARD
// ═══════════════════════════════════════════════════════

@Composable
private fun HeaderCard(order: PurchaseOrder) {
    Surface(
        shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
        color = AppColors.Surface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeaderRow(
                icon = { Icon(Icons.Default.Tag, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(16.dp)) },
                label = stringResource(R.string.purchase_code_label),
                value = order.code,
                valueColor = AppColors.Primary,
                valueBold = true,
            )
            if (!order.supplierName.isNullOrEmpty()) {
                HorizontalDivider(color = AppColors.Border, thickness = 0.5.dp)
                HeaderRow(
                    icon = { Icon(Icons.Default.Business, contentDescription = null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
                    label = stringResource(R.string.purchase_supplier_label),
                    value = order.supplierName!!,
                )
            }
            HorizontalDivider(color = AppColors.Border, thickness = 0.5.dp)
            HeaderRow(
                icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
                label = stringResource(R.string.purchase_date_label),
                value = DateUtils.formatDateTime(order.createdAt),
            )
            if (!order.notes.isNullOrEmpty()) {
                HorizontalDivider(color = AppColors.Border, thickness = 0.5.dp)
                HeaderRow(
                    icon = { Icon(Icons.Default.Notes, contentDescription = null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
                    label = stringResource(R.string.purchase_notes_label),
                    value = order.notes!!,
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = AppColors.TextPrimary,
    valueBold: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppColors.TextSecondary,
            modifier = Modifier.width(90.dp),
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = valueColor,
            fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
    }
}

// ═══════════════════════════════════════════════════════
// LINE ITEM ROW
// ═══════════════════════════════════════════════════════

@Composable
private fun PurchaseOrderLineItem(item: PurchaseOrderItem) {
    Surface(
        shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
        color = AppColors.Surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Product icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusSm))
                    .background(AppColors.PrimaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            // Name + qty
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 2,
                )
                Text(
                    text = "× ${item.quantity.toLong()} · ${CurrencyFormatter.format(item.unitCost)} / ${stringResource(R.string.unit_each)}",
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                )
            }
            // Total
            Text(
                text = CurrencyFormatter.format(item.totalCost),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// TOTALS CARD
// ═══════════════════════════════════════════════════════

@Composable
private fun TotalsCard(order: PurchaseOrder, itemCount: Int) {
    Surface(
        shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
        color = AppColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.purchase_detail_total_products), fontSize = 13.sp, color = AppColors.TextSecondary)
                Text("$itemCount", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
            }
            HorizontalDivider(color = AppColors.Border, thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.purchase_detail_total_amount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Text(
                    CurrencyFormatter.format(order.totalAmount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.Accent,
                )
            }
        }
    }
}
