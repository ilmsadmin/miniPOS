package com.minipos.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.domain.model.*
import com.minipos.domain.repository.AuthRepository
import com.minipos.domain.repository.StoreRepository
import com.minipos.domain.repository.UserRepository
import com.minipos.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val store: Store? = null,
    val currentUser: User? = null,
    val currentUserHasPin: Boolean = false,
    val users: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,

    // Dialog states
    val showStoreInfoDialog: Boolean = false,
    val showSalesSettingsDialog: Boolean = false,
    val showUserManagementSheet: Boolean = false,
    val showAddUserDialog: Boolean = false,
    val showEditUserDialog: User? = null,
    val showResetPinDialog: User? = null,
    val showDeleteUserConfirm: User? = null,
    val showBackupDialog: Boolean = false,
    val showRestoreDialog: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val storeRepository: StoreRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private fun str(resId: Int) = app.getString(resId)
    private fun str(resId: Int, vararg args: Any) = app.getString(resId, *args)

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore()
            val userId = appPreferences.currentUserId.first()
            val currentUser = userId?.let { userRepository.getUserById(it) }
            val hasPin = userId?.let { userRepository.hasPin(it) } ?: false
            val users = store?.let { userRepository.getActiveUsers(it.id) } ?: emptyList()
            _state.update {
                it.copy(store = store, currentUser = currentUser, currentUserHasPin = hasPin, users = users, isLoading = false)
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    // ============ Store Info ============

    fun showStoreInfoDialog() { _state.update { it.copy(showStoreInfoDialog = true) } }
    fun dismissStoreInfoDialog() { _state.update { it.copy(showStoreInfoDialog = false) } }

    fun updateStoreInfo(name: String, address: String?, phone: String?) {
        viewModelScope.launch {
            val store = _state.value.store ?: return@launch
            val updated = store.copy(name = name, address = address, phone = phone)
            when (val result = storeRepository.updateStore(updated)) {
                is Result.Success -> {
                    _state.update { it.copy(store = result.data, showStoreInfoDialog = false, message = str(R.string.msg_store_updated)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    fun updateOwnerName(name: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val updated = user.copy(displayName = name)
            when (val result = userRepository.updateUser(updated)) {
                is Result.Success -> {
                    _state.update { it.copy(currentUser = result.data) }
                }
                is Result.Error -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    fun updateOwnerPin(currentPin: String, newPin: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            // If user already has a PIN, verify the current one first
            if (userRepository.hasPin(user.id)) {
                val valid = userRepository.verifyPin(user.id, currentPin)
                if (!valid) {
                    _state.update { it.copy(message = str(R.string.msg_pin_incorrect)) }
                    return@launch
                }
            }
            when (val result = userRepository.resetPin(user.id, newPin)) {
                is Result.Success -> {
                    _state.update { it.copy(showStoreInfoDialog = false, message = str(R.string.msg_pin_updated)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    // ============ Sales Settings ============

    fun showSalesSettingsDialog() { _state.update { it.copy(showSalesSettingsDialog = true) } }
    fun dismissSalesSettingsDialog() { _state.update { it.copy(showSalesSettingsDialog = false) } }

    fun updateSalesSettings(settings: StoreSettings) {
        viewModelScope.launch {
            val store = _state.value.store ?: return@launch
            val updated = store.copy(settings = settings)
            when (val result = storeRepository.updateStore(updated)) {
                is Result.Success -> {
                    _state.update { it.copy(store = result.data, showSalesSettingsDialog = false, message = str(R.string.msg_sales_settings_updated)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    // ============ User Management ============

    fun showUserManagement() { _state.update { it.copy(showUserManagementSheet = true) } }
    fun dismissUserManagement() { _state.update { it.copy(showUserManagementSheet = false) } }

    fun showAddUserDialog() { _state.update { it.copy(showAddUserDialog = true) } }
    fun dismissAddUserDialog() { _state.update { it.copy(showAddUserDialog = false) } }

    fun showEditUserDialog(user: User) { _state.update { it.copy(showEditUserDialog = user) } }
    fun dismissEditUserDialog() { _state.update { it.copy(showEditUserDialog = null) } }

    fun showResetPinDialog(user: User) { _state.update { it.copy(showResetPinDialog = user) } }
    fun dismissResetPinDialog() { _state.update { it.copy(showResetPinDialog = null) } }

    fun showDeleteUserConfirm(user: User) { _state.update { it.copy(showDeleteUserConfirm = user) } }
    fun dismissDeleteUserConfirm() { _state.update { it.copy(showDeleteUserConfirm = null) } }

    fun addUser(name: String, pin: String, role: UserRole) {
        viewModelScope.launch {
            val storeId = _state.value.store?.id ?: return@launch
            when (val result = userRepository.createUser(storeId, name, pin, role)) {
                is Result.Success -> {
                    val users = userRepository.getActiveUsers(storeId)
                    _state.update { it.copy(users = users, showAddUserDialog = false, message = str(R.string.msg_staff_added, result.data.displayName)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            val storeId = _state.value.store?.id ?: return@launch
            when (val result = userRepository.updateUser(user)) {
                is Result.Success -> {
                    val users = userRepository.getActiveUsers(storeId)
                    _state.update { it.copy(users = users, showEditUserDialog = null, message = str(R.string.msg_staff_updated)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    fun resetUserPin(userId: String, newPin: String) {
        viewModelScope.launch {
            when (val result = userRepository.resetPin(userId, newPin)) {
                is Result.Success -> {
                    _state.update { it.copy(showResetPinDialog = null, message = str(R.string.msg_pin_reset)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            val storeId = _state.value.store?.id ?: return@launch
            when (val result = userRepository.deleteUser(userId)) {
                is Result.Success -> {
                    val users = userRepository.getActiveUsers(storeId)
                    _state.update { it.copy(users = users, showDeleteUserConfirm = null, message = str(R.string.msg_staff_deleted)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    // ============ Backup/Restore ============

    fun showBackupDialog() { _state.update { it.copy(showBackupDialog = true) } }
    fun dismissBackupDialog() { _state.update { it.copy(showBackupDialog = false) } }

    fun showRestoreDialog() { _state.update { it.copy(showRestoreDialog = true) } }
    fun dismissRestoreDialog() { _state.update { it.copy(showRestoreDialog = false) } }

}
