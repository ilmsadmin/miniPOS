package com.minipos.domain.model

import android.content.Context
import com.minipos.R

data class Order(
    val id: String,
    val storeId: String,
    val orderCode: String,
    val customerId: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val subtotal: Double = 0.0,
    val discountType: String? = null,
    val discountValue: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: OrderStatus = OrderStatus.COMPLETED,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

enum class OrderStatus {
    COMPLETED, REFUNDED, PARTIALLY_REFUNDED, CANCELLED;

    companion object {
        fun fromString(value: String): OrderStatus = when (value.lowercase()) {
            "completed" -> COMPLETED
            "refunded" -> REFUNDED
            "partially_refunded" -> PARTIALLY_REFUNDED
            "cancelled" -> CANCELLED
            else -> COMPLETED
        }
    }
}

data class OrderItem(
    val id: String,
    val orderId: String,
    val productId: String,
    val variantId: String? = null,
    val productName: String,
    val variantName: String? = null,
    val quantity: Double,
    val unitPrice: Double,
    val costPrice: Double = 0.0,
    val discountType: String? = null,
    val discountValue: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalPrice: Double,
)

data class OrderPayment(
    val id: String,
    val orderId: String,
    val method: PaymentMethod,
    val amount: Double,
    val receivedAmount: Double? = null,
    val changeAmount: Double = 0.0,
    val referenceNo: String? = null,
    val notes: String? = null,
)

enum class PaymentMethod {
    CASH, TRANSFER, EWALLET, OTHER;

    companion object {
        fun fromString(value: String): PaymentMethod = when (value.lowercase()) {
            "cash" -> CASH
            "transfer" -> TRANSFER
            "ewallet" -> EWALLET
            else -> OTHER
        }
    }

    fun displayName(context: Context): String = when (this) {
        CASH -> context.getString(R.string.payment_method_cash)
        TRANSFER -> context.getString(R.string.payment_method_transfer)
        EWALLET -> context.getString(R.string.payment_method_ewallet)
        OTHER -> context.getString(R.string.payment_method_other)
    }
}

data class OrderDetail(
    val order: Order,
    val items: List<OrderItem>,
    val payments: List<OrderPayment>,
)
