package com.minipos.ui.stockaudit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.utils.DateUtils
import com.minipos.core.utils.UuidGenerator
import com.minipos.domain.model.StockMovementType
import com.minipos.domain.model.StockOverviewItem
import com.minipos.domain.repository.AuthRepository
import com.minipos.domain.repository.InventoryRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════
// TAB TYPES — mirrors the 3 tabs in the HTML mock
// ═══════════════════════════════════════════════════════

enum class AuditTabType {
    STOCK_AUDIT,   // Kiểm kho
    ADD_STOCK,     // Nhập thêm
    REMOVE_STOCK,  // Xuất / Hủy
}

// ═══════════════════════════════════════════════════════
// DIFFERENCE REASONS
// ═══════════════════════════════════════════════════════

enum class DiffReason {
    LOSS,          // Hao hụt
    DAMAGED,       // Hàng hỏng
    THEFT,         // Mất mát
    NOT_UPDATED,   // Nhập chưa cập nhật
    MISCOUNT,      // Đếm sai lần trước
    OTHER,         // Khác
}

// ═══════════════════════════════════════════════════════
// AUDIT ITEM — each product row in the audit list
// ═══════════════════════════════════════════════════════

data class AuditItem(
    val productId: String,
    val productName: String,
    val productSku: String,
    val systemStock: Double,
    val actualQty: String = "",        // text field value
    val shelfLocation: String = "",
    val diffReason: DiffReason = DiffReason.OTHER,
) {
    val actualQtyDouble: Double
        get() = actualQty.toDoubleOrNull() ?: 0.0

    val difference: Double
        get() = actualQtyDouble - systemStock

    val isMatch: Boolean
        get() = actualQty.isNotEmpty() && difference == 0.0
}

// ═══════════════════════════════════════════════════════
// SCREEN STATE
// ═══════════════════════════════════════════════════════

