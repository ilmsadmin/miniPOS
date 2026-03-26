package com.minipos.ui.barcode

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.barcode.BarcodeGenerator
import com.minipos.core.barcode.BarcodePrintHelper
import com.minipos.core.receipt.ReceiptPrintHelper
import com.minipos.domain.model.Product
import com.minipos.domain.model.Result
import com.minipos.domain.repository.ProductRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BarcodeProductItem(
    val product: Product,
    val isSelected: Boolean = false,
    val hasBarcode: Boolean = false,
    val generatedBarcode: String? = null,
)

data class BarcodeScreenState(
    val products: List<BarcodeProductItem> = emptyList(),
    val filteredProducts: List<BarcodeProductItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val filterMode: BarcodeFilterMode = BarcodeFilterMode.NO_BARCODE,
    val selectedCount: Int = 0,

    // Generation
    val isGenerating: Boolean = false,
    val generatedBarcodes: Map<String, String> = emptyMap(), // productId -> barcode

    // Preview & Print
    val showPreview: Boolean = false,
    val previewBitmap: Bitmap? = null,
    val labelBitmaps: List<Bitmap> = emptyList(),
    val isPrinting: Boolean = false,

    // Bluetooth printer
    val showPrinterPicker: Boolean = false,
    val availablePrinters: List<BluetoothDevice> = emptyList(),

    // Messages
    val message: String? = null,
    val error: String? = null,
)

enum class BarcodeFilterMode(val label: String) {
    ALL("Tất cả"),
    NO_BARCODE("Chưa có mã"),
    HAS_BARCODE("Đã có mã"),
}

