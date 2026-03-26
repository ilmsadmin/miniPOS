package com.minipos.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.model.Order
import com.minipos.domain.repository.OrderRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderListState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val filterStatus: String? = null, // null = all
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

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            orderRepository.observeOrders(storeId).collect { orders ->
                allOrders = orders
                applyFilter()
            }
        }
    }

    fun setFilter(status: String?) {
        _state.update { it.copy(filterStatus = status) }
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = _state.value.filterStatus?.let { status ->
            allOrders.filter { it.status.name.equals(status, ignoreCase = true) }
        } ?: allOrders
        _state.update { it.copy(orders = filtered, isLoading = false) }
    }
}
