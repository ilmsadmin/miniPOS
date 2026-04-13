package com.minipos.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.utils.DateUtils
import com.minipos.domain.model.Order
import com.minipos.domain.repository.OrderRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class PeriodFilter { TODAY, LAST_7_DAYS, LAST_30_DAYS, CUSTOM }

data class DayGroup(
    val dateLabel: String,
    val isToday: Boolean,
    val orders: List<Order>,
    val dayTotal: Double,
)

data class OrderListState(
    val allOrders: List<Order> = emptyList(),
    val dayGroups: List<DayGroup> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val periodFilter: PeriodFilter = PeriodFilter.TODAY,
    val filterStatus: String? = null,
)

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OrderListState())
    val state: StateFlow<OrderListState> = _state

    private var storeId: String = ""
    private var allOrders: List<Order> = emptyList()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            try {
                orderRepository.observeOrders(storeId).collect { orders ->
                    allOrders = orders.sortedByDescending { it.createdAt }
                    applyFilters()
                }
            } catch (_: Exception) { /* prevent crash from reactive observer */ }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setPeriodFilter(period: PeriodFilter) {
        _state.update { it.copy(periodFilter = period) }
        applyFilters()
    }

    fun setFilter(status: String?) {
        _state.update { it.copy(filterStatus = status) }
        applyFilters()
    }

    private fun applyFilters() {
        val now = System.currentTimeMillis()
        val periodFilter = _state.value.periodFilter
        val searchQuery = _state.value.searchQuery.trim().lowercase()
        val statusFilter = _state.value.filterStatus

        // 1. Filter by period
        val startTime = when (periodFilter) {
            PeriodFilter.TODAY -> DateUtils.startOfDay(now)
            PeriodFilter.LAST_7_DAYS -> DateUtils.startOfDay(now) - 6L * 24 * 60 * 60 * 1000
            PeriodFilter.LAST_30_DAYS -> DateUtils.startOfDay(now) - 29L * 24 * 60 * 60 * 1000
            PeriodFilter.CUSTOM -> 0L // Show all
        }

        var filtered = allOrders.filter { it.createdAt >= startTime }

        // 2. Filter by status
        if (statusFilter != null) {
            filtered = filtered.filter { it.status.name.equals(statusFilter, ignoreCase = true) }
        }

        // 3. Filter by search
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { order ->
                order.orderCode.lowercase().contains(searchQuery) ||
                        (order.customerName?.lowercase()?.contains(searchQuery) == true) ||
                        (order.customerPhone?.contains(searchQuery) == true)
            }
        }

        // 4. Group by day
        val todayStart = DateUtils.startOfDay(now)
        val grouped = filtered.groupBy { DateUtils.formatDate(it.createdAt) }
        val dayGroups = grouped.map { (dateStr, orders) ->
            val isToday = orders.firstOrNull()?.let {
                DateUtils.startOfDay(it.createdAt) == todayStart
            } ?: false
            DayGroup(
                dateLabel = dateStr,
                isToday = isToday,
                orders = orders.sortedByDescending { it.createdAt },
                dayTotal = orders.sumOf { it.totalAmount },
            )
        }.sortedByDescending { it.orders.firstOrNull()?.createdAt ?: 0L }

        _state.update { it.copy(allOrders = filtered, dayGroups = dayGroups, isLoading = false) }
    }
}
