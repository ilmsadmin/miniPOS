package com.minipos.domain.model

data class Category(
    val id: String,
    val storeId: String,
    val parentId: String? = null,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
