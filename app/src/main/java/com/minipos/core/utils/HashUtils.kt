package com.minipos.core.utils

import at.favre.lib.crypto.bcrypt.BCrypt

object HashUtils {
    private const val PIN_COST = 10
    private const val PASSWORD_COST = 12

    fun hashPin(pin: String): String {
        return BCrypt.withDefaults().hashToString(PIN_COST, pin.toCharArray())
    }

    fun verifyPin(pin: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(pin.toCharArray(), hash).verified
    }

    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(PASSWORD_COST, password.toCharArray())
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }
}
