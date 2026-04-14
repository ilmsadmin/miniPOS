package com.minipos.ui.barcode

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.core.barcode.BarcodeGenerator
import com.minipos.core.barcode.BarcodePrintHelper
import com.minipos.core.receipt.ReceiptPrintHelper
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.Product
import com.minipos.domain.model.ProductVariant
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
    val variant: ProductVariant? = null,
    val isSelected: Boolean = false,
    val hasBarcode: Boolean = false,
    val generatedBarcode: String? = null,
) {
    /** Unique key: product id for standalone products, product:variant for variant items */
    val itemId: String get() = if (variant != null) "${product.id}:${variant.id}" else product.id

    /** Display name: includes variant name if applicable */
    val displayName: String get() = if (variant != null) "${product.name} › ${variant.variantName}" else product.name

    /** Display SKU */
    val displaySku: String get() = variant?.sku ?: product.sku

    /** Current barcode value */
    val currentBarcode: String? get() = generatedBarcode ?: variant?.barcode ?: product.barcode

    /** Display price */
    val displayPrice: Double get() = variant?.sellingPrice ?: product.sellingPrice
}

enum class BarcodeType(val label: String, @StringRes val descriptionRes: Int) {
    EAN_13("EAN-13", R.string.barcode_ean13_desc),
    QR_CODE("QR Code", R.string.barcode_qr_desc),
}

data class BarcodeScreenState(
    val products: List<BarcodeProductItem> = emptyList(),
    val filteredProducts: List<BarcodeProductItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val filterMode: BarcodeFilterMode = BarcodeFilterMode.NO_BARCODE,
    val selectedCount: Int = 0,

    // Barcode type & label options (new design)
    val barcodeType: BarcodeType = BarcodeType.EAN_13,
    val showProductName: Boolean = true,
    val showPrice: Boolean = true,
    val labelsPerProduct: Int = 4,

    // Product picker
    val showProductPicker: Boolean = false,
    val pickerSearchQuery: String = "",

    // Generation
    val isGenerating: Boolean = false,
    val generatedBarcodes: Map<String, String> = emptyMap(), // itemId -> barcode

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
) {
    /** Products that the user has selected (checked) */
    val selectedProducts: List<BarcodeProductItem> get() = products.filter { it.isSelected }

    /** Products available for the picker dialog (not yet selected) */
    val pickerProducts: List<BarcodeProductItem> get() {
        val query = pickerSearchQuery.lowercase()
        return products
            .filter { !it.isSelected }
            .filter {
                if (query.isBlank()) true
                else it.displayName.lowercase().contains(query) ||
                        it.displaySku.lowercase().contains(query)
            }
    }

    /** First selected product (for preview) */
    val previewProduct: BarcodeProductItem? get() = selectedProducts.firstOrNull()
}

enum class BarcodeFilterMode(@StringRes val labelRes: Int) {
    ALL(R.string.barcode_filter_all),
    NO_BARCODE(R.string.barcode_filter_no_barcode),
    HAS_BARCODE(R.string.barcode_filter_has_barcode),
}

