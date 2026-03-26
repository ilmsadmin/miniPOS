package com.minipos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.repository.AuthRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppState {
    Loading, Onboarding, Login, Home
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storeRepository: StoreRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _appState = MutableStateFlow(AppState.Loading)
    val appState: StateFlow<AppState> = _appState

    init {
        viewModelScope.launch {
            authRepository.isOnboarded().collect { onboarded ->
                if (!onboarded) {
                    _appState.value = AppState.Onboarding
                } else {
                    // Safety check: if onboarded but database was wiped (e.g. destructive migration),
                    // reset to onboarding so user can re-create store
                    val store = storeRepository.getStore()
                    if (store == null) {
                        prefs.clearAll()
                        _appState.value = AppState.Onboarding
                        return@collect
                    }

                    val session = authRepository.getCurrentSession()
                    _appState.value = if (session != null) AppState.Home else AppState.Login
                }
            }
        }
    }
}
