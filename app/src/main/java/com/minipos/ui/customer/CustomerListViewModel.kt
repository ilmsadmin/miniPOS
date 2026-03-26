package com.minipos.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.utils.UuidGenerator
import com.minipos.domain.model.Customer
import com.minipos.domain.model.Result
import com.minipos.domain.repository.CustomerRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerListState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val showForm: Boolean = false,
    val editingCustomer: Customer? = null,
)

@HiltViewModel
class CustomerListViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerListState())
    val state: StateFlow<CustomerListState> = _state

    private var storeId: String = ""

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val customers = customerRepository.getRecent(storeId, 100)
            _state.update { it.copy(customers = customers, isLoading = false) }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(searchQuery = query) }
            val customers = if (query.isBlank()) {
                customerRepository.getRecent(storeId, 100)
            } else {
                customerRepository.search(storeId, query)
            }
            _state.update { it.copy(customers = customers) }
        }
    }

    fun showCreateForm() { _state.update { it.copy(showForm = true, editingCustomer = null) } }
    fun showEditForm(customer: Customer) { _state.update { it.copy(showForm = true, editingCustomer = customer) } }
    fun dismissForm() { _state.update { it.copy(showForm = false, editingCustomer = null) } }

    fun save(name: String, phone: String?, email: String?, address: String?, notes: String?) {
        viewModelScope.launch {
            val editing = _state.value.editingCustomer
            val customer = Customer(
                id = editing?.id ?: UuidGenerator.generate(),
                storeId = storeId,
                name = name,
                phone = phone,
                email = email,
                address = address,
                notes = notes,
                totalSpent = editing?.totalSpent ?: 0.0,
                visitCount = editing?.visitCount ?: 0,
                lastVisitAt = editing?.lastVisitAt,
                createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            val result = if (editing != null) customerRepository.update(customer) else customerRepository.create(customer)
            if (result is Result.Success) {
                dismissForm()
                loadData()
            }
        }
    }

    fun delete(customer: Customer) {
        viewModelScope.launch {
            customerRepository.delete(customer.id)
            loadData()
        }
    }
}
