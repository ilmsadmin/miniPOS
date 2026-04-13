package com.minipos.ui.onboarding

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.core.utils.Validators
import com.minipos.domain.model.Result
import com.minipos.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateStoreState(
    val storeName: String = "",
    val storeCode: String = "",
    val storeAddress: String = "",
    val storePhone: String = "",
    val ownerName: String = "",
    val ownerPin: String = "",
    val ownerPinConfirm: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
) {
    val pinValid: Boolean
        get() = ownerPin.length in 4..6 && ownerPin == ownerPinConfirm
    val canCreate: Boolean
        get() = storeName.isNotBlank() &&
                storeCode.length >= 4 &&
                ownerName.isNotBlank() &&
                pinValid
}

@HiltViewModel
class CreateStoreViewModel @Inject constructor(
    private val app: Application,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateStoreState())
    val state: StateFlow<CreateStoreState> = _state

    fun updateStoreName(value: String) = _state.update { it.copy(storeName = value, error = null) }
    fun updateStoreCode(value: String) = _state.update { it.copy(storeCode = value, error = null) }
    fun updateStoreAddress(value: String) = _state.update { it.copy(storeAddress = value) }
    fun updateStorePhone(value: String) = _state.update { it.copy(storePhone = value) }
    fun updateOwnerName(value: String) = _state.update { it.copy(ownerName = value, error = null) }
    fun updateOwnerPin(value: String) = _state.update { it.copy(ownerPin = value.take(6), error = null) }
    fun updateOwnerPinConfirm(value: String) = _state.update { it.copy(ownerPinConfirm = value.take(6), error = null) }

    fun createStore() {
        val s = _state.value
        if (!s.canCreate) return

        if (!Validators.isValidStoreCode(s.storeCode)) {
            _state.update { it.copy(error = app.getString(R.string.error_store_code_format)) }
            return
        }

        if (s.ownerPin.length < 4) {
            _state.update { it.copy(error = app.getString(R.string.error_pin_length)) }
            return
        }

        if (s.ownerPin != s.ownerPinConfirm) {
            _state.update { it.copy(error = app.getString(R.string.pin_mismatch_error)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.createStore(
                storeName = s.storeName,
                storeCode = s.storeCode,
                address = s.storeAddress.ifBlank { null },
                phone = s.storePhone.ifBlank { null },
                ownerName = s.ownerName,
                ownerPin = s.ownerPin,
                ownerPassword = "",
            )
            when (result) {
                is Result.Success -> _state.update { it.copy(isLoading = false, isSuccess = true) }
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}
