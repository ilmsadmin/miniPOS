package com.minipos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.utils.DateUtils
import com.minipos.domain.model.StockHistoryItem
import com.minipos.domain.model.StockMovementType
import com.minipos.domain.model.StockSummary
import com.minipos.domain.repository.InventoryRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class InventoryHubState(
    val isLoading: Boolean = true,
    val summary: StockSummary = StockSummary(),
    val recentPurchases: List<StockHistoryItem> = emptyList(),
    val hasLoadedOnce: Boolean = false,
)

@HiltViewModel
class InventoryHubViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val inventoryRepository: InventoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryHubState())
    val state: StateFlow<InventoryHubState> = _state

    private var storeId: String = ""

    init {
        loadData()
    }

    fun refresh() {
        // Silent refresh — don't show loading spinner to avoid layout jank
        loadData(silent = true)
    }

    private fun loadData(silent: Boolean = false) {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            if (!silent) {
                _state.update { it.copy(isLoading = true) }
            }
            try {
                // Summary: use last 30 days range
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.DAY_OF_YEAR, -30)
                }
                val startTime = DateUtils.startOfDay(cal.timeInMillis)
                val endTime = DateUtils.endOfDay(now)

                val summary = inventoryRepository.getStockSummary(storeId, startTime, endTime)

                // Recent purchase-in movements (last 30 days, limited to 10)
                val allHistory = inventoryRepository.getStockHistory(storeId, startTime, endTime)
                val recentPurchases = allHistory
                    .filter { it.type == StockMovementType.PURCHASE_IN }
                    .sortedByDescending { it.createdAt }
                    .take(10)

                _state.update {
                    it.copy(
                        summary = summary,
                        recentPurchases = recentPurchases,
                        isLoading = false,
                        hasLoadedOnce = true,
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, hasLoadedOnce = true) }
            }
        }
    }
}
