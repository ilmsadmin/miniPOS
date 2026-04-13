package com.minipos.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.model.OrderPayment
import com.minipos.domain.model.PaymentMethod
import com.minipos.domain.model.Result
import com.minipos.domain.repository.OrderRepository
import com.minipos.data.preferences.AppPreferences
import com.minipos.core.utils.UuidGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

data class PosStep4State(
    val selectedMethod: PaymentMethod = PaymentMethod.CASH,
    val receivedAmountText: String = "",
    val changeAmount: Double = 0.0,
    val notes: String = "",
    val isProcessing: Boolean = false,
    val error: String? = null,
) {
    fun canConfirm(grandTotal: Double): Boolean {
        return when (selectedMethod) {
            PaymentMethod.CASH -> {
                val received = receivedAmountText.toDoubleOrNull() ?: 0.0
                received >= grandTotal
            }
            else -> true
        }
    }
}

@HiltViewModel
class PosStep4ViewModel @Inject constructor(
    val cartHolder: PosCartHolder,
    private val orderRepository: OrderRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(PosStep4State())
    val state: StateFlow<PosStep4State> = _state

    fun selectMethod(method: PaymentMethod) {
        _state.update {
            it.copy(
                selectedMethod = method,
                receivedAmountText = "",
                changeAmount = 0.0,
            )
        }
    }

    fun updateReceivedAmount(text: String) {
        val cleaned = text.filter { it.isDigit() }
        val amount = cleaned.toDoubleOrNull() ?: 0.0
        val grandTotal = cartHolder.cart.value.grandTotal
        _state.update {
            it.copy(
                receivedAmountText = cleaned,
                changeAmount = (amount - grandTotal).coerceAtLeast(0.0),
            )
        }
    }

    fun updateNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    fun getQuickAmounts(grandTotal: Double): List<Double> {
        if (grandTotal <= 0) return emptyList()
        val rounded = ceil(grandTotal / 1000) * 1000
        val amounts = mutableListOf<Double>()
        amounts.add(rounded)
        val steps = listOf(10_000.0, 20_000.0, 50_000.0, 100_000.0, 200_000.0, 500_000.0)
        for (step in steps) {
            val v = ceil(grandTotal / step) * step
            if (v > rounded && v !in amounts) {
                amounts.add(v)
            }
            if (amounts.size >= 6) break
        }
        return amounts.distinct().sorted().take(6)
    }

    fun confirmPayment(grandTotal: Double, onSuccess: () -> Unit) {
        val currentState = _state.value
        if (currentState.isProcessing) return

        _state.update { it.copy(isProcessing = true, error = null) }

        viewModelScope.launch {
            try {
                val storeId = appPreferences.currentStoreId.first() ?: throw Exception("Không tìm thấy cửa hàng")
                val userId = appPreferences.currentUserId.first() ?: throw Exception("Không tìm thấy người dùng")

                val receivedAmount = if (currentState.selectedMethod == PaymentMethod.CASH) {
                    currentState.receivedAmountText.toDoubleOrNull() ?: grandTotal
                } else grandTotal

                val payment = OrderPayment(
                    id = UuidGenerator.generate(),
                    orderId = "", // will be set by repository
                    method = currentState.selectedMethod,
                    amount = grandTotal,
                    receivedAmount = receivedAmount,
                    changeAmount = currentState.changeAmount,
                    notes = currentState.notes.ifBlank { null },
                )

                val cart = cartHolder.cart.value
                val result = orderRepository.createOrder(
                    cart = cart,
                    userId = userId,
                    storeId = storeId,
                    payments = listOf(payment),
                )

                when (result) {
                    is Result.Success -> {
                        cartHolder.setLastOrderId(result.data.id)
                        _state.update { it.copy(isProcessing = false) }
                        onSuccess()
                    }
                    is Result.Error -> {
                        _state.update { it.copy(isProcessing = false, error = result.message) }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = e.message ?: "Unknown error") }
            }
        }
    }
}
