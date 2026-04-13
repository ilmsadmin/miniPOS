package com.minipos.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.R
import com.minipos.core.auth.PermissionChecker
import com.minipos.core.theme.ThemeManager
import com.minipos.core.theme.ThemeMode
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.model.CashierPermissions
import com.minipos.domain.model.Permission
import com.minipos.domain.model.Store
import com.minipos.domain.model.User
import com.minipos.domain.model.UserRole
import com.minipos.domain.model.Result
import com.minipos.domain.repository.AuthRepository
import com.minipos.domain.repository.CategoryRepository
import com.minipos.domain.repository.CustomerRepository
import com.minipos.domain.repository.OrderRepository
import com.minipos.domain.repository.ProductRepository
import com.minipos.domain.repository.StoreRepository
import com.minipos.domain.repository.SupplierRepository
import com.minipos.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val storeName: String = "",
    val userName: String = "",
    val userRole: UserRole = UserRole.CASHIER,
    val store: Store? = null,
    val currentUser: User? = null,
    val currentUserHasPin: Boolean = false,
    val todayRevenue: Double = 0.0,
    val todayOrders: Int = 0,
    val lowStockCount: Int = 0,
    // Permissions state — computed from role + cashierPerms
    val cashierPerms: CashierPermissions = CashierPermissions(),
    val effectivePermissions: Set<Permission> = emptySet(),
    // PIN change state
    val showChangePinSheet: Boolean = false,
    val pinVerified: Boolean = false,
    val pinVerifyError: String? = null,
    val pinMessage: String? = null,
    // Setup Guide state
    val setupGuideVisible: Boolean = false,
    val setupCategoryCount: Int = 0,
    val setupProductCount: Int = 0,
    val setupSupplierCount: Int = 0,
    val setupOrderCount: Int = 0,
) {
    /** Shortcut: kiểm tra quyền từ UI state */
    fun can(permission: Permission): Boolean = effectivePermissions.contains(permission)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val storeRepository: StoreRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val appPreferences: AppPreferences,
    val themeManager: ThemeManager,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    init {
        loadData()
        loadSetupGuide()
    }

    private fun loadData() {
        viewModelScope.launch {
            val session = authRepository.getCurrentSession()
            val store = storeRepository.getStore()
            val userId = appPreferences.currentUserId.first()
            val currentUser = userId?.let { userRepository.getUserById(it) }
            val hasPin = userId?.let { userRepository.hasPin(it) } ?: false

            if (session != null && store != null) {
                val cashierPerms = store.settings.cashierPermissions
                val effectivePermissions = PermissionChecker.getEffectivePermissions(
                    currentUser?.role ?: session.role, cashierPerms
                )
                _state.update {
                    it.copy(
                        storeName = store.name,
                        userName = currentUser?.displayName ?: session.displayName,
                        userRole = currentUser?.role ?: UserRole.CASHIER,
                        store = store,
                        currentUser = currentUser,
                        currentUserHasPin = hasPin,
                        cashierPerms = cashierPerms,
                        effectivePermissions = effectivePermissions,
                    )
                }
                // Observe orders to auto-refresh dashboard data
                try {
                    orderRepository.observeOrders(store.id).collect {
                        try {
                            val dashboard = orderRepository.getDashboardData(store.id)
                            _state.update { s ->
                                s.copy(
                                    todayRevenue = dashboard.todayRevenue,
                                    todayOrders = dashboard.todayOrders,
                                    lowStockCount = dashboard.lowStockCount,
                                )
                            }
                        } catch (_: Exception) { /* dashboard refresh failed, non-fatal */ }
                    }
                } catch (_: Exception) { /* prevent crash from reactive observer */ }
            }
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            val dashboard = orderRepository.getDashboardData(store.id)
            _state.update {
                it.copy(
                    todayRevenue = dashboard.todayRevenue,
                    todayOrders = dashboard.todayOrders,
                    lowStockCount = dashboard.lowStockCount,
                )
            }
        }
    }

    /** Reload current user info — call on screen resume after switch account */
    fun refreshCurrentUser() {
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first() ?: return@launch
            val currentUser = userRepository.getUserById(userId) ?: return@launch
            val hasPin = userRepository.hasPin(userId)
            val store = storeRepository.getStore()
            val cashierPerms = store?.settings?.cashierPermissions ?: CashierPermissions()
            val effectivePermissions = PermissionChecker.getEffectivePermissions(currentUser.role, cashierPerms)
            _state.update {
                it.copy(
                    userName = currentUser.displayName,
                    userRole = currentUser.role,
                    currentUser = currentUser,
                    currentUserHasPin = hasPin,
                    cashierPerms = cashierPerms,
                    effectivePermissions = effectivePermissions,
                )
            }
        }
    }

    // ── Setup Guide ──

    private fun loadSetupGuide() {
        viewModelScope.launch {
            val dismissed = appPreferences.setupGuideDismissed.first()
            if (dismissed) {
                _state.update { it.copy(setupGuideVisible = false) }
                return@launch
            }
            val store = storeRepository.getStore() ?: return@launch
            try {
                combine(
                    categoryRepository.observeCategories(store.id),
                    productRepository.observeProducts(store.id),
                    supplierRepository.observeSuppliers(store.id),
                    orderRepository.observeOrders(store.id),
                ) { categories, products, suppliers, orders ->
                    val allDone = categories.isNotEmpty() && products.isNotEmpty() &&
                            suppliers.isNotEmpty() && orders.isNotEmpty()
                    _state.value.copy(
                        setupGuideVisible = !allDone,
                        setupCategoryCount = categories.size,
                        setupProductCount = products.size,
                        setupSupplierCount = suppliers.size,
                        setupOrderCount = orders.size,
                    )
                }.collect { guideState ->
                    _state.update {
                        it.copy(
                            setupGuideVisible = guideState.setupGuideVisible,
                            setupCategoryCount = guideState.setupCategoryCount,
                            setupProductCount = guideState.setupProductCount,
                            setupSupplierCount = guideState.setupSupplierCount,
                            setupOrderCount = guideState.setupOrderCount,
                        )
                    }
                }
            } catch (_: Exception) { /* prevent crash from reactive observer */ }
        }
    }

    fun dismissSetupGuide() {
        viewModelScope.launch {
            appPreferences.setSetupGuideDismissed(true)
            _state.update { it.copy(setupGuideVisible = false) }
        }
    }

    // ── PIN management ──
    fun showChangePinSheet() {
        _state.update { it.copy(showChangePinSheet = true, pinVerified = false, pinVerifyError = null, pinMessage = null) }
    }

    fun dismissChangePinSheet() {
        _state.update { it.copy(showChangePinSheet = false, pinVerified = false, pinVerifyError = null) }
    }

    fun verifyCurrentPin(pin: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val valid = userRepository.verifyPin(user.id, pin)
            if (valid) {
                _state.update { it.copy(pinVerified = true, pinVerifyError = null) }
            } else {
                _state.update { it.copy(pinVerifyError = application.getString(R.string.msg_pin_incorrect)) }
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
                            showChangePinSheet = false,
                            pinVerified = false,
                            pinVerifyError = null,
                            currentUserHasPin = true,
                            pinMessage = application.getString(R.string.msg_pin_updated),
                        )
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(pinMessage = result.message) }
                }
            }
        }
    }
}
