package com.minipos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.repository.AuthRepository
import com.minipos.domain.repository.UserRepository
import com.minipos.data.preferences.AppPreferences
import com.minipos.core.rating.RatingManager
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
    val ratingManager: RatingManager,
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
            // createStore() sets isLoggedIn = true → go to Locked/Home
            // joinStore() does NOT set isLoggedIn → go to Login so user can choose their account
            _appState.value = determineHomeOrLock()
        }
    }

    fun unlock() {
        _appState.value = AppState.Home
    }

    fun lock() {
        _appState.value = AppState.Locked
    }

    // true khi user vào màn hình chọn tài khoản từ Home (Switch User), false khi login lần đầu
    private val _loginCanGoBack = MutableStateFlow(false)
    val loginCanGoBack: StateFlow<Boolean> = _loginCanGoBack

    /** Called after user explicitly logs out — show user-selection Login screen */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loginCanGoBack.value = false
            _appState.value = AppState.Login
        }
    }

    /** Called from HomeScreen "Switch User" — show user-selection without fully logging out */
    fun switchUser() {
        _loginCanGoBack.value = true
        _appState.value = AppState.Login
    }

    /** Called when user presses back on the Login screen (only available in switchUser mode) */
    fun cancelSwitchUser() {
        _loginCanGoBack.value = false
        _appState.value = AppState.Home
    }

    /** Called by LoginScreen when a user has successfully authenticated */
    fun onLoginSuccess() {
        // Login thành công → đi thẳng vào Home (không qua Locked)
        // Locked chỉ xuất hiện khi app đang chạy bị đưa background rồi quay lại
        _appState.value = AppState.Home
    }

    // ── Rating ──

    /** Call this after any successful user action to potentially trigger the rating dialog */
    fun notifySuccessAction() {
        viewModelScope.launch {
            ratingManager.onSuccessAction()
        }
    }

    fun dismissRating() {
        viewModelScope.launch {
            ratingManager.onDismiss()
        }
    }

    fun onRatingCompleted(stars: Int) {
        viewModelScope.launch {
            ratingManager.onRated(stars)
        }
    }

    /** Force-show the rating dialog (for testing from Settings) */
    fun forceShowRating() {
        ratingManager.forceShow()
    }
}
