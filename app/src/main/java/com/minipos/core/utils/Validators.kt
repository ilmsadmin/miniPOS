package com.minipos.core.utils

import com.minipos.core.constants.AppConstants

object Validators {
    fun isValidStoreName(name: String): Boolean =
        name.isNotBlank() && name.length <= 100

    fun isValidStoreCode(code: String): Boolean =
        code.length in AppConstants.STORE_CODE_MIN_LENGTH..AppConstants.STORE_CODE_MAX_LENGTH &&
                code.matches(Regex("^[A-Za-z0-9]+$"))

    fun isValidPin(pin: String): Boolean =
        pin.length in AppConstants.PIN_MIN_LENGTH..AppConstants.PIN_MAX_LENGTH &&
                pin.all { it.isDigit() } &&
                !isSequentialPin(pin) &&
                !isRepeatedPin(pin)

    fun isValidPassword(password: String): Boolean =
        password.length >= AppConstants.PASSWORD_MIN_LENGTH

    fun isValidDisplayName(name: String): Boolean =
        name.isNotBlank() && name.trim().length in 1..100

    fun isValidPhone(phone: String): Boolean =
        phone.matches(Regex("^0\\d{9}$"))

    fun isValidEmail(email: String): Boolean =
        email.isEmpty() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun isValidPrice(price: Double): Boolean = price >= 0 && price.isFinite()

    fun isValidQuantity(qty: Double): Boolean = qty > 0 && qty.isFinite()

    /** Reject PINs like 1234, 4321 — too easy to guess */
    private fun isSequentialPin(pin: String): Boolean {
        if (pin.length < 3) return false
        val ascending = pin.zipWithNext().all { (a, b) -> b - a == 1 }
        val descending = pin.zipWithNext().all { (a, b) -> a - b == 1 }
        return ascending || descending
    }

    /** Reject PINs like 0000, 1111 — too easy to guess */
    private fun isRepeatedPin(pin: String): Boolean {
        return pin.all { it == pin[0] }
    }
}
