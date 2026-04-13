package com.minipos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.repository.AuthRepository
import com.minipos.domain.repository.UserRepository
import com.minipos.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppState {
    Splash, Onboarding, Locked, Login, Home
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _appState = MutableStateFlow(AppState.Splash)
    val appState: StateFlow<AppState> = _appState

    // The state to go to after splash finishes
    private var postSplashState: AppState? = null
    private var dataReady = false

    init {
        viewModelScope.launch {
            // Determine what screen to show after splash
            val isOnboarded = appPreferences.isOnboarded.first()
            if (!isOnboarded) {
                // Edge case: existing user upgrading — store already exists but isOnboarded was never set
                val storeId = appPreferences.getCurrentStoreIdSync()
                if (storeId != null) {
                    appPreferences.setOnboarded(true)
                    authRepository.ensureDefaultStore()
                    postSplashState = determineHomeOrLock()
                } else {
                    postSplashState = AppState.Onboarding
                }
            } else {
                authRepository.ensureDefaultStore()
                postSplashState = determineHomeOrLock()
            }
            dataReady = true

            // If splash already finished waiting, transition now
            // (handled by onSplashFinished)
        }
    }

    private suspend fun determineHomeOrLock(): AppState {
        val isLoggedIn = appPreferences.isLoggedInSync()
        if (!isLoggedIn) {
            // User chưa đăng nhập trong session trước → luôn show màn hình chọn user
            return AppState.Login
        }
        val userId = appPreferences.getCurrentUserIdSync() ?: return AppState.Login
        return when {
            userRepository.hasPin(userId) -> AppState.Locked
            else -> AppState.Home
        }
    }

    /** Called by SplashScreen when its animation completes */
    fun onSplashFinished() {
        viewModelScope.launch {
            // Wait until data is ready (should already be ready since splash takes ~3s)
            while (!dataReady) {
                kotlinx.coroutines.delay(50)
            }
            _appState.value = postSplashState ?: AppState.Onboarding
        }
    }

    fun onOnboardingComplete() {
        viewModelScope.launch {
            appPreferences.setOnboarded(true)
            authRepository.ensureDefaultStore()
            // createStore() đã set isLoggedIn = true → dùng determineHomeOrLock
            _appState.value = determineHomeOrLock()
        }
    }

    fun unlock() {
        _appState.value = AppState.Home
    }

    fun lock() {
        _appState.value = AppState.Locked
    }

    /** Called after user explicitly logs out — show user-selection Login screen */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _appState.value = AppState.Login
        }
    }

    /** Called by LoginScreen when a user has successfully authenticated */
    fun onLoginSuccess() {
        // Login thành công → đi thẳng vào Home (không qua Locked)
        // Locked chỉ xuất hiện khi app đang chạy bị đưa background rồi quay lại
        _appState.value = AppState.Home
    }
}
