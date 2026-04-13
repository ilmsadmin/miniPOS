package com.minipos.ui.sync

import android.app.Application
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.core.sync.DiscoveredDevice
import com.minipos.core.sync.SyncStatus
import com.minipos.core.sync.WifiSyncManager
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WifiSyncUiState(
    val storeName: String = "",
    val storeCode: String = "",
    val deviceName: String = "",
    val isServerRunning: Boolean = false,
    val status: SyncStatus = SyncStatus.Idle,
    val devices: List<DiscoveredDevice> = emptyList(),
    val lastSyncAt: Long? = null,
    val confirmDevice: DiscoveredDevice? = null,
)

@HiltViewModel
class WifiSyncViewModel @Inject constructor(
    private val app: Application,
    private val storeRepository: StoreRepository,
    private val syncManager: WifiSyncManager,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(WifiSyncUiState())
    val state: StateFlow<WifiSyncUiState> = _state

    init {
        viewModelScope.launch {
            val store = storeRepository.getStore()
            val deviceName = getDeviceDisplayName()
            _state.update {
                it.copy(
                    storeName = store?.name ?: "",
                    storeCode = store?.code ?: "",
                    deviceName = deviceName,
                )
            }
        }

        // Observe sync manager state
        viewModelScope.launch {
            syncManager.status.collect { status ->
                _state.update { it.copy(status = status) }
                if (status is SyncStatus.Success) {
                    _state.update { it.copy(lastSyncAt = status.updatedAt) }
                }
            }
        }
        viewModelScope.launch {
            syncManager.discoveredDevices.collect { devices ->
                _state.update { it.copy(devices = devices) }
            }
        }
    }

    private fun getDeviceDisplayName(): String {
        return try {
            android.os.Build.MODEL
        } catch (_: Exception) {
            "Device"
        }
    }

    fun startServer() {
        val storeCode = _state.value.storeCode
        val deviceName = _state.value.deviceName
        if (storeCode.isBlank()) return
        syncManager.startServer(storeCode, deviceName)
        _state.update { it.copy(isServerRunning = true) }
    }

    fun stopServer() {
        syncManager.stopServer()
        _state.update { it.copy(isServerRunning = false) }
    }

    fun startScan() {
        val storeCode = _state.value.storeCode
        if (storeCode.isBlank()) return
        syncManager.startDiscovery(storeCode)
    }

    fun stopScan() {
        syncManager.stopDiscovery()
    }

    fun requestSync(device: DiscoveredDevice) {
        _state.update { it.copy(confirmDevice = device) }
    }

    fun confirmSync() {
        val device = _state.value.confirmDevice ?: return
        _state.update { it.copy(confirmDevice = null) }
        syncManager.syncWithDevice(device)
    }

    fun dismissConfirm() {
        _state.update { it.copy(confirmDevice = null) }
    }

    fun resetStatus() {
        syncManager.resetStatus()
    }

    override fun onCleared() {
        super.onCleared()
        syncManager.stopDiscovery()
    }
}
