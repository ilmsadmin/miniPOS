package com.minipos.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.minipos.domain.model.Discount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PosStep2ViewModel @Inject constructor(
    val cartHolder: PosCartHolder,
) : ViewModel() {

    private val _showOrderDiscount = MutableStateFlow(false)
    val showOrderDiscount: StateFlow<Boolean> = _showOrderDiscount

    private val _stockError = MutableStateFlow<String?>(null)
    val stockError: StateFlow<String?> = _stockError

    init {
        viewModelScope.launch {
            cartHolder.stockError.collect { error ->
                _stockError.value = error
            }
        }
    }

    fun updateQuantity(index: Int, quantity: Double) {
        cartHolder.updateItemQuantity(index, quantity)
    }

    fun updatePrice(index: Int, price: Double) {
        cartHolder.updateItemPrice(index, price)
    }

    fun removeItem(index: Int) {
        cartHolder.removeItem(index)
    }

    fun setOrderDiscount(discount: Discount?) {
        cartHolder.setOrderDiscount(discount)
    }

    fun clearStockError() {
        cartHolder.clearStockError()
        _stockError.value = null
    }

    fun showOrderDiscountDialog() { _showOrderDiscount.value = true }
    fun dismissOrderDiscountDialog() { _showOrderDiscount.value = false }
}
