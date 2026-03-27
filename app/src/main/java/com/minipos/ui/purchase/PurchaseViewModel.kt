package com.minipos.ui.purchase

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.model.Product
import com.minipos.domain.model.ProductVariant
import com.minipos.domain.model.Result
import com.minipos.domain.model.StockMovementType
import com.minipos.domain.model.Supplier
import com.minipos.domain.repository.InventoryRepository
import com.minipos.domain.repository.ProductRepository
import com.minipos.domain.repository.StoreRepository
import com.minipos.domain.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseLineItem(
    val product: Product,
    val variant: ProductVariant? = null,
    val currentStock: Double = 0.0,
    val quantity: Double = 0.0,
    val costPrice: Double = 0.0,
) {
    /** Unique key combining product + variant */
    val lineKey: String get() = if (variant != null) "${product.id}::${variant.id}" else product.id
    val displayName: String get() = if (variant != null) "${product.name} (${variant.variantName})" else product.name
    val displaySku: String get() = variant?.sku ?: product.sku
}

data class PurchaseState(
    val isLoading: Boolean = true,
    val suppliers: List<Supplier> = emptyList(),
    val selectedSupplierId: String? = null,
    val products: List<Product> = emptyList(),
    val lineItems: List<PurchaseLineItem> = emptyList(),
    val searchQuery: String = "",
    val showProductPicker: Boolean = false,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val notes: String = "",
    // Variant picker
    val showVariantPicker: Boolean = false,
    val variantPickerProduct: Product? = null,
    val variantPickerVariants: List<ProductVariant> = emptyList(),
)

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val app: Application,
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val inventoryRepository: InventoryRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private fun str(resId: Int) = app.getString(resId)
    private fun str(resId: Int, vararg args: Any) = app.getString(resId, *args)

    private val _state = MutableStateFlow(PurchaseState())
    val state: StateFlow<PurchaseState> = _state

    private var storeId: String = ""

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val suppliers = supplierRepository.getAll(storeId)
            val products = productRepository.getAll(storeId).filter { it.trackInventory && it.isActive }
            _state.update { it.copy(isLoading = false, suppliers = suppliers, products = products) }
        }
    }

    fun selectSupplier(supplierId: String?) {
        _state.update { it.copy(selectedSupplierId = supplierId) }
    }

    fun showProductPicker() {
        _state.update { it.copy(showProductPicker = true, searchQuery = "") }
    }

    fun dismissProductPicker() {
        _state.update { it.copy(showProductPicker = false, searchQuery = "") }
    }

    fun updateSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun addProduct(product: Product) {
        if (product.hasVariants) {
            // Load variants and show picker
            viewModelScope.launch {
                val variants = productRepository.getVariants(product.id)
                if (variants.isEmpty()) {
                    // No variants defined, add product directly
                    addLineItem(product, null)
                } else {
                    _state.update {
                        it.copy(
                            showVariantPicker = true,
                            variantPickerProduct = product,
                            variantPickerVariants = variants,
                        )
                    }
                }
            }
        } else {
            addLineItem(product, null)
        }
    }

    fun addProductWithVariant(product: Product, variant: ProductVariant) {
        addLineItem(product, variant)
        dismissVariantPicker()
    }

    fun dismissVariantPicker() {
        _state.update { it.copy(showVariantPicker = false, variantPickerProduct = null, variantPickerVariants = emptyList()) }
    }

    private fun addLineItem(product: Product, variant: ProductVariant?) {
        viewModelScope.launch {
            val lineKey = if (variant != null) "${product.id}::${variant.id}" else product.id
            val existing = _state.value.lineItems.find { it.lineKey == lineKey }
            if (existing != null) {
                // Already in list, just increment
                _state.update { s ->
                    s.copy(lineItems = s.lineItems.map {
                        if (it.lineKey == lineKey) it.copy(quantity = it.quantity + 1)
                        else it
                    })
                }
            } else {
                val stock = inventoryRepository.getStock(storeId, product.id)
                val cost = variant?.costPrice ?: product.costPrice
                _state.update { s ->
                    s.copy(lineItems = s.lineItems + PurchaseLineItem(
                        product = product,
                        variant = variant,
                        currentStock = stock?.quantity ?: 0.0,
                        quantity = 1.0,
                        costPrice = cost,
                    ))
                }
            }
        }
    }

    fun updateQuantity(lineKey: String, quantity: Double) {
        _state.update { s ->
            s.copy(lineItems = s.lineItems.map {
                if (it.lineKey == lineKey) it.copy(quantity = quantity.coerceAtLeast(0.0))
                else it
            })
        }
    }

    fun updateCostPrice(lineKey: String, costPrice: Double) {
        _state.update { s ->
            s.copy(lineItems = s.lineItems.map {
                if (it.lineKey == lineKey) it.copy(costPrice = costPrice.coerceAtLeast(0.0))
                else it
            })
        }
    }

    fun removeProduct(lineKey: String) {
        _state.update { s ->
            s.copy(lineItems = s.lineItems.filter { it.lineKey != lineKey })
        }
    }

    fun updateNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    fun clearMessage() {
        _state.update { it.copy(successMessage = null, errorMessage = null) }
    }

    val filteredProducts: List<Product>
        get() {
            val query = _state.value.searchQuery.trim().lowercase()
            val products = _state.value.products
            if (query.isEmpty()) return products
            return products.filter {
                it.name.lowercase().contains(query) ||
                    it.sku.lowercase().contains(query) ||
                    (it.barcode?.lowercase()?.contains(query) == true)
            }
        }

    fun confirmPurchase() {
        val items = _state.value.lineItems.filter { it.quantity > 0 }
        if (items.isEmpty()) {
            _state.update { it.copy(errorMessage = str(R.string.msg_purchase_no_items)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val userId = appPreferences.currentUserId.first() ?: run {
                _state.update { it.copy(isSaving = false, errorMessage = str(R.string.msg_user_not_found)) }
                return@launch
            }
            val supplierId = _state.value.selectedSupplierId

            var hasError = false
            for (item in items) {
                val result = inventoryRepository.adjustStock(
                    storeId = storeId,
                    productId = item.product.id,
                    amount = item.quantity,
                    type = StockMovementType.PURCHASE_IN,
                    userId = userId,
                    referenceId = null,
                    supplierId = supplierId,
                )
                if (result is Result.Error) {
                    hasError = true
                    _state.update { it.copy(isSaving = false, errorMessage = str(R.string.msg_purchase_item_error, item.product.name, result.message)) }
                    return@launch
                }
                // Update cost price if changed
                if (item.costPrice > 0) {
                    if (item.variant != null) {
                        // Update variant cost price
                        if (item.costPrice != (item.variant.costPrice ?: 0.0)) {
                            productRepository.updateVariant(item.variant.copy(costPrice = item.costPrice))
                        }
                    } else if (item.costPrice != item.product.costPrice) {
                        productRepository.update(item.product.copy(costPrice = item.costPrice))
                    }
                }
            }

            if (!hasError) {
                val totalQty = items.sumOf { it.quantity }.toLong()
                val totalProducts = items.size
                _state.update {
                    it.copy(
                        isSaving = false,
                        lineItems = emptyList(),
                        notes = "",
                        successMessage = str(R.string.msg_purchase_success, totalQty.toDouble(), totalProducts),
                    )
                }
            }
        }
    }
}
