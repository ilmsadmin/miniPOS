package com.minipos.ui.product

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.core.barcode.BarcodeGenerator
import com.minipos.core.utils.UuidGenerator
import com.minipos.domain.model.Category
import com.minipos.domain.model.Product
import com.minipos.domain.model.ProductVariant
import com.minipos.domain.model.Result
import com.minipos.domain.repository.CategoryRepository
import com.minipos.domain.repository.InventoryRepository
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
    val stockMap: Map<String, Double> = emptyMap(),
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: Category? = null,
    val showForm: Boolean = false,
    val editingProduct: Product? = null,
    val isLoading: Boolean = true,
    val showBarcodeScanner: Boolean = false,
    // Variants
    val variants: List<ProductVariant> = emptyList(),
    val showVariantForm: Boolean = false,
    val editingVariant: ProductVariant? = null,
)

data class ProductFormState(
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val categoryId: String? = null,
    val costPrice: String = "",
    val sellingPrice: String = "",
    val unit: String = "pcs",
    val description: String = "",
    val minStock: String = "0",
    val trackInventory: Boolean = true,
    val taxRate: String = "0",
    val imagePath: String? = null,
    val additionalImages: List<String> = emptyList(),
    val hasVariants: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
)

data class VariantFormState(
    val variantName: String = "",
    val sku: String = "",
    val barcode: String = "",
    val costPrice: String = "",
    val sellingPrice: String = "",
    val attributes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val app: Application,
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val inventoryRepository: InventoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductListState())
    val state: StateFlow<ProductListState> = _state

    private val _formState = MutableStateFlow(ProductFormState())
    val formState: StateFlow<ProductFormState> = _formState

    private val _variantFormState = MutableStateFlow(VariantFormState())
    val variantFormState: StateFlow<VariantFormState> = _variantFormState

    private var storeId: String = ""
    private var storeCode: String = ""

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            storeCode = store.code
            val products = productRepository.getAll(storeId)
            val categories = categoryRepository.getAll(storeId)
            val stockMap = products.associate { p ->
                p.id to (inventoryRepository.getStock(storeId, p.id)?.quantity ?: 0.0)
            }
            _state.update { it.copy(products = products, stockMap = stockMap, categories = categories, isLoading = false) }
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
            val stockMap = products.associate { p ->
                p.id to (inventoryRepository.getStock(storeId, p.id)?.quantity ?: 0.0)
            }
            _state.update { it.copy(products = products, stockMap = stockMap) }
        }
    }

    fun filterByCategory(category: Category?) {
        viewModelScope.launch {
            val products = if (category != null) {
                productRepository.getByCategory(storeId, category.id)
            } else {
                productRepository.getAll(storeId)
            }
            val stockMap = products.associate { p ->
                p.id to (inventoryRepository.getStock(storeId, p.id)?.quantity ?: 0.0)
            }
            _state.update { it.copy(selectedCategory = category, products = products, stockMap = stockMap) }
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
            hasVariants = product.hasVariants,
        )
        _state.update { it.copy(showForm = true, editingProduct = product) }
        loadVariants(product.id)
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
            _formState.update { it.copy(error = app.getString(R.string.error_name_price_required)) }
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
                hasVariants = form.hasVariants,
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

    fun generateBarcode() {
        viewModelScope.launch {
            // Get all existing barcodes to determine next sequence
            val allProducts = productRepository.getAll(storeId)
            val existingBarcodes = allProducts
                .mapNotNull { it.barcode }
                .filter { it.startsWith("2") && it.length == 13 }
            val maxSeq = existingBarcodes.maxOfOrNull { barcode ->
                try { barcode.substring(3, 9).toInt() } catch (_: Exception) { 0 }
            } ?: 0

            val newBarcode = BarcodeGenerator.generateEan13(storeCode, maxSeq + 1)
            _formState.update { it.copy(barcode = newBarcode) }
        }
    }

    // ---- Variant management ----

    private fun loadVariants(productId: String) {
        viewModelScope.launch {
            val variants = productRepository.getVariants(productId)
            _state.update { it.copy(variants = variants) }
        }
    }

    fun showCreateVariantForm() {
        _variantFormState.value = VariantFormState()
        _state.update { it.copy(showVariantForm = true, editingVariant = null) }
    }

    fun showEditVariantForm(variant: ProductVariant) {
        _variantFormState.value = VariantFormState(
            variantName = variant.variantName,
            sku = variant.sku,
            barcode = variant.barcode ?: "",
            costPrice = variant.costPrice?.toLong()?.toString() ?: "",
            sellingPrice = variant.sellingPrice?.toLong()?.toString() ?: "",
            attributes = variant.attributes.takeIf { it != "{}" } ?: "",
        )
        _state.update { it.copy(showVariantForm = true, editingVariant = variant) }
    }

    fun dismissVariantForm() {
        _state.update { it.copy(showVariantForm = false, editingVariant = null) }
    }

    fun updateVariantFormField(block: VariantFormState.() -> VariantFormState) {
        _variantFormState.update { it.block() }
    }

    fun saveVariant() {
        val form = _variantFormState.value
        val productId = _state.value.editingProduct?.id ?: return
        if (form.variantName.isBlank()) {
            _variantFormState.update { it.copy(error = app.getString(R.string.error_variant_name_required)) }
            return
        }
        _variantFormState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val existing = _state.value.editingVariant
            val variant = ProductVariant(
                id = existing?.id ?: UuidGenerator.generate(),
                storeId = storeId,
                productId = productId,
                variantName = form.variantName,
                sku = form.sku.ifBlank { "${_formState.value.sku}-${form.variantName}" },
                barcode = form.barcode.ifBlank { null },
                costPrice = form.costPrice.toDoubleOrNull(),
                sellingPrice = form.sellingPrice.toDoubleOrNull(),
                attributes = form.attributes.ifBlank { "{}" },
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            val result = if (existing != null) productRepository.updateVariant(variant) else productRepository.createVariant(variant)
            when (result) {
                is Result.Success -> {
                    _variantFormState.update { it.copy(isSaving = false) }
                    dismissVariantForm()
                    loadVariants(productId)
                }
                is Result.Error -> {
                    _variantFormState.update { it.copy(isSaving = false, error = result.message) }
                }
            }
        }
    }

    fun deleteVariant(variant: ProductVariant) {
        viewModelScope.launch {
            productRepository.deleteVariant(variant.id)
            loadVariants(variant.productId)
        }
    }
}
