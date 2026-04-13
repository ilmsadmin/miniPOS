package com.minipos.ui.pos

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.core.utils.UuidGenerator
import com.minipos.domain.model.Category
import com.minipos.domain.model.Customer
import com.minipos.domain.model.Discount
import com.minipos.domain.model.Product
import com.minipos.domain.model.ProductVariant
import com.minipos.domain.repository.CategoryRepository
import com.minipos.domain.repository.CustomerRepository
import com.minipos.domain.repository.InventoryRepository
import com.minipos.domain.repository.ProductRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PosStep1State(
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val selectedCategory: Category? = null,
    val searchQuery: String = "",
    val stockError: String? = null,
    val showBarcodeScanner: Boolean = false,
    // Variant picker
    val showVariantPicker: Boolean = false,
    val variantPickerProduct: Product? = null,
    val variantPickerVariants: List<ProductVariant> = emptyList(),
    val variantStockMap: Map<String, Double> = emptyMap(), // variantId -> stock qty (only filled when per-variant inventory exists)
    val isSharedStock: Boolean = false, // true when stock is tracked at product-level, not per-variant
    // Customer picker (merged from Step 3)
    val recentCustomers: List<Customer> = emptyList(),
    val customerSearchResults: List<Customer> = emptyList(),
    val customerSearchQuery: String = "",
    val showCreateCustomerForm: Boolean = false,
    // Order discount (merged from Step 2)
    val showOrderDiscount: Boolean = false,
)

