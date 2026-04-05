package com.minipos.domain.model

data class Customer(
    val id: String,
    val storeId: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val totalSpent: Double = 0.0,
    val debtAmount: Double = 0.0,
    val visitCount: Int = 0,
    val lastVisitAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    /** Two-letter avatar initials from customer name */
    val initials: String
        get() {
            val parts = name.trim().split("\\s+".toRegex())
            return when {
                parts.size >= 2 -> "${parts.first().first().uppercase()}${parts.last().first().uppercase()}"
                name.isNotBlank() -> name.take(2).uppercase()
                else -> "?"
            }
        }

    val hasDebt: Boolean get() = debtAmount > 0
}
