package com.minipos.ui.supplier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.utils.UuidGenerator
import com.minipos.domain.model.Result
import com.minipos.domain.model.Supplier
import com.minipos.domain.repository.ProductRepository
import com.minipos.domain.repository.StoreRepository
import com.minipos.domain.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierFormData(
    val name: String = "",
    val code: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val taxCode: String = "",
    val paymentTerm: String = "Cash",
    val bankName: String = "",
    val bankAccount: String = "",
    val bankAccountHolder: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
)

data class SupplierListState(
    val suppliers: List<Supplier> = emptyList(),
    val filteredSuppliers: List<Supplier> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val showForm: Boolean = false,
    val editingSupplier: Supplier? = null,
    val productCounts: Map<String, Int> = emptyMap(),
)

@HiltViewModel
class SupplierListViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val supplierRepository: SupplierRepository,
    private val productRepository: ProductRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SupplierListState())
    val state: StateFlow<SupplierListState> = _state

    private val _supplierFormState = MutableStateFlow(SupplierFormData())
    val supplierFormState: StateFlow<SupplierFormData> = _supplierFormState

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess: SharedFlow<Unit> = _saveSuccess

    private var storeId: String = ""
    private var editingSupplierId: String? = null

    init { loadData() }

    fun refreshData() { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val suppliers = supplierRepository.getAll(storeId)
            // Count products per supplier: each product = 1 (root variant) + child variants
            val products = productRepository.getAll(storeId)
            val counts = mutableMapOf<String, Int>()
            products.filter { it.supplierId != null }.forEach { product ->
                val supplierId = product.supplierId!!
                val variantCount = productRepository.getVariantCount(product.id)
                counts[supplierId] = (counts[supplierId] ?: 0) + 1 + variantCount
            }
            val query = _state.value.searchQuery
            _state.update {
                it.copy(
                    suppliers = suppliers,
                    filteredSuppliers = filterSuppliers(suppliers, query),
                    productCounts = counts,
                    isLoading = false,
                )
            }
        }
    }

    fun search(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                filteredSuppliers = filterSuppliers(it.suppliers, query),
            )
        }
    }

    private fun filterSuppliers(suppliers: List<Supplier>, query: String): List<Supplier> {
        if (query.isBlank()) return suppliers
        val q = query.trim().lowercase()
        return suppliers.filter { supplier ->
            supplier.name.lowercase().contains(q) ||
                supplier.contactPerson?.lowercase()?.contains(q) == true ||
                supplier.phone?.contains(q) == true ||
                supplier.email?.lowercase()?.contains(q) == true
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

    // ═══ Full-screen form support ═══

    fun initNewSupplierForm() {
        editingSupplierId = null
        _supplierFormState.value = SupplierFormData()
    }

    fun loadSupplierForEdit(supplierId: String) {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val suppliers = supplierRepository.getAll(storeId)
            val supplier = suppliers.find { it.id == supplierId } ?: return@launch
            editingSupplierId = supplier.id
            _supplierFormState.value = SupplierFormData(
                name = supplier.name,
                contactPerson = supplier.contactPerson ?: "",
                phone = supplier.phone ?: "",
                mobile = supplier.mobile ?: "",
                email = supplier.email ?: "",
                address = supplier.address ?: "",
                taxCode = supplier.taxCode ?: "",
                paymentTerm = supplier.paymentTerm ?: "Cash",
                bankName = supplier.bankName ?: "",
                bankAccount = supplier.bankAccount ?: "",
                bankAccountHolder = supplier.bankAccountHolder ?: "",
                notes = supplier.notes ?: "",
                isActive = supplier.isActive,
            )
        }
    }

    fun updateSupplierForm(data: SupplierFormData) {
        _supplierFormState.value = data
    }

    fun saveSupplierForm() {
        val form = _supplierFormState.value
        if (form.name.isBlank()) return
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val existing = editingSupplierId?.let { id ->
                supplierRepository.getAll(storeId).find { it.id == id }
            }
            val supplier = Supplier(
                id = existing?.id ?: UuidGenerator.generate(),
                storeId = storeId,
                name = form.name,
                contactPerson = form.contactPerson.ifBlank { null },
                phone = form.phone.ifBlank { null },
                mobile = form.mobile.ifBlank { null },
                email = form.email.ifBlank { null },
                address = form.address.ifBlank { null },
                taxCode = form.taxCode.ifBlank { null },
                paymentTerm = form.paymentTerm.ifBlank { null },
                bankName = form.bankName.ifBlank { null },
                bankAccount = form.bankAccount.ifBlank { null },
                bankAccountHolder = form.bankAccountHolder.ifBlank { null },
                notes = form.notes.ifBlank { null },
                isActive = form.isActive,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            val result = if (existing != null) supplierRepository.update(supplier) else supplierRepository.create(supplier)
            if (result is Result.Success) {
                _saveSuccess.emit(Unit)
                loadData()
            }
        }
    }

    fun deleteSupplierFromForm() {
        viewModelScope.launch {
            editingSupplierId?.let {
                supplierRepository.delete(it)
                _saveSuccess.emit(Unit)
                loadData()
            }
        }
    }
}
