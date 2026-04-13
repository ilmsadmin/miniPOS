package com.minipos.ui.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils
import com.minipos.domain.model.Customer
import com.minipos.domain.model.Order
import com.minipos.domain.model.StockOverviewItem
import com.minipos.domain.model.StockSummary
import com.minipos.domain.repository.CustomerRepository
import com.minipos.domain.repository.InventoryRepository
import com.minipos.domain.repository.OrderRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
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

data class ProductProfit(
    val name: String,
    val revenue: Double,
    val cost: Double,
    val profit: Double,
    val margin: Double, // percentage
    val quantitySold: Int,
)

data class InventoryReportData(
    val summary: StockSummary = StockSummary(),
    val stockOverview: List<StockOverviewItem> = emptyList(),
)

data class DebtReportData(
    val totalDebt: Double = 0.0,
    val customersWithDebt: List<Customer> = emptyList(),
)

data class ProfitReportData(
    val totalRevenue: Double = 0.0,
    val totalCost: Double = 0.0,
    val grossProfit: Double = 0.0,
    val margin: Double = 0.0, // percentage
    val productProfits: List<ProductProfit> = emptyList(),
)

enum class DetailReportType {
    NONE, INVENTORY, DEBT, PROFIT
}

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
    // Detail reports
    val activeDetailReport: DetailReportType = DetailReportType.NONE,
    val inventoryReport: InventoryReportData = InventoryReportData(),
    val debtReport: DebtReportData = DebtReportData(),
    val profitReport: ProfitReportData = ProfitReportData(),
    val isDetailLoading: Boolean = false,
    val exportMessage: String? = null,
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val storeRepository: StoreRepository,
    private val orderRepository: OrderRepository,
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository,
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
        // Aggregate items across orders, distributing order-level discount proportionally
        val itemMap = mutableMapOf<String, Pair<String, Pair<Int, Double>>>() // productId -> (name, (qty, total))
        for (order in orders) {
            val detail = orderRepository.getOrderDetail(order.id) ?: continue
            val itemsSubtotal = detail.items.sumOf { it.totalPrice }
            val orderDiscount = order.discountAmount
            for (item in detail.items) {
                // Distribute order-level discount proportionally based on item's share of subtotal
                val itemShare = if (itemsSubtotal > 0) item.totalPrice / itemsSubtotal else 0.0
                val itemOrderDiscount = orderDiscount * itemShare
                val adjustedTotal = item.totalPrice - itemOrderDiscount

                val existing = itemMap[item.productId]
                if (existing != null) {
                    itemMap[item.productId] = existing.first to
                            Pair(existing.second.first + item.quantity.toInt(), existing.second.second + adjustedTotal)
                } else {
                    itemMap[item.productId] = item.productName to Pair(item.quantity.toInt(), adjustedTotal)
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

    // ─── Detail Report Methods ───

    fun openDetailReport(type: DetailReportType) {
        _state.update { it.copy(activeDetailReport = type, isDetailLoading = true) }
        viewModelScope.launch {
            when (type) {
                DetailReportType.INVENTORY -> loadInventoryReport()
                DetailReportType.DEBT -> loadDebtReport()
                DetailReportType.PROFIT -> loadProfitReport()
                DetailReportType.NONE -> {}
            }
            _state.update { it.copy(isDetailLoading = false) }
        }
    }

    fun closeDetailReport() {
        _state.update { it.copy(activeDetailReport = DetailReportType.NONE) }
    }

    fun clearExportMessage() {
        _state.update { it.copy(exportMessage = null) }
    }

    private suspend fun loadInventoryReport() {
        val store = storeRepository.getStore() ?: return
        val storeId = store.id
        val now = System.currentTimeMillis()
        val startTime = DateUtils.startOfDay(now) - (30 * 24 * 60 * 60 * 1000L)
        val endTime = DateUtils.endOfDay(now)

        val summary = inventoryRepository.getStockSummary(storeId, startTime, endTime)
        val overview = inventoryRepository.getAllStockOverview(storeId)
            .sortedByDescending { it.stockValue }

        _state.update {
            it.copy(
                inventoryReport = InventoryReportData(
                    summary = summary,
                    stockOverview = overview,
                )
            )
        }
    }

    private suspend fun loadDebtReport() {
        val store = storeRepository.getStore() ?: return
        val storeId = store.id

        // Get all customers and filter those with debt
        val allCustomers = customerRepository.search(storeId, "")
        val customersWithDebt = allCustomers
            .filter { it.debtAmount > 0 }
            .sortedByDescending { it.debtAmount }
        val totalDebt = customersWithDebt.sumOf { it.debtAmount }

        _state.update {
            it.copy(
                debtReport = DebtReportData(
                    totalDebt = totalDebt,
                    customersWithDebt = customersWithDebt,
                )
            )
        }
    }

    private suspend fun loadProfitReport() {
        storeRepository.getStore() ?: return

        val orders = allOrders[_state.value.selectedTab] ?: return

        // Calculate profit per product, distributing order-level discount proportionally
        val productMap = mutableMapOf<String, ProductProfitAccum>() // productId -> accumulator
        for (order in orders) {
            val detail = orderRepository.getOrderDetail(order.id) ?: continue
            val itemsSubtotal = detail.items.sumOf { it.totalPrice }
            val orderDiscount = order.discountAmount
            for (item in detail.items) {
                // Distribute order-level discount proportionally based on item's share of subtotal
                val itemShare = if (itemsSubtotal > 0) item.totalPrice / itemsSubtotal else 0.0
                val itemOrderDiscount = orderDiscount * itemShare
                val adjustedRevenue = item.totalPrice - itemOrderDiscount

                val existing = productMap[item.productId]
                if (existing != null) {
                    productMap[item.productId] = existing.copy(
                        revenue = existing.revenue + adjustedRevenue,
                        cost = existing.cost + (item.costPrice * item.quantity),
                        quantity = existing.quantity + item.quantity.toInt(),
                    )
                } else {
                    productMap[item.productId] = ProductProfitAccum(
                        name = item.productName,
                        revenue = adjustedRevenue,
                        cost = item.costPrice * item.quantity,
                        quantity = item.quantity.toInt(),
                    )
                }
            }
        }

        val productProfits = productMap.values.map { accum ->
            val profit = accum.revenue - accum.cost
            val margin = if (accum.revenue > 0) (profit / accum.revenue * 100) else 0.0
            ProductProfit(
                name = accum.name,
                revenue = accum.revenue,
                cost = accum.cost,
                profit = profit,
                margin = margin,
                quantitySold = accum.quantity,
            )
        }.sortedByDescending { it.profit }

        val totalRevenue = productProfits.sumOf { it.revenue }
        val totalCost = productProfits.sumOf { it.cost }
        val grossProfit = totalRevenue - totalCost
        val margin = if (totalRevenue > 0) (grossProfit / totalRevenue * 100) else 0.0

        _state.update {
            it.copy(
                profitReport = ProfitReportData(
                    totalRevenue = totalRevenue,
                    totalCost = totalCost,
                    grossProfit = grossProfit,
                    margin = margin,
                    productProfits = productProfits,
                )
            )
        }
    }

    fun exportSalesReport(context: Context) {
        viewModelScope.launch {
            try {
                val store = storeRepository.getStore() ?: return@launch
                val orders = allOrders[_state.value.selectedTab] ?: return@launch
                val periodLabel = when (_state.value.selectedTab) {
                    0 -> "today"
                    1 -> "week"
                    else -> "month"
                }

                val fileName = "minipos_sales_${periodLabel}_${DateUtils.formatOrderDate(System.currentTimeMillis())}.csv"
                val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
                val file = File(reportsDir, fileName)

                FileWriter(file).use { writer ->
                    // BOM for Excel UTF-8
                    writer.write("\uFEFF")
                    // Header
                    writer.write("Order Code,Date,Customer,Subtotal,Discount,Tax,Total,Status\n")
                    for (order in orders) {
                        writer.write(
                            "${order.orderCode}," +
                            "${DateUtils.formatDateTime(order.createdAt)}," +
                            "${(order.customerName ?: "-").replace(",", " ")}," +
                            "${order.subtotal}," +
                            "${order.discountAmount}," +
                            "${order.taxAmount}," +
                            "${order.totalAmount}," +
                            "${order.status.name}\n"
                        )
                    }
                    // Summary
                    writer.write("\n")
                    writer.write("Total Orders,${orders.size}\n")
                    writer.write("Total Revenue,${orders.sumOf { it.totalAmount }}\n")
                    writer.write("Store,${store.name}\n")
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Mini POS - Sales Report ($periodLabel)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })

                _state.update { it.copy(exportMessage = "success") }
            } catch (e: Exception) {
                _state.update { it.copy(exportMessage = "error") }
            }
        }
    }

    private data class ProductProfitAccum(
        val name: String,
        val revenue: Double,
        val cost: Double,
        val quantity: Int,
    )
}
