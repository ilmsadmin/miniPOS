package com.minipos.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManagementStatsState(
    val productCount: Int = 0,
    val categoryCount: Int = 0,
    val customerCount: Int = 0,
    val supplierCount: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class StoreManagementViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val customerRepository: CustomerRepository,
    private val supplierRepository: SupplierRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ManagementStatsState())
    val state: StateFlow<ManagementStatsState> = _state.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch

            // Observe all counts reactively
            try {
                combine(
                    productRepository.observeProducts(store.id),
                    categoryRepository.observeCategories(store.id),
                    customerRepository.observeCustomers(store.id),
                    supplierRepository.observeSuppliers(store.id),
                ) { products, categories, customers, suppliers ->
                    ManagementStatsState(
                        productCount = products.size,
                        categoryCount = categories.size,
                        customerCount = customers.size,
                        supplierCount = suppliers.size,
                        isLoading = false,
                    )
                }.collect { stats ->
                    _state.value = stats
                }
            } catch (_: Exception) { /* prevent crash from reactive observer */ }
        }
    }
}
