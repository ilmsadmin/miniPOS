package com.minipos.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.model.Product
import com.minipos.domain.repository.InventoryRepository
import com.minipos.domain.repository.ProductRepository
import com.minipos.domain.repository.StoreRepository
import com.minipos.ui.pos.PosCartHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannedProduct(
    val product: Product,
    val currentStock: Double = 0.0,
    val quantity: Int = 1,
    val isSelected: Boolean = true,
)

data class ScanToPosState(
    val isScanning: Boolean = true,
    val scannedProducts: List<ScannedProduct> = emptyList(),
    val lastScannedBarcode: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isTransferring: Boolean = false,
)

@HiltViewModel
class ScanToPosViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    val cartHolder: PosCartHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanToPosState())
    val state: StateFlow<ScanToPosState> = _state

    private var storeId: String? = null

    init {
        viewModelScope.launch {
            storeId = storeRepository.getStore()?.id
        }
    }

    fun onBarcodeScanned(barcode: String) {
        // Avoid duplicate scan of same barcode in quick succession
        if (barcode == _state.value.lastScannedBarcode &&
            _state.value.scannedProducts.any { it.product.barcode == barcode }
        ) {
            // Already scanned – just increment quantity
            incrementExisting(barcode)
            return
        }

        viewModelScope.launch {
            val sid = storeId ?: run {
                _state.update { it.copy(errorMessage = "Chưa có thông tin cửa hàng") }
                return@launch
            }

            val product = productRepository.getByBarcode(sid, barcode)
            if (product == null) {
                _state.update {
                    it.copy(
                        errorMessage = "Không tìm thấy sản phẩm với mã: $barcode",
                        lastScannedBarcode = barcode,
                        isScanning = false,
                    )
                }
                return@launch
            }

            // Check if already in list
            val existing = _state.value.scannedProducts.indexOfFirst { it.product.id == product.id }
            if (existing >= 0) {
                val items = _state.value.scannedProducts.toMutableList()
                val item = items[existing]
                items[existing] = item.copy(quantity = item.quantity + 1)
                _state.update {
                    it.copy(
                        scannedProducts = items,
                        lastScannedBarcode = barcode,
                        isScanning = false,
                        successMessage = "+1 ${product.name}",
                        errorMessage = null,
                    )
                }
            } else {
                // Get stock info
                val stock = if (product.trackInventory) {
                    inventoryRepository.getStock(sid, product.id)?.quantity ?: 0.0
                } else {
                    Double.MAX_VALUE
                }

                _state.update {
                    it.copy(
                        scannedProducts = it.scannedProducts + ScannedProduct(
                            product = product,
                            currentStock = stock,
                            quantity = 1,
                            isSelected = true,
                        ),
                        lastScannedBarcode = barcode,
                        isScanning = false,
                        successMessage = "Đã thêm: ${product.name}",
                        errorMessage = null,
                    )
                }
            }
        }
    }

    private fun incrementExisting(barcode: String) {
        val items = _state.value.scannedProducts.toMutableList()
        val idx = items.indexOfFirst { it.product.barcode == barcode }
        if (idx >= 0) {
            val item = items[idx]
            items[idx] = item.copy(quantity = item.quantity + 1)
            _state.update {
                it.copy(
                    scannedProducts = items,
                    successMessage = "+1 ${item.product.name}",
                )
            }
        }
    }

    fun resumeScanning() {
        _state.update { it.copy(isScanning = true, errorMessage = null, successMessage = null) }
    }

    fun stopScanning() {
        _state.update { it.copy(isScanning = false, errorMessage = null, successMessage = null) }
    }

    fun toggleSelection(index: Int) {
        val items = _state.value.scannedProducts.toMutableList()
        if (index in items.indices) {
            items[index] = items[index].copy(isSelected = !items[index].isSelected)
            _state.update { it.copy(scannedProducts = items) }
        }
    }

    fun updateQuantity(index: Int, quantity: Int) {
        if (quantity < 1) return
        val items = _state.value.scannedProducts.toMutableList()
        if (index in items.indices) {
            items[index] = items[index].copy(quantity = quantity)
            _state.update { it.copy(scannedProducts = items) }
        }
    }

    fun removeProduct(index: Int) {
        val items = _state.value.scannedProducts.toMutableList()
        if (index in items.indices) {
            items.removeAt(index)
            _state.update { it.copy(scannedProducts = items) }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _state.update { it.copy(successMessage = null) }
    }

    /**
     * Transfer selected products to POS cart and navigate to POS Step1.
     */
    fun transferToPos(): Boolean {
        val selected = _state.value.scannedProducts.filter { it.isSelected }
        if (selected.isEmpty()) {
            _state.update { it.copy(errorMessage = "Chưa chọn sản phẩm nào") }
            return false
        }

        // Clear current cart before adding
        cartHolder.clearCart()

        viewModelScope.launch {
            val sid = storeId ?: return@launch
            // Load stock into cart holder
            val allProducts = selected.map { it.product }
            cartHolder.loadStock(sid, allProducts)

            // Add each selected product with the specified quantity
            for (item in selected) {
                repeat(item.quantity) {
                    cartHolder.addItem(item.product)
                }
            }
        }

        return true
    }

    val selectedCount: Int
        get() = _state.value.scannedProducts.count { it.isSelected }

    val selectedTotalQuantity: Int
        get() = _state.value.scannedProducts.filter { it.isSelected }.sumOf { it.quantity }

    val selectedTotalAmount: Double
        get() = _state.value.scannedProducts
            .filter { it.isSelected }
            .sumOf { it.product.sellingPrice * it.quantity }
}
