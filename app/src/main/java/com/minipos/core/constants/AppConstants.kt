package com.minipos.core.constants

object AppConstants {
    const val APP_NAME = "miniPOS"
    const val APP_VERSION = "1.0.0"
    const val DB_NAME = "minipos.db"
    const val DB_VERSION = 1

    // PIN
    const val PIN_MIN_LENGTH = 4
    const val PIN_MAX_LENGTH = 6
    const val PASSWORD_MIN_LENGTH = 6
    const val MAX_LOGIN_ATTEMPTS = 5
    const val LOCK_DURATION_MINUTES = 5

    // Session timeouts (ms)
    const val SESSION_TIMEOUT_CASHIER = 5 * 60 * 1000L
    const val SESSION_TIMEOUT_MANAGER = 15 * 60 * 1000L
    const val SESSION_TIMEOUT_OWNER = 30 * 60 * 1000L

    // Pagination
    const val DEFAULT_PAGE_SIZE = 50

    // Store code
    const val STORE_CODE_MIN_LENGTH = 4
    const val STORE_CODE_MAX_LENGTH = 8

    // Currency
    const val DEFAULT_CURRENCY = "VND"

    // Order code prefix
    const val ORDER_CODE_PREFIX = "HD"
    const val PURCHASE_ORDER_CODE_PREFIX = "NK"

    // Default units
    val DEFAULT_UNITS = listOf(
        "cái", "chiếc", "hộp", "gói", "kg", "g",
        "lít", "ml", "chai", "lon", "túi", "bịch", "thùng", "bộ"
    )
}
