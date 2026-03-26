package com.minipos.ui.supplier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.utils.UuidGenerator
import com.minipos.domain.model.Result
import com.minipos.domain.model.Supplier
import com.minipos.domain.repository.StoreRepository
import com.minipos.domain.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierListState(
    val suppliers: List<Supplier> = emptyList(),
    val isLoading: Boolean = true,
    val showForm: Boolean = false,
    val editingSupplier: Supplier? = null,
)

@HiltViewModel
class SupplierListViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val supplierRepository: SupplierRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SupplierListState())
    val state: StateFlow<SupplierListState> = _state

    private var storeId: String = ""

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val suppliers = supplierRepository.getAll(storeId)
            _state.update { it.copy(suppliers = suppliers, isLoading = false) }
        }
    }

    fun showCreateForm() { _state.update { it.copy(showForm = true, editingSupplier = null) } }
    fun showEditForm(supplier: Supplier) { _state.update { it.copy(showForm = true, editingSupplier = supplier) } }
    fun dismissForm() { _state.update { it.copy(showForm = false, editingSupplier = null) } }

    fun save(name: String, contactPerson: String?, phone: String?, email: String?, address: String?, taxCode: String?, notes: String?) {
        viewModelScope.launch {
            val editing = _state.value.editingSupplier
            val supplier = Supplier(
                id = editing?.id ?: UuidGenerator.generate(),
                storeId = storeId,
                name = name,
                contactPerson = contactPerson,
                phone = phone,
                email = email,
                address = address,
                taxCode = taxCode,
                notes = notes,
                isActive = true,
                createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            val result = if (editing != null) supplierRepository.update(supplier) else supplierRepository.create(supplier)
            if (result is Result.Success) {
                dismissForm()
                loadData()
            }
        }
    }

    fun delete(supplier: Supplier) {
        viewModelScope.launch {
            supplierRepository.delete(supplier.id)
            loadData()
        }
    }
}
