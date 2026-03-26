package com.minipos.ui.report

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
import javax.inject.Inject

data class ReportState(
    val todayRevenue: Double = 0.0,
    val todayOrders: Int = 0,
    val weekRevenue: Double = 0.0,
    val weekOrders: Int = 0,
    val monthRevenue: Double = 0.0,
    val monthOrders: Int = 0,
    val recentOrders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTab: Int = 0, // 0=today, 1=week, 2=month
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportState())
    val state: StateFlow<ReportState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            val storeId = store.id

            val now = System.currentTimeMillis()
            val todayStart = DateUtils.startOfDay(now)
            val todayEnd = DateUtils.endOfDay(now)

            // Calculate week start (7 days ago)
            val weekStart = todayStart - (7 * 24 * 60 * 60 * 1000L)

            // Calculate month start (30 days ago)
            val monthStart = todayStart - (30 * 24 * 60 * 60 * 1000L)

            val todayOrders = orderRepository.getOrdersByDateRange(storeId, todayStart, todayEnd)
            val weekOrders = orderRepository.getOrdersByDateRange(storeId, weekStart, todayEnd)
            val monthOrders = orderRepository.getOrdersByDateRange(storeId, monthStart, todayEnd)

            _state.update {
                it.copy(
                    todayRevenue = todayOrders.sumOf { o -> o.totalAmount },
                    todayOrders = todayOrders.size,
                    weekRevenue = weekOrders.sumOf { o -> o.totalAmount },
                    weekOrders = weekOrders.size,
                    monthRevenue = monthOrders.sumOf { o -> o.totalAmount },
                    monthOrders = monthOrders.size,
                    recentOrders = todayOrders.take(10),
                    isLoading = false,
                )
            }
        }
    }

    fun selectTab(tab: Int) {
        _state.update { it.copy(selectedTab = tab) }
    }
}
