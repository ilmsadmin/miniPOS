package com.minipos.ui.login

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.model.AuthSession
import com.minipos.domain.model.Result
import com.minipos.domain.model.User
import com.minipos.domain.model.UserRole
import com.minipos.domain.repository.AuthRepository
import com.minipos.domain.repository.StoreRepository
import com.minipos.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ForgotPinStep { ENTER_PASSWORD, RESET_PIN }

data class LoginState(
    val users: List<User> = emptyList(),
    val storeName: String = "",
    val storeCode: String = "",
    val currentUserId: String? = null,
    val selectedUser: User? = null,
    val pin: String = "",
    val error: String? = null,
    val isLoading: Boolean = true,
    val loginSuccess: AuthSession? = null,
    // Auto-submit: true khi user chỉ cần 4 số (nếu PIN dài 4), false nếu dùng 6 số
    val pinLength: Int = 6,
    // Forgot PIN flow (OWNER only)
    val showForgotPin: Boolean = false,
    val forgotPinStep: ForgotPinStep = ForgotPinStep.ENTER_PASSWORD,
    val ownerHasPassword: Boolean = false,  // whether selected owner has a password set
    val password: String = "",
    val newPin: String = "",
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val app: Application,
    private val storeRepository: StoreRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            val store = storeRepository.getStore()
            if (store == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            // Dùng getAllUsers để hiển thị tất cả tài khoản (kể cả inactive).
            // AuthRepository.login() sẽ chặn user bị vô hiệu hóa khi họ cố đăng nhập.
            val users = userRepository.getAllUsers(store.id)
            // Chỉ hiện badge "Đang dùng" nếu user thực sự đang đăng nhập (isLoggedIn = true)
            // Không dùng currentUserId khi app mới khởi động mà chưa login
            val isLoggedIn = appPreferences.isLoggedInSync()
            val currentUserId = if (isLoggedIn) appPreferences.getCurrentUserIdSync() else null

            // Sort: OWNER first, then MANAGER, then CASHIER; each group sorted by name
            val sorted = users.sortedWith(
                compareBy(
                    { when (it.role) { UserRole.OWNER -> 0; UserRole.MANAGER -> 1; UserRole.CASHIER -> 2 } },
                    { it.displayName },
                )
            )

            _state.update {
                it.copy(
                    users = sorted,
                    storeName = store.name,
                    storeCode = store.code,
                    currentUserId = currentUserId,
                    isLoading = false,
                )
            }

            // Auto-select nếu chỉ có đúng 1 user (OWNER duy nhất — setup mới)
            if (sorted.size == 1) {
                val singleUser = sorted.first()
                val hasPassword = if (singleUser.role == UserRole.OWNER) userRepository.hasPassword(singleUser.id) else false
                _state.update { it.copy(selectedUser = singleUser, pin = "", error = null, ownerHasPassword = hasPassword) }
            }
        }
    }

    fun selectUser(user: User) {
        _state.update { it.copy(selectedUser = user, pin = "", error = null) }
        // Pre-load password status for OWNER so ForgotPin UI knows which flow to show
        if (user.role == UserRole.OWNER) {
            viewModelScope.launch {
                val hasPassword = userRepository.hasPassword(user.id)
                _state.update { it.copy(ownerHasPassword = hasPassword) }
            }
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedUser = null, pin = "", error = null) }
    }

    fun onPinChanged(pin: String) {
        _state.update { it.copy(pin = pin, error = null) }
        // Auto-submit: nếu PIN đủ 6 số → tự đăng nhập luôn
        // Nếu PIN đúng 4 số và user đã confirm (bằng cách nhấn Done) sẽ xử lý qua onDone
        if (pin.length == 6) {
            login()
        }
    }

    fun login() {
        val user = _state.value.selectedUser ?: return
        val pin = _state.value.pin
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val hasPin = userRepository.hasPin(user.id)
            if (!hasPin) {
                // No PIN set — this is a security issue, should not happen after onboarding fix
                // Still allow login for legacy data but log warning
                appPreferences.setCurrentUser(user.id)
                val session = authRepository.getCurrentSession()
                _state.update {
                    it.copy(isLoading = false, loginSuccess = session)
                }
                return@launch
            }
            // Verify PIN via AuthRepository (handles lock logic)
            when (val result = authRepository.login(user.id, pin)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, loginSuccess = result.data) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, pin = "", error = result.message) }
                }
            }
        }
    }

    fun consumeLoginSuccess() {
        _state.update { it.copy(loginSuccess = null) }
    }

    // ── Forgot PIN flow ────────────────────────────────────────────────

    fun showForgotPin() {
        val hasPassword = _state.value.ownerHasPassword
        val initialStep = if (hasPassword) ForgotPinStep.ENTER_PASSWORD else ForgotPinStep.RESET_PIN
        _state.update { it.copy(showForgotPin = true, forgotPinStep = initialStep, password = "", newPin = "", error = null) }
    }

    fun hideForgotPin() {
        _state.update { it.copy(showForgotPin = false, password = "", newPin = "", error = null) }
    }

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun onNewPinChanged(value: String) {
        _state.update { it.copy(newPin = value, error = null) }
    }

    /** Step 1: verify owner password → proceed to reset PIN step */
    fun verifyPasswordForReset() {
        val user = _state.value.selectedUser ?: return
        val password = _state.value.password
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.loginWithPassword(user.id, password)) {
                is Result.Success -> {
                    // Password correct → move to reset-PIN step
                    _state.update { it.copy(isLoading = false, forgotPinStep = ForgotPinStep.RESET_PIN, error = null) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    /** Step 2: set new PIN, then login.
     *  If owner has no password (ownerHasPassword = false), resets directly without password verification. */
    fun resetPinAndLogin() {
        val user = _state.value.selectedUser ?: return
        val password = _state.value.password
        val newPin = _state.value.newPin
        val hasPassword = _state.value.ownerHasPassword
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val resetResult = if (hasPassword) {
                authRepository.resetPinWithPassword(user.id, password, newPin)
            } else {
                userRepository.resetPin(user.id, newPin)
            }
            when (resetResult) {
                is Result.Success -> {
                    // PIN reset — now login with new PIN automatically
                    when (val loginResult = authRepository.login(user.id, newPin)) {
                        is Result.Success -> {
                            _state.update { it.copy(isLoading = false, showForgotPin = false, loginSuccess = loginResult.data) }
                        }
                        is Result.Error -> {
                            _state.update { it.copy(isLoading = false, error = loginResult.message) }
                        }
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = resetResult.message) }
                }
            }
        }
    }
}
