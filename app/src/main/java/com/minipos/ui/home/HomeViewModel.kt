package com.minipos.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.repository.AuthRepository
import com.minipos.domain.repository.OrderRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val storeName: String = "",
    val userName: String = "",
    val todayRevenue: Double = 0.0,
    val todayOrders: Int = 0,
    val lowStockCount: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storeRepository: StoreRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val session = authRepository.getCurrentSession()
            val store = storeRepository.getStore()
            if (session != null && store != null) {
                _state.update {
                    it.copy(
                        storeName = store.name,
                        userName = session.displayName,
                    )
                }
                // Observe orders to auto-refresh dashboard data
                orderRepository.observeOrders(store.id).collect {
                    val dashboard = orderRepository.getDashboardData(store.id)
                    _state.update { s ->
                        s.copy(
                            todayRevenue = dashboard.todayRevenue,
                            todayOrders = dashboard.todayOrders,
                            lowStockCount = dashboard.lowStockCount,
                        )
                    }
                }
            }
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            val dashboard = orderRepository.getDashboardData(store.id)
            _state.update {
                it.copy(
                    todayRevenue = dashboard.todayRevenue,
                    todayOrders = dashboard.todayOrders,
                    lowStockCount = dashboard.lowStockCount,
                )
            }
        }
    }
}
