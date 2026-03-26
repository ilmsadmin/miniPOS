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
    val visitCount: Int = 0,
    val lastVisitAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
