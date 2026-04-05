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
    // New fields for store settings screen
    val showLogoOnReceipt: Boolean = true,
    val receiptThankYou: String = "",
    val allowDebt: Boolean = true,
    val salesSound: Boolean = false,
    val defaultLowStockLevel: Int = 20,
    val openTime: String = "06:00",
    val closeTime: String = "22:00",
)
