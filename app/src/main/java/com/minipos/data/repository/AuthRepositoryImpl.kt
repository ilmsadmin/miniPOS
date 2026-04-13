package com.minipos.data.repository

import android.content.Context
import com.minipos.R
import com.minipos.core.utils.DateUtils
import com.minipos.core.utils.HashUtils
import com.minipos.core.utils.UuidGenerator
import com.minipos.data.database.dao.*
import com.minipos.data.database.entity.*
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.model.*
import com.minipos.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storeDao: StoreDao,
    private val userDao: UserDao,
    private val inventoryDao: InventoryDao,
    private val prefs: AppPreferences,
) : AuthRepository {

    override suspend fun createStore(
        storeName: String, storeCode: String, address: String?, phone: String?,
        ownerName: String, ownerPin: String, ownerPassword: String
    ): Result<Store> {
        return try {
            val existing = storeDao.getStoreByCode(storeCode.uppercase())
            if (existing != null) {
                return Result.Error(ErrorCode.DUPLICATE_ENTRY, context.getString(R.string.error_store_code_exists))
            }

            val now = DateUtils.now()
            val deviceId = prefs.getDeviceIdSync()
            val storeId = UuidGenerator.generate()
            val userId = UuidGenerator.generate()

            val storeEntity = StoreEntity(
                id = storeId, name = storeName, code = storeCode.uppercase(),
                address = address, phone = phone, currency = "VND",
                createdAt = now, updatedAt = now, deviceId = deviceId,
            )
            storeDao.insert(storeEntity)

            val userEntity = UserEntity(
                id = userId, storeId = storeId, displayName = ownerName,
                role = "owner",
                pinHash = if (ownerPin.isNotBlank()) HashUtils.hashPin(ownerPin) else "",
                passwordHash = if (ownerPassword.isNotBlank()) HashUtils.hashPassword(ownerPassword) else "",
                isActive = true, createdAt = now, updatedAt = now, deviceId = deviceId,
            )
            userDao.insert(userEntity)

            // Owner tự động đăng nhập sau khi tạo store
            prefs.setCurrentStore(storeId)
            prefs.setCurrentUser(userId)
            prefs.setOnboarded(true)
            prefs.setLoggedIn(true)

            val store = Store(
                id = storeId, name = storeName, code = storeCode.uppercase(),
                address = address, phone = phone, createdAt = now, updatedAt = now,
            )
            Result.Success(store)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_create_store))
        }
    }

    override suspend fun login(userId: String, pin: String): Result<AuthSession> {
        return try {
            val lockUntil = prefs.getLockUntil()
            if (lockUntil > DateUtils.now()) {
                val remainingMinutes = ((lockUntil - DateUtils.now()) / 60000).toInt() + 1
                return Result.Error(ErrorCode.ACCOUNT_LOCKED, context.getString(R.string.error_account_locked_minutes, remainingMinutes))
            }

            val user = userDao.getById(userId)
                ?: return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_account_not_found))

            if (!user.isActive) {
                return Result.Error(ErrorCode.ACCOUNT_DISABLED, context.getString(R.string.error_account_disabled))
            }

            if (user.pinHash.isBlank()) {
                return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_account_no_pin))
            }

            if (!HashUtils.verifyPin(pin, user.pinHash)) {
                val attempts = prefs.getLoginAttempts() + 1
                prefs.setLoginAttempts(attempts)
                if (attempts >= 5) {
                    prefs.setLockUntil(DateUtils.now() + 5 * 60 * 1000)
                    prefs.setLoginAttempts(0)
                    return Result.Error(ErrorCode.ACCOUNT_LOCKED, context.getString(R.string.error_account_locked_5min))
                }
                return Result.Error(ErrorCode.INVALID_PIN, context.getString(R.string.error_wrong_pin_attempts, 5 - attempts))
            }

            // Success
            prefs.setLoginAttempts(0)
            prefs.setLockUntil(0)
            val now = DateUtils.now()
            userDao.updateLastLogin(userId, now)
            prefs.setCurrentUser(userId)
            prefs.setLoggedIn(true)

            val session = AuthSession(
                userId = userId, storeId = user.storeId,
                role = UserRole.fromString(user.role),
                displayName = user.displayName, loginAt = now,
            )
            Result.Success(session)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_login))
        }
    }

    override suspend fun logout() {
        prefs.clearCurrentUser()
    }

    override suspend fun loginWithPassword(userId: String, password: String): Result<AuthSession> {
        return try {
            val user = userDao.getById(userId)
                ?: return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_account_not_found))

            if (!user.isActive) {
                return Result.Error(ErrorCode.ACCOUNT_DISABLED, context.getString(R.string.error_account_disabled))
            }

            if (user.role.lowercase() != "owner") {
                return Result.Error(ErrorCode.PERMISSION_DENIED, context.getString(R.string.error_owner_only))
            }

            val hash = user.passwordHash
            if (hash.isNullOrBlank()) {
                return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_no_password_set))
            }

            if (!HashUtils.verifyPassword(password, hash)) {
                return Result.Error(ErrorCode.INVALID_PIN, context.getString(R.string.error_wrong_password))
            }

            // Success
            prefs.setLoginAttempts(0)
            prefs.setLockUntil(0)
            val now = DateUtils.now()
            userDao.updateLastLogin(userId, now)
            prefs.setCurrentUser(userId)
            prefs.setLoggedIn(true)

            val session = AuthSession(
                userId = userId, storeId = user.storeId,
                role = UserRole.fromString(user.role),
                displayName = user.displayName, loginAt = now,
            )
            Result.Success(session)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_login))
        }
    }

    override suspend fun resetPinWithPassword(userId: String, password: String, newPin: String): Result<Unit> {
        return try {
            val user = userDao.getById(userId)
                ?: return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_account_not_found))

            if (user.role.lowercase() != "owner") {
                return Result.Error(ErrorCode.PERMISSION_DENIED, context.getString(R.string.error_owner_only))
            }

            val hash = user.passwordHash
            if (hash.isNullOrBlank()) {
                return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_no_password_set))
            }

            if (!HashUtils.verifyPassword(password, hash)) {
                return Result.Error(ErrorCode.INVALID_PIN, context.getString(R.string.error_wrong_password))
            }

            if (newPin.length < 4 || newPin.length > 6 || !newPin.all { it.isDigit() }) {
                return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_pin_length))
            }

            val newHash = HashUtils.hashPin(newPin)
            userDao.updatePinHash(userId, newHash, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_reset_pin))
        }
    }

    override suspend fun autoLogin() {
        // Không tự set currentUserId — tránh bypass màn hình chọn user khi có nhiều user.
        // currentUserId chỉ được set khi user chủ động đăng nhập qua LoginScreen.
    }

    override suspend fun ensureDefaultStore() {
        if (storeDao.getStore() != null) {
            autoLogin()
            return
        }
        // Legacy fallback: tạo store mặc định nếu chưa có
        val now = DateUtils.now()
        val deviceId = prefs.getDeviceIdSync()
        val storeId = UuidGenerator.generate()
        val userId = UuidGenerator.generate()
        storeDao.insert(
            StoreEntity(
                id = storeId, name = "My Store", code = "STORE1",
                address = null, phone = null, currency = "VND",
                createdAt = now, updatedAt = now, deviceId = deviceId,
            )
        )
        userDao.insert(
            UserEntity(
                id = userId, storeId = storeId, displayName = "Owner",
                role = "owner", pinHash = "", passwordHash = "",
                isActive = true, createdAt = now, updatedAt = now, deviceId = deviceId,
            )
        )
        prefs.setCurrentStore(storeId)
        prefs.setCurrentUser(userId)
        prefs.setOnboarded(true)
    }

    override suspend fun getCurrentSession(): AuthSession? {
        val userId = prefs.getCurrentUserIdSync() ?: return null
        val user = userDao.getById(userId) ?: return null
        return AuthSession(
            userId = user.id, storeId = user.storeId,
            role = UserRole.fromString(user.role),
            displayName = user.displayName,
            loginAt = user.lastLoginAt ?: 0,
        )
    }

    override fun isOnboarded(): Flow<Boolean> = prefs.isOnboarded
}
