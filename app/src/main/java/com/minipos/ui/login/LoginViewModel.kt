package com.minipos.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.model.Result
import com.minipos.domain.model.User
import com.minipos.domain.repository.AuthRepository
import com.minipos.domain.repository.StoreRepository
import com.minipos.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val storeName: String = "",
    val users: List<User> = emptyList(),
    val selectedUser: User? = null,
    val pin: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storeRepository: StoreRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore()
            if (store != null) {
                val users = userRepository.getActiveUsers(store.id)
                _state.update { it.copy(storeName = store.name, users = users) }
                // Auto-select if only one user
                if (users.size == 1) {
                    _state.update { it.copy(selectedUser = users.first()) }
                }
            } else {
                // Database was likely wiped - show error state
                _state.update { it.copy(storeName = "Lỗi dữ liệu", users = emptyList()) }
            }
        }
    }

    fun selectUser(user: User) {
        _state.update { it.copy(selectedUser = user, pin = "", error = null) }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedUser = null, pin = "", error = null) }
    }

    fun updatePin(pin: String) {
        _state.update { it.copy(pin = pin, error = null) }
    }

    fun login() {
        val user = _state.value.selectedUser ?: return
        val pin = _state.value.pin
        if (pin.length < 4) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(user.id, pin)) {
                is Result.Success -> _state.update { it.copy(isLoading = false, isLoggedIn = true) }
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message, pin = "") }
            }
        }
    }
}
