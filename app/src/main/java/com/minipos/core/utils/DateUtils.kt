package com.minipos.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thread-safe date formatting utilities.
 * SimpleDateFormat is NOT thread-safe, so we use ThreadLocal to avoid
 * crashes and corrupted output when called from multiple coroutines.
 */
object DateUtils {
    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
    }
    private val dateTimeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
    }
    private val timeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm", Locale("vi", "VN"))
    }
    private val orderDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyyMMdd", Locale.US)
    }

    fun now(): Long = System.currentTimeMillis()

    fun formatDate(timestamp: Long): String = dateFormat.get()!!.format(Date(timestamp))

    fun formatDateTime(timestamp: Long): String = dateTimeFormat.get()!!.format(Date(timestamp))

    fun formatTime(timestamp: Long): String = timeFormat.get()!!.format(Date(timestamp))

    fun formatOrderDate(timestamp: Long): String = orderDateFormat.get()!!.format(Date(timestamp))

    fun startOfDay(timestamp: Long): Long {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayStr = fmt.format(Date(timestamp))
        return fmt.parse(dayStr)?.time ?: timestamp
    }

    fun endOfDay(timestamp: Long): Long {
        return startOfDay(timestamp) + 24 * 60 * 60 * 1000 - 1
    }
}
