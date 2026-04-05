package com.minipos.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.utils.UuidGenerator
import com.minipos.domain.model.Customer
import com.minipos.domain.model.Result
import com.minipos.domain.repository.CustomerRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CustomerStats(
    val totalCustomers: Int = 0,
    val newThisMonth: Int = 0,
    val withDebt: Int = 0,
)

data class CustomerFormData(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val customerType: String = "individual",
    val allowDebt: Boolean = true,
    val debtLimit: String = "",
    val isActive: Boolean = true,
) {
    val initials: String
        get() {
            val parts = name.trim().split("\\s+".toRegex())
            return when {
                parts.size >= 2 -> "${parts.first().first().uppercase()}${parts.last().first().uppercase()}"
                name.isNotBlank() -> name.take(2).uppercase()
                else -> "?"
            }
        }
}

data class CustomerListState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val showForm: Boolean = false,
    val editingCustomer: Customer? = null,
    val stats: CustomerStats = CustomerStats(),
    val detailCustomer: Customer? = null,
)

@HiltViewModel
class CustomerListViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerListState())
    val state: StateFlow<CustomerListState> = _state

    private val _customerFormState = MutableStateFlow(CustomerFormData())
    val customerFormState: StateFlow<CustomerFormData> = _customerFormState

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess: SharedFlow<Unit> = _saveSuccess

    private var storeId: String = ""
    private var editingCustomerId: String? = null

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val customers = customerRepository.getRecent(storeId, 100)
            val stats = computeStats(customers)
            _state.update { it.copy(customers = customers, isLoading = false, stats = stats) }
        }
    }

    private fun computeStats(customers: List<Customer>): CustomerStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis

        return CustomerStats(
            totalCustomers = customers.size,
            newThisMonth = customers.count { it.createdAt >= monthStart },
            withDebt = customers.count { it.hasDebt },
        )
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
                debtAmount = editing?.debtAmount ?: 0.0,
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

    // ═══ Full-screen form support ═══

    fun initNewCustomerForm() {
        editingCustomerId = null
        _customerFormState.value = CustomerFormData()
    }

    fun loadCustomerForEdit(customerId: String) {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val customers = customerRepository.getRecent(storeId, 500)
            val customer = customers.find { it.id == customerId } ?: return@launch
            editingCustomerId = customer.id
            _customerFormState.value = CustomerFormData(
                name = customer.name,
                phone = customer.phone ?: "",
                email = customer.email ?: "",
                address = customer.address ?: "",
                notes = customer.notes ?: "",
                isActive = true,
            )
        }
    }

    fun updateCustomerForm(data: CustomerFormData) {
        _customerFormState.value = data
    }

    fun saveCustomerForm() {
        val form = _customerFormState.value
        if (form.name.isBlank()) return
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val existing = editingCustomerId?.let { id ->
                customerRepository.getRecent(storeId, 500).find { it.id == id }
            }
            val customer = Customer(
                id = existing?.id ?: UuidGenerator.generate(),
                storeId = storeId,
                name = form.name,
                phone = form.phone.ifBlank { null },
                email = form.email.ifBlank { null },
                address = form.address.ifBlank { null },
                notes = form.notes.ifBlank { null },
                totalSpent = existing?.totalSpent ?: 0.0,
                debtAmount = existing?.debtAmount ?: 0.0,
                visitCount = existing?.visitCount ?: 0,
                lastVisitAt = existing?.lastVisitAt,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            val result = if (existing != null) customerRepository.update(customer) else customerRepository.create(customer)
            if (result is Result.Success) {
                _saveSuccess.emit(Unit)
                loadData()
            }
        }
    }

    fun deleteCustomerFromForm() {
        viewModelScope.launch {
            editingCustomerId?.let {
                customerRepository.delete(it)
                _saveSuccess.emit(Unit)
                loadData()
            }
        }
    }

    fun loadCustomerDetail(customerId: String) {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val customers = customerRepository.getRecent(storeId, 500)
            val customer = customers.find { it.id == customerId }
            _state.update { it.copy(detailCustomer = customer) }
        }
    }
}
