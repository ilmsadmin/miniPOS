package com.minipos.core.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val vnFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    fun format(amount: Double): String {
        return "${vnFormat.format(amount)}đ"
    }

    fun formatCompact(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> "${vnFormat.format(amount / 1_000_000_000)}tỷ"
            amount >= 1_000_000 -> "${vnFormat.format(amount / 1_000_000)}tr"
            amount >= 1_000 -> "${vnFormat.format(amount / 1_000)}k"
            else -> "${vnFormat.format(amount)}đ"
        }
    }
}
