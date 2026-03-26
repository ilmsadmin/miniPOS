package com.minipos.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val step: Int = 1,
    val storeName: String = "",
    val storeCode: String = "",
    val storeAddress: String = "",
    val storePhone: String = "",
    val ownerName: String = "",
    val ownerPin: String = "",
    val ownerPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
) {
    val canCreate: Boolean
        get() = ownerName.isNotBlank() &&
                Validators.isValidPin(ownerPin) &&
                Validators.isValidPassword(ownerPassword) &&
                ownerPassword == confirmPassword
}

@HiltViewModel
class CreateStoreViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateStoreState())
    val state: StateFlow<CreateStoreState> = _state

    fun updateStoreName(value: String) = _state.update { it.copy(storeName = value, error = null) }
    fun updateStoreCode(value: String) = _state.update { it.copy(storeCode = value, error = null) }
    fun updateStoreAddress(value: String) = _state.update { it.copy(storeAddress = value) }
    fun updateStorePhone(value: String) = _state.update { it.copy(storePhone = value) }
    fun updateOwnerName(value: String) = _state.update { it.copy(ownerName = value, error = null) }
    fun updateOwnerPin(value: String) = _state.update { it.copy(ownerPin = value, error = null) }
    fun updateOwnerPassword(value: String) = _state.update { it.copy(ownerPassword = value, error = null) }
    fun updateConfirmPassword(value: String) = _state.update { it.copy(confirmPassword = value, error = null) }

    fun nextStep() {
        val s = _state.value
        if (!Validators.isValidStoreCode(s.storeCode)) {
            _state.update { it.copy(error = "Mã cửa hàng phải từ 4–8 ký tự chữ/số") }
            return
        }
        _state.update { it.copy(step = 2, error = null) }
    }

    fun previousStep() = _state.update { it.copy(step = 1, error = null) }

    fun createStore() {
        val s = _state.value
        if (!s.canCreate) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.createStore(
                storeName = s.storeName,
                storeCode = s.storeCode,
                address = s.storeAddress.ifBlank { null },
                phone = s.storePhone.ifBlank { null },
                ownerName = s.ownerName,
                ownerPin = s.ownerPin,
                ownerPassword = s.ownerPassword,
            )
            when (result) {
                is Result.Success -> _state.update { it.copy(isLoading = false, isSuccess = true) }
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}
