package com.minipos.domain.model

/**
 * Result wrapper for all service operations
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(
        val code: ErrorCode,
        val message: String,
        val details: Map<String, Any>? = null
    ) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw AppException(code, message)
    }

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
}

enum class ErrorCode {
    // Validation
    INVALID_INPUT,
    DUPLICATE_ENTRY,

    // Auth
    INVALID_PIN,
    ACCOUNT_LOCKED,
    ACCOUNT_DISABLED,
    INSUFFICIENT_PERMISSION,

    // Business
    CATEGORY_HAS_PRODUCTS,
    INSUFFICIENT_STOCK,
    ORDER_NOT_FOUND,
    ORDER_ALREADY_REFUNDED,

    // Sync
    DEVICE_NOT_FOUND,
    SYNC_CONFLICT,
    CONNECTION_FAILED,

    // Backup
    GOOGLE_AUTH_FAILED,
    BACKUP_IN_PROGRESS,
    INVALID_BACKUP_FILE,
    WRONG_BACKUP_PASSWORD,

    // System
    DATABASE_ERROR,
    UNKNOWN_ERROR
}

class AppException(
    val code: ErrorCode,
    override val message: String
) : Exception(message)
