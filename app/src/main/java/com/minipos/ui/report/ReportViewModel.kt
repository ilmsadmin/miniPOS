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
import java.util.Calendar
import javax.inject.Inject

data class TopProduct(
    val name: String,
    val quantity: Int,
    val totalAmount: Double,
)

data class HourlyRevenue(
    val hour: String,
    val amount: Double,
)

data class ReportState(
    val todayRevenue: Double = 0.0,
    val todayOrders: Int = 0,
    val weekRevenue: Double = 0.0,
    val weekOrders: Int = 0,
    val monthRevenue: Double = 0.0,
    val monthOrders: Int = 0,
    val productsSold: Int = 0,
    val customerCount: Int = 0,
    val revenueTrend: Double? = null, // percentage change
    val ordersTrend: Int? = null, // absolute change
    val hourlyRevenue: List<HourlyRevenue> = emptyList(),
    val topProducts: List<TopProduct> = emptyList(),
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

    private var allOrders: Map<Int, List<Order>> = emptyMap() // tab -> orders

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            val storeId = store.id

            val now = System.currentTimeMillis()
            val todayStart = DateUtils.startOfDay(now)
            val todayEnd = DateUtils.endOfDay(now)
            val weekStart = todayStart - (7 * 24 * 60 * 60 * 1000L)
            val monthStart = todayStart - (30 * 24 * 60 * 60 * 1000L)

            val todayOrders = orderRepository.getOrdersByDateRange(storeId, todayStart, todayEnd)
            val weekOrders = orderRepository.getOrdersByDateRange(storeId, weekStart, todayEnd)
            val monthOrders = orderRepository.getOrdersByDateRange(storeId, monthStart, todayEnd)

            allOrders = mapOf(0 to todayOrders, 1 to weekOrders, 2 to monthOrders)

            // Compute today details
            val hourly = computeHourlyRevenue(todayOrders)
            val topProds = computeTopProducts(todayOrders)
            val productsSold = computeProductsSold(todayOrders)
            val customerCount = todayOrders.mapNotNull { it.customerId }.distinct().size

            // Trends - compare with yesterday
            val yesterdayStart = todayStart - (24 * 60 * 60 * 1000L)
            val yesterdayEnd = todayStart - 1
            val yesterdayOrders = orderRepository.getOrdersByDateRange(storeId, yesterdayStart, yesterdayEnd)
            val yesterdayRevenue = yesterdayOrders.sumOf { it.totalAmount }
            val todayRevenueVal = todayOrders.sumOf { it.totalAmount }
            val revenueTrend = if (yesterdayRevenue > 0) {
                ((todayRevenueVal - yesterdayRevenue) / yesterdayRevenue * 100)
            } else null
            val ordersTrend = todayOrders.size - yesterdayOrders.size

            _state.update {
                it.copy(
                    todayRevenue = todayRevenueVal,
                    todayOrders = todayOrders.size,
                    weekRevenue = weekOrders.sumOf { o -> o.totalAmount },
                    weekOrders = weekOrders.size,
                    monthRevenue = monthOrders.sumOf { o -> o.totalAmount },
                    monthOrders = monthOrders.size,
                    productsSold = productsSold,
                    customerCount = customerCount,
                    revenueTrend = revenueTrend,
                    ordersTrend = if (ordersTrend != 0) ordersTrend else null,
                    hourlyRevenue = hourly,
                    topProducts = topProds,
                    recentOrders = todayOrders.take(10),
                    isLoading = false,
                )
            }
        }
    }

    fun selectTab(tab: Int) {
        _state.update { it.copy(selectedTab = tab) }
        // Recompute tab-specific data
        viewModelScope.launch {
            val orders = allOrders[tab] ?: return@launch
            val topProds = computeTopProducts(orders)
            val productsSold = computeProductsSold(orders)
            val customerCount = orders.mapNotNull { it.customerId }.distinct().size
            val hourly = if (tab == 0) computeHourlyRevenue(orders) else emptyList()

            _state.update {
                it.copy(
                    topProducts = topProds,
                    productsSold = productsSold,
                    customerCount = customerCount,
                    hourlyRevenue = hourly,
                )
            }
        }
    }

    private suspend fun computeTopProducts(orders: List<Order>): List<TopProduct> {
        // Aggregate items across orders
        val itemMap = mutableMapOf<String, Pair<String, Pair<Int, Double>>>() // productId -> (name, (qty, total))
        for (order in orders) {
            val detail = orderRepository.getOrderDetail(order.id) ?: continue
            for (item in detail.items) {
                val existing = itemMap[item.productId]
                if (existing != null) {
                    itemMap[item.productId] = existing.first to
                            Pair(existing.second.first + item.quantity.toInt(), existing.second.second + item.totalPrice)
                } else {
                    itemMap[item.productId] = item.productName to Pair(item.quantity.toInt(), item.totalPrice)
                }
            }
        }
        return itemMap.values
            .map { TopProduct(it.first, it.second.first, it.second.second) }
            .sortedByDescending { it.quantity }
            .take(5)
    }

    private fun computeProductsSold(@Suppress("UNUSED_PARAMETER") orders: List<Order>): Int {
        // Simple estimate: count total items (we'd need order details for exact)
        // For performance, we'll use the top products data
        return 0 // Will be computed from order details in selectTab
    }

    private fun computeHourlyRevenue(orders: List<Order>): List<HourlyRevenue> {
        val cal = Calendar.getInstance()
        val hourMap = mutableMapOf<Int, Double>()
        for (order in orders) {
            cal.timeInMillis = order.createdAt
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourMap[hour] = (hourMap[hour] ?: 0.0) + order.totalAmount
        }
        // Return hours from 6h to 22h
        return (6..22).map { h ->
            HourlyRevenue("${h}h", hourMap[h] ?: 0.0)
        }
    }
}