data class StockAuditState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val activeTab: AuditTabType = AuditTabType.STOCK_AUDIT,
    val sessionCode: String = "",
    val auditDate: String = "",
    val reason: String = "",
    val notes: String = "",
    val auditItems: List<AuditItem> = emptyList(),
    val searchQuery: String = "",
    val allProducts: List<StockOverviewItem> = emptyList(),
    val showProductSearch: Boolean = false,
    val toastMessage: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class StockAuditViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val inventoryRepository: InventoryRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StockAuditState())
    val state: StateFlow<StockAuditState> = _state

    private var storeId: String = ""
    private var userId: String = ""

    init {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val session = authRepository.getCurrentSession()
            userId = session?.userId ?: ""

            // Generate session code
            val code = "KK-${String.format("%04d", (1..9999).random())}"

            _state.update {
                it.copy(
                    sessionCode = code,
                    auditDate = DateUtils.formatDate(DateUtils.now()),
                    isLoading = false,
                )
            }

            // Load all stock products for search
            try {
                val products = inventoryRepository.getAllStockOverview(storeId)
                _state.update { it.copy(allProducts = products) }
            } catch (_: Exception) { }
        }
    }

    // ── Tab selection ──

    fun selectTab(tab: AuditTabType) {
        _state.update { it.copy(activeTab = tab) }
    }

    // ── Form fields ──

    fun updateReason(value: String) {
        _state.update { it.copy(reason = value) }
    }

    fun updateNotes(value: String) {
        _state.update { it.copy(notes = value) }
    }

    fun updateSearchQuery(value: String) {
        _state.update { it.copy(searchQuery = value) }
    }

    // ── Product search results (filtered) ──

    val searchResults: List<StockOverviewItem>
        get() {
            val s = _state.value
            if (s.searchQuery.length < 1) return emptyList()
            val q = s.searchQuery.trim().lowercase()
            val addedIds = s.auditItems.map { it.productId }.toSet()
            return s.allProducts
                .filter { it.productId !in addedIds }
                .filter {
                    it.productName.lowercase().contains(q) ||
                        it.productSku.lowercase().contains(q)
                }
                .take(10)
        }

    // ── Add / remove product ──

    fun addProduct(product: StockOverviewItem) {
        val current = _state.value
        if (current.auditItems.any { it.productId == product.productId }) {
            showToast(_state.value.toastMessage ?: "Product already in list")
            return
        }
        val item = AuditItem(
            productId = product.productId,
            productName = product.productName,
            productSku = product.productSku,
            systemStock = product.currentStock,
            actualQty = when (current.activeTab) {
                AuditTabType.STOCK_AUDIT -> product.currentStock.toLong().toString()
                else -> ""
            },
        )
        _state.update {
            it.copy(
                auditItems = it.auditItems + item,
                searchQuery = "",
            )
        }
    }

    fun removeProduct(productId: String) {
        _state.update {
            it.copy(auditItems = it.auditItems.filter { item -> item.productId != productId })
        }
    }

    // ── Update audit item fields ──

    fun updateActualQty(productId: String, value: String) {
        _state.update {
            it.copy(auditItems = it.auditItems.map { item ->
                if (item.productId == productId) item.copy(actualQty = value) else item
            })
        }
    }

    fun updateShelfLocation(productId: String, value: String) {
        _state.update {
            it.copy(auditItems = it.auditItems.map { item ->
                if (item.productId == productId) item.copy(shelfLocation = value) else item
            })
        }
    }

    fun updateDiffReason(productId: String, reason: DiffReason) {
        _state.update {
            it.copy(auditItems = it.auditItems.map { item ->
                if (item.productId == productId) item.copy(diffReason = reason) else item
            })
        }
    }

    // ── Summary calculations ──

    val matchCount: Int get() = _state.value.auditItems.count { it.isMatch }
    val shortageCount: Int get() = _state.value.auditItems.count { it.actualQty.isNotEmpty() && it.difference < 0 }
    val surplusCount: Int get() = _state.value.auditItems.count { it.actualQty.isNotEmpty() && it.difference > 0 }
    val totalShortage: Double get() = _state.value.auditItems.filter { it.difference < 0 }.sumOf { it.difference }
    val totalSurplus: Double get() = _state.value.auditItems.filter { it.difference > 0 }.sumOf { it.difference }
    val netDifference: Double get() = _state.value.auditItems.sumOf { it.difference }

    // ── Toast ──

    fun showToast(message: String) {
        _state.update { it.copy(toastMessage = message) }
    }

    fun dismissToast() {
        _state.update { it.copy(toastMessage = null) }
    }

    // ── Confirm / Save ──

    fun confirmAudit() {
        val s = _state.value
        if (s.auditItems.isEmpty()) return
        if (s.reason.isBlank()) return

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val referenceId = UuidGenerator.generate()
                val now = DateUtils.now()

                for (item in s.auditItems) {
                    val diff = item.difference
                    if (diff == 0.0) continue

                    val type = when (s.activeTab) {
                        AuditTabType.STOCK_AUDIT -> {
                            if (diff > 0) StockMovementType.ADJUSTMENT_IN else StockMovementType.ADJUSTMENT_OUT
                        }
                        AuditTabType.ADD_STOCK -> StockMovementType.ADJUSTMENT_IN
                        AuditTabType.REMOVE_STOCK -> StockMovementType.ADJUSTMENT_OUT
                    }

                    val amount = when (s.activeTab) {
                        AuditTabType.STOCK_AUDIT -> diff // positive or negative
                        AuditTabType.ADD_STOCK -> item.actualQtyDouble  // always positive
                        AuditTabType.REMOVE_STOCK -> -item.actualQtyDouble // always negative
                    }

                    if (amount != 0.0) {
                        inventoryRepository.adjustStock(
                            storeId = storeId,
                            productId = item.productId,
                            amount = amount,
                            type = type,
                            userId = userId,
                            referenceId = referenceId,
                        )
                    }
                }

                _state.update {
                    it.copy(isSaving = false, saved = true)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
