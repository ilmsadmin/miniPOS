package com.minipos.ui.pinlock

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.core.utils.DateUtils
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.model.Result
import com.minipos.domain.repository.AuthRepository
import com.minipos.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PinLockForgotStep { ENTER_PASSWORD, SET_NEW_PIN }

data class PinLockState(
    val pin: String = "",
    val error: String? = null,
    val isLoading: Boolean = false,
    val isUnlocked: Boolean = false,
    val lockedUntilMessage: String? = null,
    // Forgot PIN flow
    val showForgotPin: Boolean = false,
    val forgotStep: PinLockForgotStep = PinLockForgotStep.ENTER_PASSWORD,
    val ownerHasPassword: Boolean = false,
    val forgotPassword: String = "",
    val newPin: String = "",
    val forgotError: String? = null,
)

@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val app: Application,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(PinLockState())
    val state: StateFlow<PinLockState> = _state

    init {
        checkLockAndLoadUser()
    }

    private fun checkLockAndLoadUser() {
        viewModelScope.launch {
            val lockUntil = appPreferences.getLockUntil()
            val now = DateUtils.now()
            if (lockUntil > now) {
                val minutes = ((lockUntil - now) / 60000).toInt() + 1
                _state.update { it.copy(lockedUntilMessage = app.getString(R.string.msg_pin_locked, minutes)) }
            }
            // Pre-load whether the current owner has a password set
            val userId = appPreferences.getCurrentUserIdSync()
            if (userId != null) {
                val hasPassword = userRepository.hasPassword(userId)
                _state.update { it.copy(ownerHasPassword = hasPassword) }
            }
        }
    }

    fun onPinChanged(pin: String) {
        _state.update { it.copy(pin = pin, error = null) }
    }

    fun submitPin() {
        val pin = _state.value.pin
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Check account lock
            val lockUntil = appPreferences.getLockUntil()
            val now = DateUtils.now()
            if (lockUntil > now) {
                val minutes = ((lockUntil - now) / 60000).toInt() + 1
                _state.update {
                    it.copy(isLoading = false, lockedUntilMessage = app.getString(R.string.msg_pin_locked, minutes))
                }
                return@launch
            }

            val userId = appPreferences.getCurrentUserIdSync()
            if (userId == null) {
                // No user found — cannot verify, show error instead of auto-unlocking
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = app.getString(R.string.error_account_not_found),
                    )
                }
                return@launch
            }

            val valid = userRepository.verifyPin(userId, pin)
            if (valid) {
                appPreferences.setLoginAttempts(0)
                appPreferences.setLockUntil(0)
                _state.update { it.copy(isLoading = false, isUnlocked = true) }
            } else {
                val attempts = appPreferences.getLoginAttempts() + 1
                appPreferences.setLoginAttempts(attempts)
                if (attempts >= 5) {
                    appPreferences.setLockUntil(DateUtils.now() + 5 * 60 * 1000)
                    appPreferences.setLoginAttempts(0)
                    _state.update {
                        it.copy(
                            isLoading = false, pin = "", error = null,
                            lockedUntilMessage = app.getString(R.string.msg_pin_locked_5min),
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false, pin = "",
                            error = app.getString(R.string.msg_pin_wrong, 5 - attempts),
                        )
                    }
                }
            }
        }
    }

    /** Quên PIN: mở overlay reset PIN */
    fun showForgotPin() {
        val hasPassword = _state.value.ownerHasPassword
        val initialStep = if (hasPassword) PinLockForgotStep.ENTER_PASSWORD else PinLockForgotStep.SET_NEW_PIN
        _state.update { it.copy(showForgotPin = true, forgotStep = initialStep, forgotPassword = "", newPin = "", forgotError = null) }
    }

    fun hideForgotPin() {
        _state.update { it.copy(showForgotPin = false, forgotPassword = "", newPin = "", forgotError = null) }
    }

    fun onForgotPasswordChanged(value: String) {
        _state.update { it.copy(forgotPassword = value, forgotError = null) }
    }

    fun onNewPinChanged(value: String) {
        _state.update { it.copy(newPin = value, forgotError = null) }
    }

    /** Bước 1: xác minh mật khẩu chủ cửa hàng */
    fun verifyPasswordForReset() {
        val password = _state.value.forgotPassword
        viewModelScope.launch {
            val userId = appPreferences.getCurrentUserIdSync() ?: return@launch
            _state.update { it.copy(isLoading = true, forgotError = null) }
            when (val result = authRepository.loginWithPassword(userId, password)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, forgotStep = PinLockForgotStep.SET_NEW_PIN, forgotError = null) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, forgotError = result.message) }
                }
            }
        }
    }

    /** Bước 2: đặt PIN mới và mở khóa */
    fun resetPinAndUnlock() {
        val password = _state.value.forgotPassword
        val newPin = _state.value.newPin
        val hasPassword = _state.value.ownerHasPassword
        viewModelScope.launch {
            val userId = appPreferences.getCurrentUserIdSync() ?: return@launch
            _state.update { it.copy(isLoading = true, forgotError = null) }
            val resetResult = if (hasPassword) {
                authRepository.resetPinWithPassword(userId, password, newPin)
            } else {
                userRepository.resetPin(userId, newPin)
            }
            when (resetResult) {
                is Result.Success -> {
                    appPreferences.setLoginAttempts(0)
                    appPreferences.setLockUntil(0)
                    _state.update { it.copy(isLoading = false, showForgotPin = false, isUnlocked = true) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, forgotError = resetResult.message) }
                }
            }
        }
    }
}
