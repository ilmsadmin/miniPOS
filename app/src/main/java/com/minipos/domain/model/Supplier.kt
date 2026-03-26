package com.minipos.domain.model

data class Supplier(
    val id: String,
    val storeId: String,
    val name: String,
    val contactPerson: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val taxCode: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
