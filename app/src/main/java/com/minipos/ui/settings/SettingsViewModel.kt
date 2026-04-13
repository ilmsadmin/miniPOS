package com.minipos.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.core.auth.PermissionChecker
import com.minipos.core.backup.BackupFileInfo
import com.minipos.core.backup.BackupManager
import com.minipos.core.backup.BackupResult
import com.minipos.core.backup.RestoreResult
import com.minipos.domain.model.*
import com.minipos.domain.repository.AuthRepository
import com.minipos.domain.repository.StoreRepository
import com.minipos.domain.repository.UserRepository
import com.minipos.data.preferences.AppPreferences
import com.minipos.core.theme.AppLanguage
import com.minipos.core.theme.ThemeManager
import com.minipos.core.theme.ThemeMode
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
    // Permissions
    val cashierPerms: CashierPermissions = CashierPermissions(),
    val effectivePermissions: Set<Permission> = emptySet(),

    // Dialog states
    val showStoreInfoDialog: Boolean = false,
    val showSalesSettingsDialog: Boolean = false,
    val showCashierPermissionsDialog: Boolean = false,
    val showUserManagementSheet: Boolean = false,
    val showAddUserDialog: Boolean = false,
    val showEditUserDialog: User? = null,
    val showResetPinDialog: User? = null,
    val showDeleteUserConfirm: User? = null,
    val showBackupDialog: Boolean = false,
    val showRestoreDialog: Boolean = false,
    val showChangePinDialog: Boolean = false,
    val pinVerified: Boolean = false,
    val pinVerifyError: String? = null,
    val showChangePasswordDialog: Boolean = false,
    val currentUserHasPassword: Boolean = false,

    // Backup/Restore
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val backupFiles: List<BackupFileInfo> = emptyList(),
    val showRestorePickerDialog: Boolean = false,
    val restoreConfirmFile: BackupFileInfo? = null,
    val lastBackupResult: String? = null,
) {
    fun can(permission: Permission): Boolean = effectivePermissions.contains(permission)
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val storeRepository: StoreRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val appPreferences: AppPreferences,
    val themeManager: ThemeManager,
    private val backupManager: BackupManager,
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
            val hasPassword = userId?.let { userRepository.hasPassword(it) } ?: false
            val users = store?.let { userRepository.getActiveUsers(it.id) } ?: emptyList()
            val cashierPerms = store?.settings?.cashierPermissions ?: CashierPermissions()
            val effectivePermissions = currentUser?.let {
                PermissionChecker.getEffectivePermissions(it.role, cashierPerms)
            } ?: emptySet()
            _state.update {
                it.copy(
                    store = store, currentUser = currentUser, currentUserHasPin = hasPin,
                    currentUserHasPassword = hasPassword, users = users, isLoading = false,
                    cashierPerms = cashierPerms, effectivePermissions = effectivePermissions,
                )
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun showMessage(msg: String) {
        _state.update { it.copy(message = msg) }
    }

    // ============ Theme & Language ============

    fun setThemeMode(mode: ThemeMode) {
        themeManager.setThemeMode(mode)
    }

    fun setLanguage(lang: AppLanguage) {
        themeManager.setLanguage(lang)
    }

    // ============ Store Info ============

    fun showStoreInfoDialog() {
        if (!_state.value.can(Permission.STORE_EDIT)) {
            _state.update { it.copy(message = str(R.string.error_owner_only)) }
            return
        }
        _state.update { it.copy(showStoreInfoDialog = true) }
    }
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

    // ============ Change PIN (dedicated flow) ============

    fun showChangePinDialog() { _state.update { it.copy(showChangePinDialog = true, pinVerified = false, pinVerifyError = null) } }
    fun dismissChangePinDialog() { _state.update { it.copy(showChangePinDialog = false, pinVerified = false, pinVerifyError = null) } }

    // ============ Change Password (OWNER only) ============

    fun showChangePasswordDialog() {
        if (!_state.value.can(Permission.STORE_SETTINGS)) {
            _state.update { it.copy(message = str(R.string.error_owner_only)) }
            return
        }
        _state.update { it.copy(showChangePasswordDialog = true) }
    }
    fun dismissChangePasswordDialog() { _state.update { it.copy(showChangePasswordDialog = false) } }

    fun saveOwnerPassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            // If user already has a password, verify it first
            if (_state.value.currentUserHasPassword) {
                val valid = userRepository.verifyPassword(user.id, currentPassword)
                if (!valid) {
                    _state.update { it.copy(message = str(R.string.error_wrong_password)) }
                    return@launch
                }
            }
            when (val result = userRepository.setPassword(user.id, newPassword)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            showChangePasswordDialog = false,
                            currentUserHasPassword = true,
                            message = str(R.string.msg_password_updated),
                        )
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    fun verifyCurrentPin(pin: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val valid = userRepository.verifyPin(user.id, pin)
            if (valid) {
                _state.update { it.copy(pinVerified = true, pinVerifyError = null) }
            } else {
                _state.update { it.copy(pinVerifyError = str(R.string.msg_pin_incorrect)) }
            }
        }
    }

    fun saveNewPin(newPin: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            when (val result = userRepository.resetPin(user.id, newPin)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            showChangePinDialog = false,
                            pinVerified = false,
                            pinVerifyError = null,
                            currentUserHasPin = true,
                            message = str(R.string.msg_pin_updated),
                        )
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    // ============ Sales Settings ============

    fun showSalesSettingsDialog() {
        if (!_state.value.can(Permission.STORE_SETTINGS)) {
            _state.update { it.copy(message = str(R.string.error_owner_only)) }
            return
        }
        _state.update { it.copy(showSalesSettingsDialog = true) }
    }
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

    // ============ User Management (OWNER + MANAGER) ============

    /** OWNER có thể quản lý tất cả; MANAGER chỉ được quản lý Cashier */
    private fun canManageUser(targetRole: UserRole): Boolean {
        val actorRole = _state.value.currentUser?.role ?: return false
        return PermissionChecker.canManageUser(actorRole, targetRole)
    }

    private fun requireUserPermission(permission: Permission): Boolean {
        return if (_state.value.can(permission)) true
        else { _state.update { it.copy(message = str(R.string.error_owner_only)) }; false }
    }

    fun showUserManagement() {
        if (!requireUserPermission(Permission.USER_VIEW)) return
        _state.update { it.copy(showUserManagementSheet = true) }
    }
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
        if (!requireUserPermission(Permission.USER_CREATE)) return
        if (!canManageUser(role)) {
            _state.update { it.copy(message = "Bạn không có quyền tạo tài khoản với vai trò này") }
            return
        }
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
        if (!requireUserPermission(Permission.USER_EDIT)) return
        if (!canManageUser(user.role)) {
            _state.update { it.copy(message = "Bạn không có quyền chỉnh sửa tài khoản này") }
            return
        }
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
        if (!requireUserPermission(Permission.USER_RESET_PIN)) return
        viewModelScope.launch {
            // Prevent resetting pin of higher-role user
            val targetUser = userRepository.getUserById(userId)
            if (targetUser != null && !canManageUser(targetUser.role)) {
                _state.update { it.copy(message = "Bạn không có quyền reset PIN của tài khoản này") }
                return@launch
            }
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
        if (!requireUserPermission(Permission.USER_DEACTIVATE)) return
        viewModelScope.launch {
            val targetUser = userRepository.getUserById(userId)
            if (targetUser != null && !canManageUser(targetUser.role)) {
                _state.update { it.copy(message = "Bạn không có quyền xóa tài khoản này") }
                return@launch
            }
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

    // ============ Cashier Permissions (OWNER only) ============

    fun showCashierPermissionsDialog() {
        if (!_state.value.can(Permission.STORE_SETTINGS)) {
            _state.update { it.copy(message = str(R.string.error_owner_only)) }
            return
        }
        _state.update { it.copy(showCashierPermissionsDialog = true) }
    }
    fun dismissCashierPermissionsDialog() { _state.update { it.copy(showCashierPermissionsDialog = false) } }

    fun saveCashierPermissions(cashierPerms: CashierPermissions) {
        if (!_state.value.can(Permission.STORE_SETTINGS)) {
            _state.update { it.copy(message = str(R.string.error_owner_only)) }
            return
        }
        viewModelScope.launch {
            val store = _state.value.store ?: return@launch
            val updated = store.copy(settings = store.settings.copy(cashierPermissions = cashierPerms))
            when (val result = storeRepository.updateStore(updated)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            store = result.data,
                            cashierPerms = cashierPerms,
                            showCashierPermissionsDialog = false,
                            message = "Đã lưu cài đặt quyền thu ngân",
                        )
                    }
                }
                is Result.Error -> _state.update { it.copy(message = result.message) }
            }
        }
    }

    // ============ Backup/Restore ============

    fun showBackupDialog() {
        if (!_state.value.can(Permission.DATA_BACKUP)) { _state.update { it.copy(message = str(R.string.error_owner_only)) }; return }
        val files = backupManager.listBackups()
        _state.update { it.copy(showBackupDialog = true, backupFiles = files) }
    }
    fun dismissBackupDialog() { _state.update { it.copy(showBackupDialog = false) } }

    fun createBackup() {
        if (!_state.value.can(Permission.DATA_BACKUP)) { _state.update { it.copy(message = str(R.string.error_owner_only)) }; return }
        viewModelScope.launch {
            _state.update { it.copy(isBackingUp = true) }
            when (val result = backupManager.createBackup()) {
                is BackupResult.Success -> {
                    val files = backupManager.listBackups()
                    val sizeMb = String.format("%.2f", result.sizeBytes / 1024.0 / 1024.0)
                    _state.update {
                        it.copy(
                            isBackingUp = false,
                            backupFiles = files,
                            lastBackupResult = str(R.string.backup_success, result.fileName, sizeMb),
                            message = str(R.string.backup_success, result.fileName, sizeMb),
                        )
                    }
                }
                is BackupResult.Error -> {
                    _state.update { it.copy(isBackingUp = false, message = str(R.string.backup_failed, result.message)) }
                }
            }
        }
    }

    fun deleteBackupFile(filePath: String) {
        backupManager.deleteBackup(filePath)
        val files = backupManager.listBackups()
        _state.update { it.copy(backupFiles = files) }
    }

    fun showRestoreDialog() {
        if (!_state.value.can(Permission.DATA_RESTORE)) { _state.update { it.copy(message = str(R.string.error_owner_only)) }; return }
        val files = backupManager.listBackups()
        _state.update { it.copy(showRestoreDialog = true, backupFiles = files) }
    }
    fun dismissRestoreDialog() { _state.update { it.copy(showRestoreDialog = false, restoreConfirmFile = null) } }

    fun confirmRestoreFile(file: BackupFileInfo) {
        _state.update { it.copy(restoreConfirmFile = file) }
    }
    fun cancelRestoreConfirm() { _state.update { it.copy(restoreConfirmFile = null) } }

    fun executeRestore(filePath: String) {
        if (!_state.value.can(Permission.DATA_RESTORE)) { _state.update { it.copy(message = str(R.string.error_owner_only)) }; return }
        viewModelScope.launch {
            _state.update { it.copy(isRestoring = true, restoreConfirmFile = null) }
            when (val result = backupManager.restoreBackup(filePath)) {
                is RestoreResult.Success -> {
                    // Reload all data after restore
                    loadData()
                    _state.update {
                        it.copy(isRestoring = false, showRestoreDialog = false, message = str(R.string.restore_success))
                    }
                }
                is RestoreResult.Error -> {
                    _state.update { it.copy(isRestoring = false, message = str(R.string.restore_failed, result.message)) }
                }
            }
        }
    }

}
