package com.minipos.domain.model

/**
 * In-memory cart for POS flow.
 * Uses immutable List to ensure StateFlow detects changes via structural equality.
 */
data class Cart(
    val items: List<CartItem> = emptyList(),
    val orderDiscount: Discount? = null,
    val customer: Customer? = null,
    val notes: String? = null,
) {
    val itemCount: Int get() = items.size
    val totalQuantity: Double get() = items.sumOf { it.quantity }

    val subtotal: Double get() = items.sumOf { it.lineTotal }

    val itemDiscountTotal: Double get() = items.sumOf { it.discountAmount }

    val orderDiscountAmount: Double
        get() = when (orderDiscount?.type) {
            "percent" -> subtotal * (orderDiscount.value / 100.0)
            "fixed" -> orderDiscount.value.coerceAtMost(subtotal)
            else -> 0.0
        }

    val taxAmount: Double get() = items.sumOf { it.taxAmount }

    val grandTotal: Double get() = subtotal - orderDiscountAmount + taxAmount

    fun isEmpty(): Boolean = items.isEmpty()
}

data class CartItem(
    val product: Product,
    val variant: ProductVariant? = null,
    val quantity: Double = 1.0,
    val unitPrice: Double,
    val originalPrice: Double,
    val discount: Discount? = null,
) {
    val discountAmount: Double
        get() = when (discount?.type) {
            "percent" -> unitPrice * quantity * (discount.value / 100.0)
            "fixed" -> discount.value.coerceAtMost(unitPrice * quantity)
            else -> 0.0
        }

    val taxAmount: Double
        get() = (unitPrice * quantity - discountAmount) * (product.taxRate / 100.0)

    val lineTotal: Double
        get() = unitPrice * quantity - discountAmount
}

data class Discount(
    val type: String, // "percent" or "fixed"
    val value: Double,
)
