package com.minipos.ui.pinlock

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.core.utils.DateUtils
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PinLockState(
    val pin: String = "",
    val error: String? = null,
    val isLoading: Boolean = false,
    val isUnlocked: Boolean = false,
    val lockedUntilMessage: String? = null,
)

@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val app: Application,
    private val userRepository: UserRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(PinLockState())
    val state: StateFlow<PinLockState> = _state

    init {
        checkLock()
    }

    private fun checkLock() {
        viewModelScope.launch {
            val lockUntil = appPreferences.getLockUntil()
            val now = DateUtils.now()
            if (lockUntil > now) {
                val minutes = ((lockUntil - now) / 60000).toInt() + 1
                _state.update { it.copy(lockedUntilMessage = app.getString(R.string.msg_pin_locked, minutes)) }
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
                // No user found — just unlock
                _state.update { it.copy(isLoading = false, isUnlocked = true) }
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

    /** Quên PIN: xóa PIN và vào app ngay */
    fun clearPin(onDone: () -> Unit) {
        viewModelScope.launch {
            val userId = appPreferences.getCurrentUserIdSync() ?: return@launch
            userRepository.clearPin(userId)
            appPreferences.setLoginAttempts(0)
            appPreferences.setLockUntil(0)
            onDone()
        }
    }
}
