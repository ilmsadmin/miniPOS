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
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
) {
    val canCreate: Boolean
        get() = storeName.isNotBlank() &&
                storeCode.length >= 4 &&
                ownerName.isNotBlank()
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

    fun createStore() {
        val s = _state.value
        if (!s.canCreate) return

        if (!Validators.isValidStoreCode(s.storeCode)) {
            _state.update { it.copy(error = app.getString(R.string.error_store_code_format)) }
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
                ownerPin = "",      // No PIN required — can be set later in Settings
                ownerPassword = "", // No password required
            )
            when (result) {
                is Result.Success -> _state.update { it.copy(isLoading = false, isSuccess = true) }
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}
