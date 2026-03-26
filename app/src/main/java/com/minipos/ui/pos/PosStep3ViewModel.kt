package com.minipos.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.utils.UuidGenerator
import com.minipos.domain.model.Customer
import com.minipos.domain.repository.CustomerRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PosStep3State(
    val recentCustomers: List<Customer> = emptyList(),
    val searchResults: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val showCreateForm: Boolean = false,
)

@HiltViewModel
class PosStep3ViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val customerRepository: CustomerRepository,
    val cartHolder: PosCartHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(PosStep3State())
    val state: StateFlow<PosStep3State> = _state

    init { loadRecent() }

    private fun loadRecent() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            val recent = customerRepository.getRecent(store.id, 20)
            _state.update { it.copy(recentCustomers = recent) }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(searchQuery = query) }
            if (query.isBlank()) return@launch
            val store = storeRepository.getStore() ?: return@launch
            val results = customerRepository.search(store.id, query)
            _state.update { it.copy(searchResults = results) }
        }
    }

    fun selectCustomer(customer: Customer?) {
        cartHolder.setCustomer(customer)
    }

    fun toggleCreateForm() {
        _state.update { it.copy(showCreateForm = !it.showCreateForm) }
    }

    fun quickCreateCustomer(name: String, phone: String?) {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            val customer = Customer(
                id = UuidGenerator.generate(),
                storeId = store.id,
                name = name,
                phone = phone,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            val result = customerRepository.create(customer)
            if (result is com.minipos.domain.model.Result.Success) {
                cartHolder.setCustomer(result.data)
                _state.update { it.copy(showCreateForm = false) }
                loadRecent()
            }
        }
    }
}
