package com.minipos.ui.onboarding

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.sync.DiscoveredDevice
import com.minipos.core.sync.SyncStatus
import com.minipos.core.sync.WifiSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class JoinStep {
    ENTER_CODE,     // Step 1: nhập store code
    SCANNING,       // Step 2: đang quét Wi-Fi tìm thiết bị chủ
    FOUND,          // Step 3: đã tìm thấy — xác nhận kết nối
    SYNCING,        // Step 4: đang nhận dữ liệu
    SUCCESS,        // Step 5: xong — chuyển sang Login
    ERROR,          // Có lỗi
}

data class JoinStoreState(
    val step: JoinStep = JoinStep.ENTER_CODE,
    val storeCode: String = "",
    val storeCodeError: String? = null,
    val devices: List<DiscoveredDevice> = emptyList(),
    val selectedDevice: DiscoveredDevice? = null,
    val statusMessage: String = "",
    val errorMessage: String? = null,
)

@HiltViewModel
class JoinStoreViewModel @Inject constructor(
    private val app: Application,
    private val syncManager: WifiSyncManager,
) : ViewModel() {

    private val _state = MutableStateFlow(JoinStoreState())
    val state: StateFlow<JoinStoreState> = _state

    init {
        // Observe sync manager status to drive UI transitions
        viewModelScope.launch {
            syncManager.status.collect { status ->
                when (status) {
                    is SyncStatus.Idle -> { /* no-op */ }
                    is SyncStatus.Scanning -> {
                        _state.update { it.copy(step = JoinStep.SCANNING, statusMessage = "Đang tìm kiếm thiết bị…") }
                    }
                    is SyncStatus.Found -> {
                        // Only update device list; we stay in SCANNING until user picks one
                    }
                    is SyncStatus.Connecting -> {
                        _state.update { it.copy(step = JoinStep.SYNCING, statusMessage = "Đang kết nối tới ${status.deviceName}…") }
                    }
                    is SyncStatus.Syncing -> {
                        _state.update { it.copy(step = JoinStep.SYNCING, statusMessage = "Đang tải dữ liệu cửa hàng…") }
                    }
                    is SyncStatus.Success -> {
                        _state.update { it.copy(step = JoinStep.SUCCESS, statusMessage = "Tham gia thành công!", errorMessage = null) }
                    }
                    is SyncStatus.Error -> {
                        _state.update { it.copy(step = JoinStep.ERROR, errorMessage = status.message) }
                    }
                }
            }
        }

        // Observe discovered devices list
        viewModelScope.launch {
            syncManager.discoveredDevices.collect { devices ->
                val storeCode = _state.value.storeCode
                // Filter to only devices that match the entered store code
                val filtered = devices.filter { it.storeCode.uppercase() == storeCode.uppercase() }
                _state.update { it.copy(devices = filtered) }
                if (filtered.isNotEmpty() && _state.value.step == JoinStep.SCANNING) {
                    _state.update { it.copy(step = JoinStep.FOUND) }
                }
            }
        }
    }

    fun onStoreCodeChanged(value: String) {
        _state.update { it.copy(storeCode = value.uppercase().take(20), storeCodeError = null) }
    }

    fun startScan() {
        val code = _state.value.storeCode.trim()
        if (code.length < 4) {
            _state.update { it.copy(storeCodeError = "Mã cửa hàng phải có ít nhất 4 ký tự") }
            return
        }
        syncManager.stopDiscovery()
        syncManager.startDiscovery(code)
        _state.update { it.copy(step = JoinStep.SCANNING, devices = emptyList(), errorMessage = null) }
    }

    fun retryScanning() {
        syncManager.resetStatus()
        startScan()
    }

    fun connectToDevice(device: DiscoveredDevice) {
        val storeCode = _state.value.storeCode.trim()
        _state.update { it.copy(selectedDevice = device, step = JoinStep.SYNCING, errorMessage = null) }
        syncManager.stopDiscovery()
        syncManager.joinStore(device, storeCode)
    }

    fun backToEnterCode() {
        syncManager.stopDiscovery()
        syncManager.resetStatus()
        _state.update { it.copy(step = JoinStep.ENTER_CODE, devices = emptyList(), errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        syncManager.stopDiscovery()
    }
}
