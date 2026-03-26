package com.minipos.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.utils.UuidGenerator
import com.minipos.domain.model.Category
import com.minipos.domain.model.Product
import com.minipos.domain.model.Result
import com.minipos.domain.repository.CategoryRepository
import com.minipos.domain.repository.ProductRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: Category? = null,
    val showForm: Boolean = false,
    val editingProduct: Product? = null,
    val isLoading: Boolean = true,
    val showBarcodeScanner: Boolean = false,
)

data class ProductFormState(
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val categoryId: String? = null,
    val costPrice: String = "",
    val sellingPrice: String = "",
    val unit: String = "cái",
    val description: String = "",
    val minStock: String = "0",
    val trackInventory: Boolean = true,
    val taxRate: String = "0",
    val imagePath: String? = null,
    val additionalImages: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductListState())
    val state: StateFlow<ProductListState> = _state

    private val _formState = MutableStateFlow(ProductFormState())
    val formState: StateFlow<ProductFormState> = _formState

    private var storeId: String = ""

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val products = productRepository.getAll(storeId)
            val categories = categoryRepository.getAll(storeId)
            _state.update { it.copy(products = products, categories = categories, isLoading = false) }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(searchQuery = query) }
            val products = if (query.isBlank()) {
                productRepository.getAll(storeId)
            } else {
                productRepository.search(storeId, query)
            }
            _state.update { it.copy(products = products) }
        }
    }

    fun filterByCategory(category: Category?) {
        viewModelScope.launch {
            val products = if (category != null) {
                productRepository.getByCategory(storeId, category.id)
            } else {
                productRepository.getAll(storeId)
            }
            _state.update { it.copy(selectedCategory = category, products = products) }
        }
    }

    fun showCreateForm() {
        viewModelScope.launch {
            val sku = productRepository.generateSku(storeId)
            _formState.value = ProductFormState(sku = sku)
            _state.update { it.copy(showForm = true, editingProduct = null) }
        }
    }

    fun showEditForm(product: Product) {
        _formState.value = ProductFormState(
            name = product.name,
            sku = product.sku,
            barcode = product.barcode ?: "",
            categoryId = product.categoryId,
            costPrice = product.costPrice.toLong().toString(),
            sellingPrice = product.sellingPrice.toLong().toString(),
            unit = product.unit,
            description = product.description ?: "",
            minStock = product.minStock.toString(),
            trackInventory = product.trackInventory,
            taxRate = product.taxRate.toString(),
            imagePath = product.imagePath,
            additionalImages = product.additionalImages,
        )
        _state.update { it.copy(showForm = true, editingProduct = product) }
    }

    fun dismissForm() {
        _state.update { it.copy(showForm = false, editingProduct = null) }
    }

    fun updateFormField(block: ProductFormState.() -> ProductFormState) {
        _formState.update { it.block() }
    }

    fun saveProduct() {
        val form = _formState.value
        if (form.name.isBlank() || form.sellingPrice.isBlank()) {
            _formState.update { it.copy(error = "Vui lòng nhập tên và giá bán") }
            return
        }
        _formState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val existing = _state.value.editingProduct
            val product = Product(
                id = existing?.id ?: UuidGenerator.generate(),
                storeId = storeId,
                name = form.name,
                sku = form.sku,
                barcode = form.barcode.ifBlank { null },
                categoryId = form.categoryId,
                costPrice = form.costPrice.toDoubleOrNull() ?: 0.0,
                sellingPrice = form.sellingPrice.toDoubleOrNull() ?: 0.0,
                unit = form.unit,
                description = form.description.ifBlank { null },
                imagePath = form.imagePath,
                additionalImages = form.additionalImages,
                minStock = form.minStock.toIntOrNull() ?: 0,
                trackInventory = form.trackInventory,
                taxRate = form.taxRate.toDoubleOrNull() ?: 0.0,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            val result = if (existing != null) productRepository.update(product) else productRepository.create(product)
            when (result) {
                is Result.Success -> {
                    _formState.update { it.copy(isSaving = false) }
                    dismissForm()
                    loadData()
                }
                is Result.Error -> {
                    _formState.update { it.copy(isSaving = false, error = result.message) }
                }
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productRepository.delete(product.id)
            loadData()
        }
    }

    fun showBarcodeScanner() {
        _state.update { it.copy(showBarcodeScanner = true) }
    }

    fun dismissBarcodeScanner() {
        _state.update { it.copy(showBarcodeScanner = false) }
    }

    fun onBarcodeScanned(barcode: String) {
        _formState.update { it.copy(barcode = barcode) }
        _state.update { it.copy(showBarcodeScanner = false) }
    }
}
