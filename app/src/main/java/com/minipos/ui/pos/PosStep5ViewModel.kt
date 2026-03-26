package com.minipos.ui.pos

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PosStep5ViewModel @Inject constructor(
    private val cartHolder: PosCartHolder,
) : ViewModel() {

    fun clearCartAndNavigate(navigate: () -> Unit) {
        cartHolder.clearCart()
        navigate()
    }
}
