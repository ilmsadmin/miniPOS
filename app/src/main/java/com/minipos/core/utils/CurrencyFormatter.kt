package com.minipos.core.utils

import android.content.Context
import com.minipos.R
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val vnFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    fun format(amount: Double): String {
        return "${vnFormat.format(amount)}đ"
    }

    fun format(context: Context, amount: Double): String {
        val symbol = context.getString(R.string.currency_symbol)
        return "${vnFormat.format(amount)}$symbol"
    }

    fun formatCompact(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> "${vnFormat.format(amount / 1_000_000_000)}tỷ"
            amount >= 1_000_000 -> "${vnFormat.format(amount / 1_000_000)}tr"
            amount >= 1_000 -> "${vnFormat.format(amount / 1_000)}k"
            else -> "${vnFormat.format(amount)}đ"
        }
    }

    fun formatCompact(context: Context, amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> context.getString(R.string.currency_compact_billion, vnFormat.format(amount / 1_000_000_000))
            amount >= 1_000_000 -> context.getString(R.string.currency_compact_million, vnFormat.format(amount / 1_000_000))
            amount >= 1_000 -> context.getString(R.string.currency_compact_thousand, vnFormat.format(amount / 1_000))
            else -> format(context, amount)
        }
    }
}
