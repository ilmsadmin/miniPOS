package com.minipos.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.domain.model.Result
import com.minipos.domain.model.Store
import com.minipos.domain.model.StoreSettings
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoreSettingsState(
    val isLoading: Boolean = true,
    val store: Store? = null,

    // Store info
    val storeName: String = "",
    val storeAddress: String = "",
    val storePhone: String = "",
    val storeEmail: String = "",

    // Tax & Currency
    val taxEnabled: Boolean = false,
    val defaultTaxRate: Double = 0.0,
    val currency: String = "VNĐ",

    // Receipt
    val autoPrintReceipt: Boolean = true,
    val showLogoOnReceipt: Boolean = true,
    val receiptThankYou: String = "",

    // Business rules
    val allowDebt: Boolean = true,
    val lowStockAlert: Boolean = true,
    val salesSound: Boolean = false,
    val defaultLowStockLevel: String = "20",

    // Operational hours
    val openTime: String = "06:00",
    val closeTime: String = "22:00",

    // UI state
    val nameError: Boolean = false,
    val message: String? = null,
    val isSaved: Boolean = false,
)

@HiltViewModel
class StoreSettingsViewModel @Inject constructor(
    private val app: Application,
    private val storeRepository: StoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StoreSettingsState())
    val state: StateFlow<StoreSettingsState> = _state

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore()
            if (store != null) {
                val settings = store.settings
                _state.update {
                    it.copy(
                        isLoading = false,
                        store = store,
                        storeName = store.name,
                        storeAddress = store.address ?: "",
                        storePhone = store.phone ?: "",
                        storeEmail = "", // email not in Store model yet
                        taxEnabled = settings.taxEnabled,
                        defaultTaxRate = settings.defaultTaxRate,
                        currency = store.currency,
                        autoPrintReceipt = settings.autoPrintReceipt,
                        showLogoOnReceipt = settings.showLogoOnReceipt,
                        receiptThankYou = settings.receiptThankYou,
                        allowDebt = settings.allowDebt,
                        lowStockAlert = settings.lowStockAlert,
                        salesSound = settings.salesSound,
                        defaultLowStockLevel = settings.defaultLowStockLevel.toString(),
                        openTime = settings.openTime,
                        closeTime = settings.closeTime,
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Field updaters ──

    fun updateStoreName(value: String) {
        _state.update { it.copy(storeName = value, nameError = false) }
    }

    fun updateStoreAddress(value: String) {
        _state.update { it.copy(storeAddress = value) }
    }

    fun updateStorePhone(value: String) {
        _state.update { it.copy(storePhone = value) }
    }

    fun updateStoreEmail(value: String) {
        _state.update { it.copy(storeEmail = value) }
    }

    fun updateTaxRate(rate: Double) {
        _state.update {
            it.copy(
                defaultTaxRate = rate,
                taxEnabled = rate > 0,
            )
        }
    }

    fun toggleAutoPrintReceipt() {
        _state.update { it.copy(autoPrintReceipt = !it.autoPrintReceipt) }
    }

    fun toggleShowLogoOnReceipt() {
        _state.update { it.copy(showLogoOnReceipt = !it.showLogoOnReceipt) }
    }

    fun updateReceiptThankYou(value: String) {
        _state.update { it.copy(receiptThankYou = value) }
    }

    fun toggleAllowDebt() {
        _state.update { it.copy(allowDebt = !it.allowDebt) }
    }

    fun toggleLowStockAlert() {
        _state.update { it.copy(lowStockAlert = !it.lowStockAlert) }
    }

    fun toggleSalesSound() {
        _state.update { it.copy(salesSound = !it.salesSound) }
    }

    fun updateDefaultLowStockLevel(value: String) {
        _state.update { it.copy(defaultLowStockLevel = value.filter { c -> c.isDigit() }) }
    }

    fun updateOpenTime(value: String) {
        _state.update { it.copy(openTime = value) }
    }

    fun updateCloseTime(value: String) {
        _state.update { it.copy(closeTime = value) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, isSaved = false) }
    }

    // ── Save ──

    fun save() {
        val s = _state.value
        if (s.storeName.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        val store = s.store ?: return

        viewModelScope.launch {
            val lowStockLevel = s.defaultLowStockLevel.toIntOrNull() ?: 20
            val updatedSettings = store.settings.copy(
                taxEnabled = s.taxEnabled,
                defaultTaxRate = s.defaultTaxRate,
                autoPrintReceipt = s.autoPrintReceipt,
                showLogoOnReceipt = s.showLogoOnReceipt,
                receiptThankYou = s.receiptThankYou,
                lowStockAlert = s.lowStockAlert,
                allowDebt = s.allowDebt,
                salesSound = s.salesSound,
                defaultLowStockLevel = lowStockLevel,
                openTime = s.openTime,
                closeTime = s.closeTime,
            )
            val updatedStore = store.copy(
                name = s.storeName.trim(),
                address = s.storeAddress.trim().ifBlank { null },
                phone = s.storePhone.trim().ifBlank { null },
                settings = updatedSettings,
            )
            when (val result = storeRepository.updateStore(updatedStore)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            store = result.data,
                            message = app.getString(R.string.store_settings_saved),
                            isSaved = true,
                        )
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }
}
