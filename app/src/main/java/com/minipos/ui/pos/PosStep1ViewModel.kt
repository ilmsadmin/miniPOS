package com.minipos.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.model.Category
import com.minipos.domain.model.Product
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
)

@HiltViewModel
class PosStep1ViewModel @Inject constructor(
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
        cartHolder.addItem(product)
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
        val product = _state.value.allProducts.firstOrNull { it.barcode == barcode }
        if (product != null) {
            cartHolder.addItem(product)
        } else {
            _state.update { it.copy(stockError = "Không tìm thấy sản phẩm với mã: $barcode") }
        }
    }
}
