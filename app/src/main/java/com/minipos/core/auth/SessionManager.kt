package com.minipos.core.auth

import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.model.AuthSession
import com.minipos.domain.model.CashierPermissions
import com.minipos.domain.model.Permission
import com.minipos.domain.model.Result
import com.minipos.domain.model.Store
import com.minipos.domain.model.UserRole
import com.minipos.domain.repository.AuthRepository
import com.minipos.domain.repository.StoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SessionManager — Singleton cung cấp session hiện tại và kiểm tra quyền
 * cho toàn bộ ViewModel / UseCase trong app.
 *
 * Inject vào bất kỳ ViewModel nào cần kiểm tra quyền:
 *   @Inject constructor(private val sessionManager: SessionManager)
 *
 * Cách dùng:
 *   // Kiểm tra nhanh
 *   if (!sessionManager.can(Permission.PRODUCT_CREATE)) return showError()
 *
 *   // Trong coroutine
 *   sessionManager.require(Permission.REPORT_VIEW).onError { return }
 */
@Singleton
class SessionManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val storeRepository: StoreRepository,
    private val prefs: AppPreferences,
) {

    // ── Lấy session hiện tại ────────────────────────────────

    suspend fun currentSession(): AuthSession? = authRepository.getCurrentSession()

    suspend fun currentRole(): UserRole? = currentSession()?.role

    // ── Lấy CashierPermissions từ store settings ───────────

    private suspend fun getCashierPerms(): CashierPermissions {
        return storeRepository.getStore()?.settings?.cashierPermissions ?: CashierPermissions()
    }

    // ── Kiểm tra quyền đồng bộ (trong coroutine) ───────────

    /**
     * Trả về true nếu session hiện tại có quyền [permission].
     */
    suspend fun can(permission: Permission): Boolean {
        val session = currentSession() ?: return false
        return PermissionChecker.hasPermission(session, permission, getCashierPerms())
    }

    /**
     * Trả về Result.Error(PERMISSION_DENIED) nếu không có quyền.
     * Dùng để gate-keep trong UseCase / Repository.
     */
    suspend fun require(permission: Permission): Result<Unit> {
        val session = currentSession()
            ?: return Result.Error(
                com.minipos.domain.model.ErrorCode.ACCOUNT_DISABLED,
                "Chưa đăng nhập",
            )
        return PermissionChecker.require(session, permission, getCashierPerms())
    }

    /**
     * Kiểm tra nhiều quyền cùng lúc — tất cả phải thỏa mãn.
     */
    suspend fun canAll(vararg permissions: Permission): Boolean =
        permissions.all { can(it) }

    /**
     * Kiểm tra có ít nhất 1 quyền trong danh sách.
     */
    suspend fun canAny(vararg permissions: Permission): Boolean =
        permissions.any { can(it) }

    /**
     * Trả về tập quyền hiệu lực của session hiện tại.
     * Dùng để build UI state (ẩn/hiện menu, nút bấm...).
     */
    suspend fun getEffectivePermissions(): Set<Permission> {
        val role = currentRole() ?: return emptySet()
        return PermissionChecker.getEffectivePermissions(role, getCashierPerms())
    }

    // ── Flow để observe quyền reactively ───────────────────

    /**
     * Flow emit lại khi store settings thay đổi (CashierPermissions cập nhật).
     * ViewModel collect flow này để cập nhật UI state.
     */
    fun observeEffectivePermissions(role: UserRole): Flow<Set<Permission>> =
        storeRepository.observeStore()
            .map { store ->
                val cashierPerms = store?.settings?.cashierPermissions ?: CashierPermissions()
                PermissionChecker.getEffectivePermissions(role, cashierPerms)
            }
            .distinctUntilChanged()

    // ── Helpers cho UI ──────────────────────────────────────

    /**
     * Validate giảm giá cashier.
     * @return thông báo lỗi nếu vi phạm, null nếu hợp lệ.
     */
    suspend fun validateCashierDiscount(discountPercent: Double): String? {
        if (currentRole() != UserRole.CASHIER) return null // Owner/Manager không giới hạn
        return PermissionChecker.validateCashierDiscount(discountPercent, getCashierPerms())
    }

    /**
     * Kiểm tra có thể quản lý user có [targetRole] không.
     */
    suspend fun canManageUser(targetRole: UserRole): Boolean {
        val actorRole = currentRole() ?: return false
        return PermissionChecker.canManageUser(actorRole, targetRole)
    }
}
