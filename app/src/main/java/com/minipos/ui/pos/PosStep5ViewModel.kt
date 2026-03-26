package com.minipos.ui.pos

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class PosStep5State(
    val store: Store? = null,
    val orderDetail: OrderDetail? = null,
    val isPrinting: Boolean = false,
    val isSharing: Boolean = false,
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

    // ---- Print ----

    fun onPrintClick(context: Context) {
        if (_state.value.orderDetail == null) {
            _state.update { it.copy(message = "Không có dữ liệu đơn hàng để in") }
            return
        }
        if (!ReceiptPrintHelper.isBluetoothAvailable(context)) {
            _state.update { it.copy(message = "Bluetooth chưa được bật. Vui lòng bật Bluetooth và ghép nối máy in.") }
            return
        }
        val devices = ReceiptPrintHelper.getAllPairedDevices(context)
        if (devices.isEmpty()) {
            _state.update { it.copy(message = "Không tìm thấy thiết bị Bluetooth. Vui lòng ghép nối máy in.") }
            return
        }
        _state.update { it.copy(showPrinterDialog = true, pairedDevices = devices) }
    }

    fun dismissPrinterDialog() {
        _state.update { it.copy(showPrinterDialog = false) }
    }

    fun printToDevice(device: BluetoothDevice) {
        val detail = _state.value.orderDetail ?: return
        val store = _state.value.store ?: return

        _state.update { it.copy(showPrinterDialog = false, isPrinting = true) }

        viewModelScope.launch {
            val result = ReceiptPrintHelper.printReceipt(device, store, detail)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isPrinting = false, message = "In hóa đơn thành công!") }
                },
                onFailure = { e ->
                    _state.update { it.copy(isPrinting = false, message = e.message ?: "Lỗi in hóa đơn") }
                },
            )
        }
    }

    // ---- Share ----

    fun onShareClick() {
        if (_state.value.orderDetail == null) {
            _state.update { it.copy(message = "Không có dữ liệu đơn hàng để chia sẻ") }
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
                _state.update { it.copy(isSharing = false, message = "Lỗi tạo PDF: ${e.message}") }
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
