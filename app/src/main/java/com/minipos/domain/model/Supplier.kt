package com.minipos.domain.model

data class Supplier(
    val id: String,
    val storeId: String,
    val name: String,
    val contactPerson: String? = null,
    val phone: String? = null,
    val mobile: String? = null,
    val email: String? = null,
    val address: String? = null,
    val taxCode: String? = null,
    val paymentTerm: String? = null,
    val bankName: String? = null,
    val bankAccount: String? = null,
    val bankAccountHolder: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
