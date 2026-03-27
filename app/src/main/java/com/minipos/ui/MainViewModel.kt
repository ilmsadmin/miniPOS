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
    Loading, Locked, Home
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _appState = MutableStateFlow(AppState.Loading)
    val appState: StateFlow<AppState> = _appState

    init {
        viewModelScope.launch {
            // Ensure store exists (creates default if first launch)
            authRepository.ensureDefaultStore()
            // Check if current user has a PIN — if so, require PIN entry
            val userId = appPreferences.getCurrentUserIdSync()
            if (userId != null && userRepository.hasPin(userId)) {
                _appState.value = AppState.Locked
            } else {
                _appState.value = AppState.Home
            }
        }
    }

    fun unlock() {
        _appState.value = AppState.Home
    }

    fun lock() {
        _appState.value = AppState.Locked
    }
}
