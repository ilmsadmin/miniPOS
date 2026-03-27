package com.minipos.ui.pos

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.domain.model.Category
import com.minipos.domain.model.Product
import com.minipos.domain.model.ProductVariant
import com.minipos.domain.repository.CategoryRepository
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
)

@HiltViewModel
class PosStep1ViewModel @Inject constructor(
    private val app: Application,
    private val storeRepository: StoreRepository,
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
    val cartHolder: PosCartHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(PosStep1State())
    val state: StateFlow<PosStep1State> = _state

    init {
        loadData()
        // Observe stock errors from cartHolder
        viewModelScope.launch {
            cartHolder.stockError.collect { error ->
                _state.update { it.copy(stockError = error) }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch

            // Refresh store settings in cart holder (for tax calculation)
            cartHolder.refreshStoreSettings()

            // Observe categories reactively
            launch {
                categoryRepository.observeCategories(store.id).collect { categories ->
                    _state.update { it.copy(categories = categories) }
                }
            }

            // Observe products reactively
            launch {
                productRepository.observeProducts(store.id).collect { products ->
                    // Refresh stock cache whenever products change
                    cartHolder.loadStock(store.id, products)
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
            cartHolder.addItem(product)
        }
    }

    fun addVariantToCart(product: Product, variant: ProductVariant) {
        cartHolder.addItemWithVariant(product, variant)
        dismissVariantPicker()
    }

    fun dismissVariantPicker() {
        _state.update { it.copy(showVariantPicker = false, variantPickerProduct = null, variantPickerVariants = emptyList()) }
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
}
