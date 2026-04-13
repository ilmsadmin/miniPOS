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
    // Cashier custom permissions (configurable by Owner)
    val cashierPermissions: CashierPermissions = CashierPermissions(),
)

/**
 * Cài đặt quyền tùy chỉnh cho vai trò Thu ngân (Cashier).
 * Owner có thể bật/tắt từng quyền này trong Settings.
 */
data class CashierPermissions(
    /** Cho phép thu ngân áp dụng giảm giá */
    val canApplyDiscount: Boolean = false,
    /** Giảm giá tối đa (%) — chỉ có hiệu lực khi canApplyDiscount = true; -1 = không giới hạn */
    val maxDiscountPercent: Int = 10,
    /** Cho phép thu ngân sửa giá bán trong đơn */
    val canEditPrice: Boolean = false,
    /** Cho phép thu ngân xem tồn kho */
    val canViewStock: Boolean = false,
    /** Cho phép thu ngân xem giá nhập (cost price) */
    val canViewCostPrice: Boolean = false,
    /** Cho phép thu ngân hủy đơn hàng */
    val canCancelOrder: Boolean = false,
    /** Yêu cầu xác nhận Manager/Owner để hoàn trả hàng */
    val requireApprovalForRefund: Boolean = true,
    /** Cho phép thu ngân xem tất cả đơn hàng (không chỉ đơn của mình) */
    val canViewAllOrders: Boolean = false,
)