@HiltViewModel
class BarcodeViewModel @Inject constructor(
    private val app: Application,
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
) : ViewModel() {

    private fun str(resId: Int) = app.getString(resId)
    private fun str(resId: Int, vararg args: Any) = app.getString(resId, *args)

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
            val items = mutableListOf<BarcodeProductItem>()

            for (product in products) {
                if (product.hasVariants) {
                    // Expand variants as separate barcode items
                    val variants = productRepository.getVariants(product.id)
                    if (variants.isNotEmpty()) {
                        for (variant in variants) {
                            items.add(
                                BarcodeProductItem(
                                    product = product,
                                    variant = variant,
                                    hasBarcode = !variant.barcode.isNullOrBlank(),
                                )
                            )
                        }
                    } else {
                        // Product marked hasVariants but no variants yet — show as regular product
                        items.add(
                            BarcodeProductItem(
                                product = product,
                                hasBarcode = !product.barcode.isNullOrBlank(),
                            )
                        )
                    }
                } else {
                    items.add(
                        BarcodeProductItem(
                            product = product,
                            hasBarcode = !product.barcode.isNullOrBlank(),
                        )
                    )
                }
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
                else item.displayName.lowercase().contains(query) ||
                        item.displaySku.lowercase().contains(query) ||
                        (item.currentBarcode?.lowercase()?.contains(query) == true)
            }
        _state.update { it.copy(filteredProducts = filtered) }
    }

    fun toggleProduct(itemId: String) {
        _state.update { st ->
            val updated = st.products.map { item ->
                if (item.itemId == itemId) item.copy(isSelected = !item.isSelected)
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
        val filteredIds = _state.value.filteredProducts.map { it.itemId }.toSet()
        _state.update { st ->
            val allSelected = st.filteredProducts.all { it.isSelected }
            val updated = st.products.map { item ->
                if (item.itemId in filteredIds) {
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
     * Generate barcodes for all selected products/variants that don't have one.
     */
    fun generateBarcodes() {
        viewModelScope.launch {
            generateBarcodesSync()
        }
    }

    private suspend fun generateBarcodesSync() {
            _state.update { it.copy(isGenerating = true, error = null) }

            try {
                val state = _state.value
                val selected = state.products.filter { it.isSelected && !it.hasBarcode && it.generatedBarcode == null }
                if (selected.isEmpty()) {
                    _state.update { it.copy(isGenerating = false, message = str(R.string.no_products_need_barcode)) }
                    return
                }

                val isQr = state.barcodeType == BarcodeType.QR_CODE

                // Get current max barcode sequence from all items (products + variants)
                val existingBarcodes = state.products
                    .mapNotNull { it.currentBarcode }
                    .filter { it.startsWith("2") && it.length == 13 }
                val maxSeq = existingBarcodes.maxOfOrNull { barcode ->
                    try { barcode.substring(3, 9).toInt() } catch (_: Exception) { 0 }
                } ?: 0

                val generatedMap = mutableMapOf<String, String>() // itemId -> barcode
                var seq = maxSeq

                for (item in selected) {
                    seq++
                    val barcode = if (isQr) {
                        BarcodeGenerator.generateEan13(storeCode, seq)
                    } else {
                        BarcodeGenerator.generateEan13(storeCode, seq)
                    }
                    generatedMap[item.itemId] = barcode

                    if (item.variant != null) {
                        val updatedVariant = item.variant.copy(
                            barcode = barcode,
                            updatedAt = System.currentTimeMillis(),
                        )
                        productRepository.updateVariant(updatedVariant)
                    } else {
                        val updatedProduct = item.product.copy(
                            barcode = barcode,
                            updatedAt = System.currentTimeMillis(),
                        )
                        productRepository.update(updatedProduct)
                    }
                }

                // Update state with generated barcodes
                _state.update { st ->
                    val updatedProducts = st.products.map { item ->
                        val newBarcode = generatedMap[item.itemId]
                        if (newBarcode != null) {
                            item.copy(
                                hasBarcode = true,
                                generatedBarcode = newBarcode,
                                product = if (item.variant == null) item.product.copy(barcode = newBarcode) else item.product,
                                variant = item.variant?.copy(barcode = newBarcode),
                            )
                        } else item
                    }
                    st.copy(
                        products = updatedProducts,
                        isGenerating = false,
                        generatedBarcodes = st.generatedBarcodes + generatedMap,
                        message = str(R.string.barcodes_generated, generatedMap.size),
                    )
                }
                applyFilter()
            } catch (e: Exception) {
                _state.update { it.copy(isGenerating = false, error = str(R.string.error_generate_barcode, e.message ?: "")) }
            }
    }

    /**
     * Generate preview of all selected products/variants barcode labels.
     */
    fun showBarcodePreview() {
        viewModelScope.launch {
            showBarcodePreviewSync()
        }
    }

    private suspend fun showBarcodePreviewSync() {
            _state.update { it.copy(isGenerating = true) }

            val currentState = _state.value
            val selectedWithBarcode = currentState.products.filter {
                it.isSelected && (it.hasBarcode || it.generatedBarcode != null)
            }

            if (selectedWithBarcode.isEmpty()) {
                _state.update { it.copy(isGenerating = false, error = str(R.string.select_products_with_barcode)) }
                return
            }

            val isQr = currentState.barcodeType == BarcodeType.QR_CODE

            val labels = selectedWithBarcode.flatMap { item ->
                val barcode = item.currentBarcode!!
                List(currentState.labelsPerProduct) {
                    BarcodeGenerator.generateLabelBitmap(
                        barcode = barcode,
                        productName = if (currentState.showProductName) item.displayName else "",
                        sku = item.displaySku,
                        price = if (currentState.showPrice) CurrencyFormatter.format(item.displayPrice) else "",
                        isQrCode = isQr,
                    )
                }
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

    fun dismissPreview() {
        _state.update { it.copy(showPreview = false, previewBitmap = null) }
    }

    /** Called after navigation to BarcodePreviewScreen so showPreview resets for next time */
    fun onPreviewNavigated() {
        _state.update { it.copy(showPreview = false) }
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
                _state.update { it.copy(error = str(R.string.error_sharing, e.message ?: "")) }
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
                _state.update { it.copy(error = str(R.string.error_sharing, e.message ?: "")) }
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

    fun printViaBluetooth(context: Context, device: BluetoothDevice) {
        val bitmap = _state.value.previewBitmap ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPrinting = true, showPrinterPicker = false) }
            val result = ReceiptPrintHelper.printBitmap(context, device, bitmap)
            _state.update {
                it.copy(
                    isPrinting = false,
                    message = if (result.isSuccess) str(R.string.print_success) else null,
                    error = if (result.isFailure) result.exceptionOrNull()?.message else null,
                )
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, error = null) }
    }

    // ═══════════════════════════════════════
    // NEW DESIGN: Product picker, barcode type, label options
    // ═══════════════════════════════════════

    fun showProductPicker() {
        _state.update { it.copy(showProductPicker = true, pickerSearchQuery = "") }
    }

    fun dismissProductPicker() {
        _state.update { it.copy(showProductPicker = false, pickerSearchQuery = "") }
    }

    fun searchPicker(query: String) {
        _state.update { it.copy(pickerSearchQuery = query) }
    }

    fun addProduct(itemId: String) {
        _state.update { st ->
            val updated = st.products.map { item ->
                if (item.itemId == itemId) item.copy(isSelected = true) else item
            }
            st.copy(
                products = updated,
                selectedCount = updated.count { it.isSelected },
            )
        }
        applyFilter()
    }

    fun removeProduct(itemId: String) {
        _state.update { st ->
            val updated = st.products.map { item ->
                if (item.itemId == itemId) item.copy(isSelected = false) else item
            }
            st.copy(
                products = updated,
                selectedCount = updated.count { it.isSelected },
            )
        }
        applyFilter()
    }

    fun setBarcodeType(type: BarcodeType) {
        _state.update { it.copy(barcodeType = type) }
    }

    fun toggleShowProductName() {
        _state.update { it.copy(showProductName = !it.showProductName) }
    }

    fun toggleShowPrice() {
        _state.update { it.copy(showPrice = !it.showPrice) }
    }

    fun changeLabelsPerProduct(delta: Int) {
        _state.update { st ->
            val newVal = (st.labelsPerProduct + delta).coerceIn(1, 100)
            st.copy(labelsPerProduct = newVal)
        }
    }

    fun saveBarcodesAsImage(context: Context) {
        val currentState = _state.value
        val selected = currentState.selectedProducts.filter { it.currentBarcode != null }
        if (selected.isEmpty()) {
            _state.update { it.copy(error = str(R.string.select_products_with_barcode)) }
            return
        }
        val isQr = currentState.barcodeType == BarcodeType.QR_CODE
        viewModelScope.launch {
            try {
                _state.update { it.copy(isGenerating = true) }
                val labels = selected.flatMap { item ->
                    val barcode = item.currentBarcode!!
                    List(currentState.labelsPerProduct) {
                        BarcodeGenerator.generateLabelBitmap(
                            barcode = barcode,
                            productName = if (currentState.showProductName) item.displayName else "",
                            sku = item.displaySku,
                            price = if (currentState.showPrice) CurrencyFormatter.format(item.displayPrice) else "",
                            isQrCode = isQr,
                        )
                    }
                }
                val combined = BarcodeGenerator.combineLabelBitmaps(labels)
                val file = BarcodePrintHelper.saveLabelsImage(context, combined, "barcodes_${System.currentTimeMillis()}")
                BarcodePrintHelper.shareImage(context, file)
                _state.update { it.copy(isGenerating = false, message = str(R.string.barcode_saved_image)) }
            } catch (e: Exception) {
                _state.update { it.copy(isGenerating = false, error = str(R.string.error_sharing, e.message ?: "")) }
            }
        }
    }

    fun printLabels(context: Context) {
        viewModelScope.launch {
            val selected = _state.value.selectedProducts
            if (selected.isEmpty()) {
                _state.update { it.copy(error = str(R.string.select_products_with_barcode)) }
                return@launch
            }

            // If any selected product has no barcode, generate first then preview
            val needGenerate = selected.filter { !it.hasBarcode && it.generatedBarcode == null }
            if (needGenerate.isNotEmpty()) {
                generateBarcodesSync()
            }

            showBarcodePreviewSync()
        }
    }
}
