package com.minipos.ui.stockmanagement

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.model.Result
import com.minipos.domain.model.StockMovementType
import com.minipos.domain.model.StockOverviewItem
import com.minipos.domain.repository.InventoryRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
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
    // Quick stock add
    val quickAddItem: StockOverviewItem? = null,
    val quickAddSaving: Boolean = false,
)

@HiltViewModel
class StockManagementViewModel @Inject constructor(
    private val app: Application,
    private val storeRepository: StoreRepository,
    private val inventoryRepository: InventoryRepository,
    private val appPreferences: AppPreferences,
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

    // ── Quick Stock Add ──

    fun showQuickAdd(item: StockOverviewItem) {
        _state.update { it.copy(quickAddItem = item) }
    }

    fun dismissQuickAdd() {
        _state.update { it.copy(quickAddItem = null) }
    }

    fun quickAddStock(quantity: Double, notes: String) {
        val item = _state.value.quickAddItem ?: return
        _state.update { it.copy(quickAddSaving = true) }
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first() ?: return@launch
            val result = inventoryRepository.adjustStock(
                storeId = storeId,
                productId = item.productId,
                amount = quantity,
                type = StockMovementType.ADJUSTMENT_IN,
                userId = userId,
                referenceId = null,
                notes = notes.ifBlank { null },
            )
            when (result) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            quickAddItem = null,
                            quickAddSaving = false,
                        )
                    }
                    loadData()
                }
                is Result.Error -> {
                    _state.update { it.copy(quickAddSaving = false, toastMessage = result.message) }
                }
            }
        }
    }

    // ── Export Stock Report as CSV ──

    fun exportStockReport(context: Context) {
        viewModelScope.launch {
            try {
                val store = storeRepository.getStore() ?: return@launch
                val items = _state.value.allItems
                if (items.isEmpty()) return@launch

                val dateStr = DateUtils.formatOrderDate(System.currentTimeMillis())
                val fileName = "minipos_stock_report_${dateStr}.csv"
                val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
                val file = File(reportsDir, fileName)

                FileWriter(file).use { writer ->
                    // BOM for Excel UTF-8
                    writer.write("\uFEFF")
                    // Header
                    writer.write("Product,SKU,Unit,Current Stock,Min Stock,Cost Price,Selling Price,Stock Value,Status\n")
                    for (item in items) {
                        val status = when {
                            item.currentStock <= 0 -> "OUT_OF_STOCK"
                            item.currentStock <= item.minStock -> "LOW_STOCK"
                            else -> "IN_STOCK"
                        }
                        writer.write(
                            "${item.productName.replace(",", " ")}," +
                            "${item.productSku}," +
                            "${item.productUnit}," +
                            "${item.currentStock}," +
                            "${item.minStock}," +
                            "${item.costPrice}," +
                            "${item.sellingPrice}," +
                            "${item.stockValue}," +
                            "$status\n"
                        )
                    }
                    // Summary
                    writer.write("\n")
                    writer.write("Total Products,${items.size}\n")
                    writer.write("Total Stock Value,${items.sumOf { it.stockValue }}\n")
                    writer.write("Low Stock,${items.count { it.currentStock in 1.0..it.minStock.toDouble() }}\n")
                    writer.write("Out of Stock,${items.count { it.currentStock <= 0 }}\n")
                    writer.write("Store,${store.name}\n")
                    writer.write("Date,$dateStr\n")
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Mini POS - Stock Report ($dateStr)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })

                _state.update { it.copy(toastMessage = app.getString(R.string.stock_export_success)) }
            } catch (e: Exception) {
                _state.update { it.copy(toastMessage = app.getString(R.string.stock_export_error)) }
            }
        }
    }
}
