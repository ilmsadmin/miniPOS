package com.minipos.domain.model

data class InventoryItem(
    val id: String,
    val storeId: String,
    val productId: String,
    val variantId: String? = null,
    val quantity: Double = 0.0,
    val reservedQty: Double = 0.0,
)

data class StockMovement(
    val id: String,
    val storeId: String,
    val productId: String,
    val variantId: String? = null,
    val supplierId: String? = null,
    val type: StockMovementType,
    val quantity: Double,
    val quantityBefore: Double,
    val quantityAfter: Double,
    val unitCost: Double? = null,
    val referenceId: String? = null,
    val referenceType: String? = null,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long = 0,
)

enum class StockMovementType {
    PURCHASE_IN,
    SALE_OUT,
    RETURN_IN,
    RETURN_OUT,
    ADJUSTMENT_IN,
    ADJUSTMENT_OUT,
    DAMAGE_OUT,
    TRANSFER;

    companion object {
        fun fromString(value: String): StockMovementType = when (value.lowercase()) {
            "purchase_in" -> PURCHASE_IN
            "sale_out" -> SALE_OUT
            "return_in" -> RETURN_IN
            "return_out" -> RETURN_OUT
            "adjustment_in" -> ADJUSTMENT_IN
            "adjustment_out" -> ADJUSTMENT_OUT
            "damage_out" -> DAMAGE_OUT
            "transfer" -> TRANSFER
            else -> ADJUSTMENT_IN
        }
    }
}

data class DashboardData(
    val todayRevenue: Double = 0.0,
    val todayOrders: Int = 0,
    val todayProfit: Double = 0.0,
    val lowStockCount: Int = 0,
)

data class StockOverviewItem(
    val productId: String,
    val productName: String,
    val productSku: String,
    val productUnit: String,
    val minStock: Int,
    val costPrice: Double,
    val sellingPrice: Double,
    val currentStock: Double,
    val stockValue: Double, // currentStock * costPrice
)

data class StockHistoryItem(
    val id: String,
    val productId: String,
    val productName: String,
    val productSku: String,
    val type: StockMovementType,
    val quantity: Double,
    val quantityBefore: Double,
    val quantityAfter: Double,
    val unitCost: Double? = null,
    val referenceId: String? = null,
    val referenceType: String? = null,
    val supplierId: String? = null,
    val supplierName: String? = null,
    val notes: String?,
    val createdBy: String,
    val createdAt: Long,
)

data class StockSummary(
    val totalProducts: Int = 0,
    val totalStockValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalStockIn: Double = 0.0,
    val totalStockOut: Double = 0.0,
)