@HiltViewModel
class PosStep1ViewModel @Inject constructor(
    private val app: Application,
    private val storeRepository: StoreRepository,
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val inventoryRepository: InventoryRepository,
    val cartHolder: PosCartHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(PosStep1State())
    val state: StateFlow<PosStep1State> = _state

    init {
        loadData()
        loadRecentCustomers()
        // Observe stock errors from cartHolder
        viewModelScope.launch {
            cartHolder.stockError.collect { error ->
                _state.update { it.copy(stockError = error) }
            }
        }
    }

    /** Refresh stock cache from DB — call when returning from purchase/inventory screens */
    fun refreshStock() {
        viewModelScope.launch {
            try {
                cartHolder.refreshStock()
            } catch (_: Exception) { /* stock refresh failed, non-fatal */ }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch

            // Refresh store settings in cart holder (for tax calculation)
            cartHolder.refreshStoreSettings()

            // Observe categories reactively
            launch {
                try {
                    categoryRepository.observeCategories(store.id).collect { categories ->
                        _state.update { it.copy(categories = categories) }
                    }
                } catch (_: Exception) { /* prevent crash from reactive observer */ }
            }

            // Observe products reactively
            launch {
                try {
                    productRepository.observeProducts(store.id).collect { products ->
                        // Refresh stock cache whenever products change
                        try {
                            cartHolder.loadStock(store.id, products)
                        } catch (_: Exception) { /* stock cache refresh failed, non-fatal */ }
                        _state.update { current ->
                            val filteredProducts = when {
                                current.searchQuery.isNotBlank() -> products.filter { p ->
                                    p.name.contains(current.searchQuery, ignoreCase = true) ||
                                            p.sku.contains(current.searchQuery, ignoreCase = true) ||
                                            (p.barcode?.contains(current.searchQuery, ignoreCase = true) == true)
                                }
                                current.selectedCategory != null -> products.filter { p ->
                                    p.categoryId == current.selectedCategory.id
                                }
                                else -> products
                            }
                            current.copy(
                                allProducts = products,
                                products = filteredProducts,
                            )
                        }
                    }
                } catch (_: Exception) { /* prevent crash from reactive observer */ }
            }

            // Observe inventory changes reactively — auto-refresh stock when any inventory record is updated
            launch {
                try {
                    inventoryRepository.observeInventoryChanges(store.id).collect {
                        try {
                            cartHolder.refreshStock()
                        } catch (_: Exception) { /* stock cache refresh failed, non-fatal */ }
                    }
                } catch (_: Exception) { /* prevent crash from reactive observer */ }
            }
        }
    }

    fun selectCategory(category: Category?) {
        val products = if (category != null) {
            _state.value.allProducts.filter { it.categoryId == category.id }
        } else {
            _state.value.allProducts
        }
        _state.update { it.copy(selectedCategory = category, products = products, searchQuery = "") }
    }

    fun search(query: String) {
        val products = if (query.isBlank()) {
            _state.value.allProducts
        } else {
            _state.value.allProducts.filter { p ->
                p.name.contains(query, ignoreCase = true) ||
                        p.sku.contains(query, ignoreCase = true) ||
                        (p.barcode?.contains(query, ignoreCase = true) == true)
            }
        }
        _state.update { it.copy(searchQuery = query, products = products, selectedCategory = null) }
    }

    fun addToCart(product: Product) {
        if (product.hasVariants) {
            // Show variant picker
            viewModelScope.launch {
                val variants = productRepository.getVariants(product.id)
                if (variants.isEmpty()) {
                    // No variants defined yet, add product directly
                    cartHolder.addItem(product)
                } else {
                    // Load per-variant stock (fallback to product-level if no variant inventory exists)
                    val store = storeRepository.getStore()
                    var isShared = false
                    val stockMap = if (product.trackInventory && store != null) {
                        val variantStocks = variants.associate { v ->
                            val stock = inventoryRepository.getVariantStock(store.id, product.id, v.id)
                            v.id to stock?.quantity
                        }
                        // If no variant has its own inventory record, stock is shared at product-level
                        val hasAnyVariantStock = variantStocks.values.any { it != null }
                        if (hasAnyVariantStock) {
                            val map = variantStocks.mapValues { it.value ?: 0.0 }
                            // Also populate cart holder's variant stock cache for stock checking
                            cartHolder.loadVariantStock(store.id, product.id, variants.map { it.id })
                            map
                        } else {
                            isShared = true
                            emptyMap()
                        }
                    } else emptyMap()
                    _state.update {
                        it.copy(
                            showVariantPicker = true,
                            variantPickerProduct = product,
                            variantPickerVariants = variants,
                            variantStockMap = stockMap,
                            isSharedStock = isShared,
                        )
                    }
                }
            }
        } else {
            cartHolder.addItem(product)
        }
    }

    fun addVariantToCart(product: Product, variant: ProductVariant) {
        cartHolder.addItemWithVariant(product, variant)
        dismissVariantPicker()
    }

    fun dismissVariantPicker() {
        _state.update { it.copy(showVariantPicker = false, variantPickerProduct = null, variantPickerVariants = emptyList(), variantStockMap = emptyMap(), isSharedStock = false) }
    }

    fun clearStockError() {
        cartHolder.clearStockError()
        _state.update { it.copy(stockError = null) }
    }

    fun showBarcodeScanner() {
        _state.update { it.copy(showBarcodeScanner = true) }
    }

    fun dismissBarcodeScanner() {
        _state.update { it.copy(showBarcodeScanner = false) }
    }

    fun onBarcodeScanned(barcode: String) {
        _state.update { it.copy(showBarcodeScanner = false) }
        // First check product barcode
        val product = _state.value.allProducts.firstOrNull { it.barcode == barcode }
        if (product != null) {
            addToCart(product)
            return
        }
        // Then check variant barcodes
        viewModelScope.launch {
            val storeId = storeRepository.getStore()?.id ?: return@launch
            val variant = productRepository.getVariantByBarcode(storeId, barcode)
            if (variant != null) {
                val parentProduct = _state.value.allProducts.firstOrNull { it.id == variant.productId }
                if (parentProduct != null) {
                    cartHolder.addItemWithVariant(parentProduct, variant)
                    return@launch
                }
            }
            _state.update { it.copy(stockError = app.getString(R.string.msg_barcode_not_found, barcode)) }
        }
    }

    // ═══════════════════════════════════════
    // Customer management (merged from Step 3)
    // ═══════════════════════════════════════

    private fun loadRecentCustomers() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            val recent = customerRepository.getRecent(store.id, 20)
            _state.update { it.copy(recentCustomers = recent) }
        }
    }

    fun searchCustomer(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(customerSearchQuery = query) }
            if (query.isBlank()) return@launch
            val store = storeRepository.getStore() ?: return@launch
            val results = customerRepository.search(store.id, query)
            _state.update { it.copy(customerSearchResults = results) }
        }
    }

    fun selectCustomer(customer: Customer?) {
        cartHolder.setCustomer(customer)
    }

    fun toggleCreateCustomerForm() {
        _state.update { it.copy(showCreateCustomerForm = !it.showCreateCustomerForm) }
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
                _state.update { it.copy(showCreateCustomerForm = false) }
                loadRecentCustomers()
            }
        }
    }

    // ═══════════════════════════════════════
    // Cart management (merged from Step 2)
    // ═══════════════════════════════════════

    fun updateQuantity(index: Int, quantity: Double) {
        cartHolder.updateItemQuantity(index, quantity)
    }

    fun removeItem(index: Int) {
        cartHolder.removeItem(index)
    }

    fun clearCart() {
        cartHolder.clearCart()
    }

    fun showOrderDiscountDialog() { _state.update { it.copy(showOrderDiscount = true) } }
    fun dismissOrderDiscountDialog() { _state.update { it.copy(showOrderDiscount = false) } }

    fun setOrderDiscount(discount: Discount?) {
        cartHolder.setOrderDiscount(discount)
    }
}
