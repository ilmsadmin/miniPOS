package com.minipos.domain.model

data class Store(
    val id: String,
    val name: String,
    val code: String,
    val address: String? = null,
    val phone: String? = null,
    val logoPath: String? = null,
    val settings: StoreSettings = StoreSettings(),
    val currency: String = "VND",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class StoreSettings(
    val receiptHeader: String = "",
    val receiptFooter: String = "",
    val taxEnabled: Boolean = false,
    val defaultTaxRate: Double = 0.0,
    val lowStockAlert: Boolean = true,
    val autoPrintReceipt: Boolean = false,
)
