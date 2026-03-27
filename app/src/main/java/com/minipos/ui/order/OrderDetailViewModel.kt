package com.minipos.ui.order

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.core.receipt.ReceiptBitmapGenerator
import com.minipos.core.receipt.ReceiptGenerator
import com.minipos.core.receipt.ReceiptPdfGenerator
import com.minipos.core.receipt.ReceiptPrintHelper
import com.minipos.core.receipt.ReceiptShareHelper
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

data class OrderDetailState(
    val detail: OrderDetail? = null,
    val store: Store? = null,
    val isLoading: Boolean = true,
    val isPrinting: Boolean = false,
    val isSharing: Boolean = false,
    val showReceiptPreview: Boolean = false,
    val showPrinterDialog: Boolean = false,
    val showShareOptions: Boolean = false,
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val storeRepository: StoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OrderDetailState())
    val state: StateFlow<OrderDetailState> = _state

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            val detail = orderRepository.getOrderDetail(orderId)
            val store = storeRepository.getStore()
            _state.update { it.copy(detail = detail, store = store, isLoading = false) }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
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
        if (!ReceiptPrintHelper.isBluetoothAvailable(context)) {
            _state.update { it.copy(message = context.getString(R.string.bluetooth_not_enabled)) }
            return
        }
        val devices = ReceiptPrintHelper.getAllPairedDevices(context)
        if (devices.isEmpty()) {
            _state.update { it.copy(message = context.getString(R.string.no_bluetooth_devices_found)) }
            return
        }
        _state.update { it.copy(showPrinterDialog = true, pairedDevices = devices) }
    }

    fun dismissPrinterDialog() {
        _state.update { it.copy(showPrinterDialog = false) }
    }

    fun printToDevice(context: Context, device: BluetoothDevice) {
        val detail = _state.value.detail ?: return
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

    fun onShareClick() {
        _state.update { it.copy(showShareOptions = true) }
    }

    fun dismissShareOptions() {
        _state.update { it.copy(showShareOptions = false) }
    }

    fun shareAsPdf(context: Context) {
        val detail = _state.value.detail ?: return
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
        val detail = _state.value.detail ?: return
        val store = _state.value.store ?: return

        _state.update { it.copy(showShareOptions = false, isSharing = false) }
        ReceiptShareHelper.shareText(context, store, detail)
    }
}
