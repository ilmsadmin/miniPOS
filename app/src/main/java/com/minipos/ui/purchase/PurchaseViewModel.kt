package com.minipos.ui.purchase

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.core.utils.UuidGenerator
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.model.Product
import com.minipos.domain.model.ProductVariant
import com.minipos.domain.model.PurchaseOrderItem
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    // PO metadata
    val purchaseDate: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
    val purchaseCode: String = "",
    // Inline search (on main screen)
    val inlineSearchQuery: String = "",
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
            try {
                val store = storeRepository.getStore() ?: return@launch
                storeId = store.id
                val suppliers = supplierRepository.getAll(storeId)
                val products = productRepository.getAll(storeId).filter { it.trackInventory && it.isActive }
                val poCode = inventoryRepository.generatePurchaseOrderCode(storeId)
                _state.update { it.copy(isLoading = false, suppliers = suppliers, products = products, purchaseCode = poCode) }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
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

    fun updateInlineSearch(query: String) {
        _state.update { it.copy(inlineSearchQuery = query) }
    }

    /** Try to add a product by barcode (from scan). Returns true if found. */
    fun addProductByBarcode(barcode: String): Boolean {
        val product = _state.value.products.find {
            it.barcode?.equals(barcode, ignoreCase = true) == true
        }
        if (product != null) {
            addProduct(product)
            return true
        }
        _state.update { it.copy(errorMessage = str(R.string.msg_barcode_not_found, barcode)) }
        return false
    }

    /** Products matching the inline search query */
    val inlineFilteredProducts: List<Product>
        get() {
            val query = _state.value.inlineSearchQuery.trim().lowercase()
            val products = _state.value.products
            if (query.isEmpty()) return emptyList()
            return products.filter {
                it.name.lowercase().contains(query) ||
                    it.sku.lowercase().contains(query) ||
                    (it.barcode?.lowercase()?.contains(query) == true)
            }.take(5) // limit suggestions
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
                val stock = inventoryRepository.getTotalStock(storeId, product.id, product.hasVariants)
                val cost = variant?.costPrice ?: product.costPrice
                _state.update { s ->
                    s.copy(lineItems = s.lineItems + PurchaseLineItem(
                        product = product,
                        variant = variant,
                        currentStock = stock,
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
        // Guard against double-submission
        if (_state.value.isSaving) return

        val items = _state.value.lineItems.filter { it.quantity > 0 }
        if (items.isEmpty()) {
            _state.update { it.copy(errorMessage = str(R.string.msg_purchase_no_items)) }
            return
        }

        viewModelScope.launch {
            try {
                _state.update { it.copy(isSaving = true, errorMessage = null) }
                val userId = appPreferences.currentUserId.first() ?: run {
                    _state.update { it.copy(isSaving = false, errorMessage = str(R.string.msg_user_not_found)) }
                    return@launch
                }
                val supplierId = _state.value.selectedSupplierId
                val poCode = _state.value.purchaseCode

                // Find supplier name
                val supplierName = supplierId?.let { sid ->
                    _state.value.suppliers.find { it.id == sid }?.name
                }

                // Calculate totals
                val totalAmount = items.sumOf { it.quantity * it.costPrice }
                val totalItems = items.size

                // Build PO items
                val poItems = items.map { item ->
                    PurchaseOrderItem(
                        id = UuidGenerator.generate(),
                        purchaseOrderId = "", // will be set by repository
                        productId = item.product.id,
                        variantId = item.variant?.id,
                        productName = item.displayName,
                        variantName = item.variant?.variantName,
                        quantity = item.quantity,
                        unitCost = item.costPrice,
                        totalCost = item.quantity * item.costPrice,
                    )
                }

                // Save purchase order record
                val poResult = inventoryRepository.savePurchaseOrder(
                    storeId = storeId,
                    code = poCode,
                    supplierId = supplierId,
                    supplierName = supplierName,
                    totalAmount = totalAmount,
                    totalItems = totalItems,
                    notes = _state.value.notes.ifBlank { null },
                    userId = userId,
                    items = poItems,
                )

                // Abort if PO record could not be saved
                if (poResult is Result.Error) {
                    _state.update { it.copy(isSaving = false, errorMessage = poResult.message) }
                    return@launch
                }

                val poId = (poResult as Result.Success).data

                // Adjust stock for each item
                for (item in items) {
                    val result = if (item.variant != null) {
                        inventoryRepository.adjustVariantStock(
                            storeId = storeId,
                            productId = item.product.id,
                            variantId = item.variant.id,
                            amount = item.quantity,
                            type = StockMovementType.PURCHASE_IN,
                            userId = userId,
                            referenceId = poId,
                            supplierId = supplierId,
                        )
                    } else {
                        inventoryRepository.adjustStock(
                            storeId = storeId,
                            productId = item.product.id,
                            amount = item.quantity,
                            type = StockMovementType.PURCHASE_IN,
                            userId = userId,
                            referenceId = poId,
                            supplierId = supplierId,
                        )
                    }
                    if (result is Result.Error) {
                        _state.update { it.copy(isSaving = false, errorMessage = str(R.string.msg_purchase_item_error, item.product.name, result.message)) }
                        return@launch
                    }
                    // Update cost price if changed (non-fatal)
                    if (item.costPrice > 0) {
                        try {
                            if (item.variant != null) {
                                if (item.costPrice != (item.variant.costPrice ?: 0.0)) {
                                    productRepository.updateVariant(item.variant.copy(costPrice = item.costPrice))
                                }
                            } else if (item.costPrice != item.product.costPrice) {
                                productRepository.update(item.product.copy(costPrice = item.costPrice))
                            }
                        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                            throw e // Re-throw cancellation so coroutine can be properly cleaned up
                        } catch (_: Exception) {
                            // Cost price update failed — non-fatal, continue with purchase
                        }
                    }
                }

                // All items processed — show success and refresh form for next purchase
                val totalQty = items.sumOf { it.quantity }.toLong()
                val totalProducts = items.size
                val successMsg = str(R.string.msg_purchase_success, totalQty.toDouble(), totalProducts)
                _state.update {
                    it.copy(
                        isSaving = false,
                        lineItems = emptyList(),
                        notes = "",
                        selectedSupplierId = null,
                        successMessage = successMsg,
                    )
                }
                // Refresh PO code and product list for the next purchase in this session
                loadData()
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e // Do not swallow coroutine cancellation
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = e.message ?: str(R.string.msg_purchase_no_items)) }
            }
        }
    }
}
