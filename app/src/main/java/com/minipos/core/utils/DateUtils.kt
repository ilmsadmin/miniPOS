package com.minipos.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
    private val orderDateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    fun now(): Long = System.currentTimeMillis()

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun formatOrderDate(timestamp: Long): String = orderDateFormat.format(Date(timestamp))

    fun startOfDay(timestamp: Long): Long {
        val date = Date(timestamp)
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayStr = fmt.format(date)
        return fmt.parse(dayStr)?.time ?: timestamp
    }

    fun endOfDay(timestamp: Long): Long {
        return startOfDay(timestamp) + 24 * 60 * 60 * 1000 - 1
    }
}
