package com.minipos.ui.stockmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.model.StockOverviewItem
import com.minipos.domain.repository.InventoryRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StockFilter { ALL, IN_STOCK, LOW, OUT }
enum class StockSortMode { NAME_ASC, NAME_DESC, QTY_ASC, QTY_DESC }

data class StockManagementState(
    val isLoading: Boolean = true,
    val allItems: List<StockOverviewItem> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: StockFilter = StockFilter.ALL,
    val sortMode: StockSortMode = StockSortMode.NAME_ASC,
    val toastMessage: String? = null,
)

@HiltViewModel
class StockManagementViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val inventoryRepository: InventoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StockManagementState())
    val state: StateFlow<StockManagementState> = _state

    private var storeId: String = ""

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            _state.update { it.copy(isLoading = true) }
            try {
                val items = inventoryRepository.getAllStockOverview(storeId)
                _state.update { it.copy(allItems = items, isLoading = false) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refresh() {
        loadData()
    }

    fun updateSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setFilter(filter: StockFilter) {
        _state.update { it.copy(activeFilter = filter) }
    }

    fun toggleSort() {
        _state.update {
            val next = when (it.sortMode) {
                StockSortMode.NAME_ASC -> StockSortMode.NAME_DESC
                StockSortMode.NAME_DESC -> StockSortMode.QTY_ASC
                StockSortMode.QTY_ASC -> StockSortMode.QTY_DESC
                StockSortMode.QTY_DESC -> StockSortMode.NAME_ASC
            }
            it.copy(sortMode = next)
        }
    }

    fun showToast(message: String) {
        _state.update { it.copy(toastMessage = message) }
    }

    fun dismissToast() {
        _state.update { it.copy(toastMessage = null) }
    }

    // Computed filtered + sorted list
    val filteredItems: List<StockOverviewItem>
        get() {
            val s = _state.value
            var items = s.allItems

            // Search
            val query = s.searchQuery.trim().lowercase()
            if (query.isNotEmpty()) {
                items = items.filter {
                    it.productName.lowercase().contains(query) ||
                        it.productSku.lowercase().contains(query)
                }
            }

            // Filter
            items = when (s.activeFilter) {
                StockFilter.ALL -> items
                StockFilter.IN_STOCK -> items.filter { it.currentStock > it.minStock }
                StockFilter.LOW -> items.filter { it.currentStock in 1.0..it.minStock.toDouble() }
                StockFilter.OUT -> items.filter { it.currentStock <= 0 }
            }

            // Sort
            items = when (s.sortMode) {
                StockSortMode.NAME_ASC -> items.sortedBy { it.productName }
                StockSortMode.NAME_DESC -> items.sortedByDescending { it.productName }
                StockSortMode.QTY_ASC -> items.sortedBy { it.currentStock }
                StockSortMode.QTY_DESC -> items.sortedByDescending { it.currentStock }
            }

            return items
        }

    // Counts for chip badges
    val allCount: Int get() = _state.value.allItems.size
    val inStockCount: Int get() = _state.value.allItems.count { it.currentStock > it.minStock }
    val lowCount: Int get() = _state.value.allItems.count { it.currentStock in 1.0..it.minStock.toDouble() }
    val outCount: Int get() = _state.value.allItems.count { it.currentStock <= 0 }
}
