package com.minipos.data.repository

import com.minipos.core.utils.DateUtils
import com.minipos.core.utils.HashUtils
import com.minipos.core.utils.UuidGenerator
import com.minipos.data.database.dao.*
import com.minipos.data.database.entity.*
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.model.*
import com.minipos.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
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
            // Check if store code already exists
            val existing = storeDao.getStoreByCode(storeCode.uppercase())
            if (existing != null) {
                return Result.Error(ErrorCode.DUPLICATE_ENTRY, "Mã cửa hàng đã tồn tại")
            }

            val now = DateUtils.now()
            val deviceId = prefs.getDeviceIdSync()
            val storeId = UuidGenerator.generate()
            val userId = UuidGenerator.generate()

            // Create store
            val storeEntity = StoreEntity(
                id = storeId,
                name = storeName,
                code = storeCode.uppercase(),
                address = address,
                phone = phone,
                currency = "VND",
                createdAt = now,
                updatedAt = now,
                deviceId = deviceId,
            )
            storeDao.insert(storeEntity)

            // Create owner user
            val userEntity = UserEntity(
                id = userId,
                storeId = storeId,
                displayName = ownerName,
                role = "owner",
                pinHash = HashUtils.hashPin(ownerPin),
                passwordHash = HashUtils.hashPassword(ownerPassword),
                isActive = true,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceId,
            )
            userDao.insert(userEntity)

            // Save session
            prefs.setCurrentStore(storeId)
            prefs.setCurrentUser(userId)
            prefs.setOnboarded(true)

            val store = Store(
                id = storeId,
                name = storeName,
                code = storeCode.uppercase(),
                address = address,
                phone = phone,
                createdAt = now,
                updatedAt = now,
            )
            Result.Success(store)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi tạo cửa hàng")
        }
    }

    override suspend fun login(userId: String, pin: String): Result<AuthSession> {
        return try {
            // Check lock
            val lockUntil = prefs.getLockUntil()
            if (lockUntil > DateUtils.now()) {
                val remainingMinutes = ((lockUntil - DateUtils.now()) / 60000).toInt() + 1
                return Result.Error(ErrorCode.ACCOUNT_LOCKED, "Tài khoản bị khóa. Thử lại sau $remainingMinutes phút")
            }

            val user = userDao.getById(userId)
                ?: return Result.Error(ErrorCode.INVALID_INPUT, "Tài khoản không tồn tại")

            if (!user.isActive) {
                return Result.Error(ErrorCode.ACCOUNT_DISABLED, "Tài khoản đã bị vô hiệu hóa")
            }

            if (!HashUtils.verifyPin(pin, user.pinHash)) {
                val attempts = prefs.getLoginAttempts() + 1
                prefs.setLoginAttempts(attempts)
                if (attempts >= 5) {
                    prefs.setLockUntil(DateUtils.now() + 5 * 60 * 1000)
                    prefs.setLoginAttempts(0)
                    return Result.Error(ErrorCode.ACCOUNT_LOCKED, "Tài khoản bị khóa 5 phút sau 5 lần sai")
                }
                return Result.Error(ErrorCode.INVALID_PIN, "PIN không đúng. Còn ${5 - attempts} lần thử")
            }

            // Success
            prefs.setLoginAttempts(0)
            prefs.setLockUntil(0)
            val now = DateUtils.now()
            userDao.updateLastLogin(userId, now)
            prefs.setCurrentUser(userId)

            val session = AuthSession(
                userId = userId,
                storeId = user.storeId,
                role = UserRole.fromString(user.role),
                displayName = user.displayName,
                loginAt = now,
            )
            Result.Success(session)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi đăng nhập")
        }
    }

    override suspend fun logout() {
        prefs.logout()
    }

    override suspend fun getCurrentSession(): AuthSession? {
        val userId = prefs.getCurrentUserIdSync() ?: return null
        val user = userDao.getById(userId) ?: return null
        return AuthSession(
            userId = user.id,
            storeId = user.storeId,
            role = UserRole.fromString(user.role),
            displayName = user.displayName,
            loginAt = user.lastLoginAt ?: 0,
        )
    }

    override fun isOnboarded(): Flow<Boolean> = prefs.isOnboarded
}
