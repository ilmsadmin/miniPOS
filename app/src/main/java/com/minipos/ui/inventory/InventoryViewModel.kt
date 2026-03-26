package com.minipos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.model.Product
import com.minipos.domain.model.InventoryItem
import com.minipos.domain.model.StockMovementType
import com.minipos.domain.model.StockOverviewItem
import com.minipos.domain.model.StockHistoryItem
import com.minipos.domain.model.StockSummary
import com.minipos.domain.model.Supplier
import com.minipos.domain.model.Result
import com.minipos.domain.repository.InventoryRepository
import com.minipos.domain.repository.ProductRepository
import com.minipos.domain.repository.StoreRepository
import com.minipos.domain.repository.SupplierRepository
import com.minipos.data.preferences.AppPreferences
import com.minipos.core.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class InventoryTab { OVERVIEW, STOCK_CHECK, HISTORY }

data class ProductStock(
    val product: Product,
    val currentStock: Double = 0.0,
)

data class InventoryState(
    // Common
    val isLoading: Boolean = true,
    val selectedTab: InventoryTab = InventoryTab.OVERVIEW,

    // Overview tab
    val summary: StockSummary = StockSummary(),
    val overviewItems: List<StockOverviewItem> = emptyList(),

    // Stock Check tab
    val stockCheckItems: List<ProductStock> = emptyList(),
    val stockCheckSearch: String = "",
    val suppliers: List<Supplier> = emptyList(),
    val showAdjustDialog: Boolean = false,
    val selectedProduct: Product? = null,
    val adjustError: String? = null,

    // History tab
    val historyItems: List<StockHistoryItem> = emptyList(),
    val historyLoading: Boolean = false,
    val historyFilterType: String = "all", // "all", "in", "out"
    val historyStartTime: Long = 0L,
    val historyEndTime: Long = 0L,
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    private val supplierRepository: SupplierRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state

    private var storeId: String = ""

    init {
        // Default history range: last 30 days
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -30)
        }
        _state.update { it.copy(historyStartTime = DateUtils.startOfDay(cal.timeInMillis), historyEndTime = DateUtils.endOfDay(now)) }
        loadData()
    }

    fun selectTab(tab: InventoryTab) {
        _state.update { it.copy(selectedTab = tab) }
        when (tab) {
            InventoryTab.OVERVIEW -> loadOverview()
            InventoryTab.STOCK_CHECK -> loadStockCheck()
            InventoryTab.HISTORY -> loadHistory()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            loadOverview()
            // Pre-load stock check + suppliers
            loadStockCheck()
        }
    }

    private fun loadOverview() {
        viewModelScope.launch {
            if (storeId.isEmpty()) return@launch
            _state.update { it.copy(isLoading = true) }
            try {
                val overview = inventoryRepository.getAllStockOverview(storeId)
                val s = _state.value
                val summary = inventoryRepository.getStockSummary(storeId, s.historyStartTime, s.historyEndTime)
                _state.update { it.copy(overviewItems = overview, summary = summary, isLoading = false) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadStockCheck() {
        viewModelScope.launch {
            if (storeId.isEmpty()) return@launch
            val products = productRepository.getAll(storeId).filter { it.trackInventory }
            val items = products.map { product ->
                val stock = inventoryRepository.getStock(storeId, product.id)
                ProductStock(product = product, currentStock = stock?.quantity ?: 0.0)
            }
            val suppliers = supplierRepository.getAll(storeId)
            _state.update { it.copy(stockCheckItems = items, suppliers = suppliers, isLoading = false) }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            if (storeId.isEmpty()) return@launch
            _state.update { it.copy(historyLoading = true) }
            try {
                val s = _state.value
                val items = inventoryRepository.getStockHistory(storeId, s.historyStartTime, s.historyEndTime)
                _state.update { it.copy(historyItems = items, historyLoading = false) }
            } catch (_: Exception) {
                _state.update { it.copy(historyLoading = false) }
            }
        }
    }

    fun setHistoryDateRange(startTime: Long, endTime: Long) {
        _state.update { it.copy(historyStartTime = startTime, historyEndTime = endTime) }
        loadHistory()
        // Also refresh summary with new range
        viewModelScope.launch {
            if (storeId.isEmpty()) return@launch
            try {
                val summary = inventoryRepository.getStockSummary(storeId, startTime, endTime)
                _state.update { it.copy(summary = summary) }
            } catch (_: Exception) {}
        }
    }

    fun setHistoryFilterType(filterType: String) {
        _state.update { it.copy(historyFilterType = filterType) }
    }

    fun updateStockCheckSearch(query: String) {
        _state.update { it.copy(stockCheckSearch = query) }
    }

    fun showAdjustDialog(product: Product) {
        _state.update { it.copy(showAdjustDialog = true, selectedProduct = product, adjustError = null) }
    }

    fun dismissAdjustDialog() {
        _state.update { it.copy(showAdjustDialog = false, selectedProduct = null) }
    }

    fun adjustStock(amount: Double, type: StockMovementType, supplierId: String? = null) {
        val product = _state.value.selectedProduct ?: return
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first() ?: return@launch
            val result = inventoryRepository.adjustStock(
                storeId = storeId,
                productId = product.id,
                amount = amount,
                type = type,
                userId = userId,
                referenceId = null,
                supplierId = supplierId,
            )
            when (result) {
                is Result.Success -> {
                    dismissAdjustDialog()
                    loadStockCheck()
                    loadOverview()
                }
                is Result.Error -> {
                    _state.update { it.copy(adjustError = result.message) }
                }
            }
        }
    }

    val filteredStockCheckItems: List<ProductStock>
        get() {
            val search = _state.value.stockCheckSearch.trim().lowercase()
            if (search.isEmpty()) return _state.value.stockCheckItems
            return _state.value.stockCheckItems.filter {
                it.product.name.lowercase().contains(search) ||
                    it.product.sku.lowercase().contains(search) ||
                    (it.product.barcode?.lowercase()?.contains(search) == true)
            }
        }

    val filteredHistoryItems: List<StockHistoryItem>
        get() {
            val filterType = _state.value.historyFilterType
            if (filterType == "all") return _state.value.historyItems
            return _state.value.historyItems.filter { item ->
                when (filterType) {
                    "in" -> item.type in listOf(
                        StockMovementType.PURCHASE_IN, StockMovementType.RETURN_IN, StockMovementType.ADJUSTMENT_IN
                    )
                    "out" -> item.type in listOf(
                        StockMovementType.SALE_OUT, StockMovementType.RETURN_OUT,
                        StockMovementType.ADJUSTMENT_OUT, StockMovementType.DAMAGE_OUT
                    )
                    else -> true
                }
            }
        }
}
