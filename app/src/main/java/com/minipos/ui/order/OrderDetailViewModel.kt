package com.minipos.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.model.OrderDetail
import com.minipos.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderDetailState(
    val detail: OrderDetail? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OrderDetailState())
    val state: StateFlow<OrderDetailState> = _state

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            val detail = orderRepository.getOrderDetail(orderId)
            _state.update { it.copy(detail = detail, isLoading = false) }
        }
    }
}
