package com.minipos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.model.Product
import com.minipos.domain.model.InventoryItem
import com.minipos.domain.model.StockMovementType
import com.minipos.domain.model.Supplier
import com.minipos.domain.model.Result
import com.minipos.domain.repository.InventoryRepository
import com.minipos.domain.repository.ProductRepository
import com.minipos.domain.repository.StoreRepository
import com.minipos.domain.repository.SupplierRepository
import com.minipos.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductStock(
    val product: Product,
    val currentStock: Double = 0.0,
)

data class InventoryState(
    val items: List<ProductStock> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val isLoading: Boolean = true,
    val showAdjustDialog: Boolean = false,
    val selectedProduct: Product? = null,
    val adjustError: String? = null,
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    private val supplierRepository: SupplierRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state

    private var storeId: String = ""

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val products = productRepository.getAll(storeId).filter { it.trackInventory }
            val items = products.map { product ->
                val stock = inventoryRepository.getStock(storeId, product.id)
                ProductStock(product = product, currentStock = stock?.quantity ?: 0.0)
            }
            val suppliers = supplierRepository.getAll(storeId)
            _state.update { it.copy(items = items, suppliers = suppliers, isLoading = false) }
        }
    }

    fun showAdjustDialog(product: Product) {
        _state.update { it.copy(showAdjustDialog = true, selectedProduct = product, adjustError = null) }
    }

    fun dismissAdjustDialog() {
        _state.update { it.copy(showAdjustDialog = false, selectedProduct = null) }
    }

    fun adjustStock(amount: Double, type: StockMovementType, supplierId: String? = null) {
        val product = _state.value.selectedProduct ?: return
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first() ?: return@launch
            val result = inventoryRepository.adjustStock(
                storeId = storeId,
                productId = product.id,
                amount = amount,
                type = type,
                userId = userId,
                referenceId = null,
                supplierId = supplierId,
            )
            when (result) {
                is Result.Success -> {
                    dismissAdjustDialog()
                    loadData()
                }
                is Result.Error -> {
                    _state.update { it.copy(adjustError = result.message) }
                }
            }
        }
    }
}