@HiltViewModel
class BarcodeViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BarcodeScreenState())
    val state: StateFlow<BarcodeScreenState> = _state

    private var storeId: String = ""
    private var storeCode: String = ""

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            storeCode = store.code
            val products = productRepository.getAll(storeId)
            val items = products.map { product ->
                BarcodeProductItem(
                    product = product,
                    hasBarcode = !product.barcode.isNullOrBlank(),
                )
            }
            _state.update {
                it.copy(
                    products = items,
                    isLoading = false,
                )
            }
            applyFilter()
        }
    }

    fun search(query: String) {
        _state.update { it.copy(searchQuery = query) }
        applyFilter()
    }

    fun setFilterMode(mode: BarcodeFilterMode) {
        _state.update { it.copy(filterMode = mode) }
        applyFilter()
    }

    private fun applyFilter() {
        val current = _state.value
        val query = current.searchQuery.lowercase()
        val filtered = current.products
            .filter { item ->
                when (current.filterMode) {
                    BarcodeFilterMode.ALL -> true
                    BarcodeFilterMode.NO_BARCODE -> !item.hasBarcode && item.generatedBarcode == null
                    BarcodeFilterMode.HAS_BARCODE -> item.hasBarcode || item.generatedBarcode != null
                }
            }
            .filter { item ->
                if (query.isBlank()) true
                else item.product.name.lowercase().contains(query) ||
                        item.product.sku.lowercase().contains(query) ||
                        (item.product.barcode?.lowercase()?.contains(query) == true)
            }
        _state.update { it.copy(filteredProducts = filtered) }
    }

    fun toggleProduct(productId: String) {
        _state.update { st ->
            val updated = st.products.map { item ->
                if (item.product.id == productId) item.copy(isSelected = !item.isSelected)
                else item
            }
            st.copy(
                products = updated,
                selectedCount = updated.count { it.isSelected },
            )
        }
        applyFilter()
    }

    fun selectAllFiltered() {
        val filteredIds = _state.value.filteredProducts.map { it.product.id }.toSet()
        _state.update { st ->
            val allSelected = st.filteredProducts.all { it.isSelected }
            val updated = st.products.map { item ->
                if (item.product.id in filteredIds) {
                    item.copy(isSelected = !allSelected)
                } else item
            }
            st.copy(
                products = updated,
                selectedCount = updated.count { it.isSelected },
            )
        }
        applyFilter()
    }

    fun deselectAll() {
        _state.update { st ->
            st.copy(
                products = st.products.map { it.copy(isSelected = false) },
                selectedCount = 0,
            )
        }
        applyFilter()
    }

    /**
     * Generate barcodes for all selected products that don't have one.
     */
    fun generateBarcodes() {
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, error = null) }

            try {
                val selected = _state.value.products.filter { it.isSelected && !it.hasBarcode && it.generatedBarcode == null }
                if (selected.isEmpty()) {
                    _state.update { it.copy(isGenerating = false, message = "Không có sản phẩm nào cần tạo mã vạch") }
                    return@launch
                }

                // Get current max barcode sequence
                val existingBarcodes = _state.value.products
                    .mapNotNull { it.product.barcode ?: it.generatedBarcode }
                    .filter { it.startsWith("2") && it.length == 13 }
                val maxSeq = existingBarcodes.maxOfOrNull { barcode ->
                    try { barcode.substring(3, 9).toInt() } catch (_: Exception) { 0 }
                } ?: 0

                val generatedMap = mutableMapOf<String, String>()
                var seq = maxSeq

                for (item in selected) {
                    seq++
                    val barcode = BarcodeGenerator.generateEan13(storeCode, seq)
                    generatedMap[item.product.id] = barcode

                    // Save barcode to database
                    val updatedProduct = item.product.copy(
                        barcode = barcode,
                        updatedAt = System.currentTimeMillis(),
                    )
                    productRepository.update(updatedProduct)
                }

                // Update state with generated barcodes
                _state.update { st ->
                    val updatedProducts = st.products.map { item ->
                        val newBarcode = generatedMap[item.product.id]
                        if (newBarcode != null) {
                            item.copy(
                                hasBarcode = true,
                                generatedBarcode = newBarcode,
                                product = item.product.copy(barcode = newBarcode),
                            )
                        } else item
                    }
                    st.copy(
                        products = updatedProducts,
                        isGenerating = false,
                        generatedBarcodes = st.generatedBarcodes + generatedMap,
                        message = "Đã tạo ${generatedMap.size} mã vạch",
                    )
                }
                applyFilter()
            } catch (e: Exception) {
                _state.update { it.copy(isGenerating = false, error = "Lỗi tạo mã vạch: ${e.message}") }
            }
        }
    }

    /**
     * Generate preview of all selected products' barcode labels.
     */
    fun showBarcodePreview() {
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true) }

            val selectedWithBarcode = _state.value.products.filter {
                it.isSelected && (it.hasBarcode || it.generatedBarcode != null)
            }

            if (selectedWithBarcode.isEmpty()) {
                _state.update { it.copy(isGenerating = false, error = "Vui lòng chọn sản phẩm có mã vạch") }
                return@launch
            }

            val labels = selectedWithBarcode.map { item ->
                val barcode = item.generatedBarcode ?: item.product.barcode!!
                BarcodeGenerator.generateLabelBitmap(
                    barcode = barcode,
                    productName = item.product.name,
                    sku = item.product.sku,
                )
            }

            val combined = BarcodeGenerator.combineLabelBitmaps(labels)

            _state.update {
                it.copy(
                    isGenerating = false,
                    showPreview = true,
                    previewBitmap = combined,
                    labelBitmaps = labels,
                )
            }
        }
    }

    fun dismissPreview() {
        _state.update { it.copy(showPreview = false, previewBitmap = null) }
    }

    /**
     * Share barcode labels as PDF.
     */
    fun shareAsPdf(context: Context) {
        val bitmap = _state.value.previewBitmap ?: return
        viewModelScope.launch {
            try {
                val file = BarcodePrintHelper.saveLabelsPdf(context, bitmap, "barcodes_${System.currentTimeMillis()}")
                BarcodePrintHelper.sharePdf(context, file)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Lỗi chia sẻ: ${e.message}") }
            }
        }
    }

    /**
     * Share barcode labels as image.
     */
    fun shareAsImage(context: Context) {
        val bitmap = _state.value.previewBitmap ?: return
        viewModelScope.launch {
            try {
                val file = BarcodePrintHelper.saveLabelsImage(context, bitmap, "barcodes_${System.currentTimeMillis()}")
                BarcodePrintHelper.shareImage(context, file)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Lỗi chia sẻ: ${e.message}") }
            }
        }
    }

    /**
     * Print via Bluetooth thermal printer.
     */
    fun showPrinterPicker(context: Context) {
        val devices = ReceiptPrintHelper.getAllPairedDevices(context)
        _state.update { it.copy(showPrinterPicker = true, availablePrinters = devices) }
    }

    fun dismissPrinterPicker() {
        _state.update { it.copy(showPrinterPicker = false) }
    }

    fun printViaBluetooth(device: BluetoothDevice) {
        val bitmap = _state.value.previewBitmap ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPrinting = true, showPrinterPicker = false) }
            val result = ReceiptPrintHelper.printBitmap(device, bitmap)
            _state.update {
                it.copy(
                    isPrinting = false,
                    message = if (result.isSuccess) "In thành công!" else null,
                    error = if (result.isFailure) result.exceptionOrNull()?.message else null,
                )
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, error = null) }
    }
}
