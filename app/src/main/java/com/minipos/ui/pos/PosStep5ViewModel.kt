package com.minipos.ui.pos

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.core.receipt.ReceiptPrintHelper
import com.minipos.core.receipt.ReceiptShareHelper
import com.minipos.core.receipt.ReceiptSystemPrintHelper
import com.minipos.domain.model.OrderDetail
import com.minipos.domain.model.Store
import com.minipos.domain.repository.OrderRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PosStep5State(
    val store: Store? = null,
    val orderDetail: OrderDetail? = null,
    val isPrinting: Boolean = false,
    val isSharing: Boolean = false,
    val showReceiptPreview: Boolean = false,
    val showPrinterDialog: Boolean = false,
    val showShareOptions: Boolean = false,
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class PosStep5ViewModel @Inject constructor(
    private val cartHolder: PosCartHolder,
    private val orderRepository: OrderRepository,
    private val storeRepository: StoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PosStep5State())
    val state: StateFlow<PosStep5State> = _state

    // Store the last created order ID from cart holder
    private var lastOrderId: String? = null

    init {
        loadStoreAndOrder()
    }

    private fun loadStoreAndOrder() {
        viewModelScope.launch {
            val store = storeRepository.getStore()
            _state.update { it.copy(store = store) }

            // Try to load the last order (from cartHolder's lastOrderId)
            val orderId = cartHolder.lastOrderId
            if (orderId != null) {
                lastOrderId = orderId
                val detail = orderRepository.getOrderDetail(orderId)
                _state.update { it.copy(orderDetail = detail) }

                // Auto print if store settings say so
                if (store?.settings?.autoPrintReceipt == true && detail != null) {
                    // Auto-print is available but requires user to have paired printer
                    // We skip auto-print to avoid errors; user can tap "In hóa đơn"
                }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun clearCartAndNavigate(navigate: () -> Unit) {
        cartHolder.clearCart()
        navigate()
    }

    // ---- Receipt Preview ----

    fun showReceiptPreview() {
        _state.update { it.copy(showReceiptPreview = true) }
    }

    fun dismissReceiptPreview() {
        _state.update { it.copy(showReceiptPreview = false) }
    }

    // ---- Print ----

    fun onPrintClick(context: Context) {
        val detail = _state.value.orderDetail
        val store = _state.value.store
        if (detail == null || store == null) {
            _state.update { it.copy(message = context.getString(R.string.no_order_data_print)) }
            return
        }
        // Use Android's built-in Print Framework — supports WiFi, Bluetooth, Cloud printers
        _state.update { it.copy(isPrinting = true) }
        viewModelScope.launch {
            try {
                ReceiptSystemPrintHelper.printViaSystemDialog(context, store, detail)
                _state.update { it.copy(isPrinting = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isPrinting = false, message = context.getString(R.string.error_print_receipt)) }
            }
        }
    }

    fun dismissPrinterDialog() {
        _state.update { it.copy(showPrinterDialog = false) }
    }

    fun printToDevice(context: Context, device: BluetoothDevice) {
        val detail = _state.value.orderDetail ?: return
        val store = _state.value.store ?: return

        _state.update { it.copy(showPrinterDialog = false, isPrinting = true) }

        viewModelScope.launch {
            val result = ReceiptPrintHelper.printReceipt(context, device, store, detail)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isPrinting = false, message = context.getString(R.string.print_receipt_success)) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isPrinting = false, message = e.message ?: context.getString(R.string.error_print_receipt)) }
                },
            )
        }
    }

    // ---- Share ----

    fun onShareClick(context: Context) {
        if (_state.value.orderDetail == null) {
            _state.update { it.copy(message = context.getString(R.string.no_order_data_share)) }
            return
        }
        _state.update { it.copy(showShareOptions = true) }
    }

    fun dismissShareOptions() {
        _state.update { it.copy(showShareOptions = false) }
    }

    fun shareAsPdf(context: Context) {
        val detail = _state.value.orderDetail ?: return
        val store = _state.value.store ?: return

        _state.update { it.copy(showShareOptions = false, isSharing = true) }

        viewModelScope.launch {
            try {
                ReceiptShareHelper.shareReceipt(context, store, detail, asPdf = true)
                _state.update { it.copy(isSharing = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isSharing = false, message = context.getString(R.string.error_create_pdf, e.message ?: "")) }
            }
        }
    }

    fun shareAsText(context: Context) {
        val detail = _state.value.orderDetail ?: return
        val store = _state.value.store ?: return

        _state.update { it.copy(showShareOptions = false) }
        ReceiptShareHelper.shareText(context, store, detail)
    }
}
