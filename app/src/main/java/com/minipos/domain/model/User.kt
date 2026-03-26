package com.minipos.domain.model

enum class UserRole {
    OWNER, MANAGER, CASHIER;

    companion object {
        fun fromString(value: String): UserRole = when (value.lowercase()) {
            "owner" -> OWNER
            "manager" -> MANAGER
            "cashier" -> CASHIER
            else -> CASHIER
        }
    }
}

data class User(
    val id: String,
    val storeId: String,
    val displayName: String,
    val avatarPath: String? = null,
    val role: UserRole,
    val isActive: Boolean = true,
    val lastLoginAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class AuthSession(
    val userId: String,
    val storeId: String,
    val role: UserRole,
    val displayName: String,
    val loginAt: Long,
)
