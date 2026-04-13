package com.minipos.core.auth

import com.minipos.domain.model.AuthSession
import com.minipos.domain.model.CashierPermissions
import com.minipos.domain.model.ErrorCode
import com.minipos.domain.model.Permission
import com.minipos.domain.model.Result
import com.minipos.domain.model.UserRole

/**
 * PermissionChecker — Trung tâm xử lý phân quyền của miniPOS.
 *
 * Luồng kiểm tra:
 *   1. Lấy danh sách quyền mặc định theo Role (OWNER / MANAGER / CASHIER)
 *   2. Với Cashier: áp dụng override từ CashierPermissions (do Owner cấu hình)
 *   3. Trả về Boolean / Result cho từng use-case
 *
 * Nguyên tắc thiết kế:
 *   - OWNER  → full quyền, không giới hạn
 *   - MANAGER → quyền quản lý nghiệp vụ, KHÔNG có quyền cấu hình hệ thống
 *   - CASHIER → chỉ bán hàng; quyền mở rộng do Owner bật thêm
 */
object PermissionChecker {

    // =========================================================
    // Public API
    // =========================================================

    /**
     * Kiểm tra session hiện tại có quyền [permission] hay không.
     * @param cashierPerms Cài đặt tùy chỉnh cho Cashier (lấy từ StoreSettings).
     *                     Bỏ qua nếu role != CASHIER.
     */
    fun hasPermission(
        session: AuthSession,
        permission: Permission,
        cashierPerms: CashierPermissions = CashierPermissions(),
    ): Boolean = hasPermission(session.role, permission, cashierPerms)

    fun hasPermission(
        role: UserRole,
        permission: Permission,
        cashierPerms: CashierPermissions = CashierPermissions(),
    ): Boolean {
        return when (role) {
            UserRole.OWNER -> ownerPermissions.contains(permission)
            UserRole.MANAGER -> managerPermissions.contains(permission)
            UserRole.CASHIER -> cashierHasPermission(permission, cashierPerms)
        }
    }

    /**
     * Kiểm tra và trả về Result.Error(PERMISSION_DENIED) nếu không có quyền.
     * Dùng trong Repository/UseCase để gate-keep các thao tác nhạy cảm.
     */
    fun require(
        session: AuthSession,
        permission: Permission,
        cashierPerms: CashierPermissions = CashierPermissions(),
    ): Result<Unit> {
        return if (hasPermission(session, permission, cashierPerms)) {
            Result.Success(Unit)
        } else {
            Result.Error(
                ErrorCode.PERMISSION_DENIED,
                buildDeniedMessage(session.role, permission),
            )
        }
    }

    /**
     * Trả về tập quyền đầy đủ hiệu lực của một role + cashierPerms.
     * Dùng để render UI (ẩn/hiện nút, menu item...).
     */
    fun getEffectivePermissions(
        role: UserRole,
        cashierPerms: CashierPermissions = CashierPermissions(),
    ): Set<Permission> = when (role) {
        UserRole.OWNER -> ownerPermissions
        UserRole.MANAGER -> managerPermissions
        UserRole.CASHIER -> buildCashierPermissions(cashierPerms)
    }

    /**
     * Kiểm tra Manager có quyền thao tác lên [targetRole] hay không.
     * Manager chỉ được quản lý Cashier; không được sửa Owner hay Manager khác.
     */
    fun canManageUser(actorRole: UserRole, targetRole: UserRole): Boolean {
        return when (actorRole) {
            UserRole.OWNER -> targetRole != UserRole.OWNER // Owner không tự xóa bản thân qua đây
            UserRole.MANAGER -> targetRole == UserRole.CASHIER
            UserRole.CASHIER -> false
        }
    }

    /**
     * Kiểm tra giảm giá Cashier có hợp lệ không.
     * @return null nếu hợp lệ; thông báo lỗi nếu vi phạm.
     */
    fun validateCashierDiscount(
        discountPercent: Double,
        cashierPerms: CashierPermissions,
    ): String? {
        if (!cashierPerms.canApplyDiscount) return "Thu ngân không có quyền áp dụng giảm giá"
        if (cashierPerms.maxDiscountPercent >= 0 && discountPercent > cashierPerms.maxDiscountPercent) {
            return "Giảm giá tối đa cho thu ngân là ${cashierPerms.maxDiscountPercent}%"
        }
        return null
    }

    // =========================================================
    // Role Permission Sets
    // =========================================================

    /** OWNER — toàn quyền */
    val ownerPermissions: Set<Permission> = Permission.entries.toSet()

    /** MANAGER — quản lý nghiệp vụ, không có quyền hệ thống */
    val managerPermissions: Set<Permission> = setOf(
        // Store
        Permission.STORE_VIEW,
        // Users — chỉ Cashier
        Permission.USER_VIEW,
        Permission.USER_CREATE,
        Permission.USER_EDIT,
        Permission.USER_DEACTIVATE,
        Permission.USER_RESET_PIN,
        // Categories
        Permission.CATEGORY_VIEW,
        Permission.CATEGORY_CREATE,
        Permission.CATEGORY_EDIT,
        Permission.CATEGORY_DELETE,
        // Products
        Permission.PRODUCT_VIEW,
        Permission.PRODUCT_CREATE,
        Permission.PRODUCT_EDIT,
        Permission.PRODUCT_DELETE,
        Permission.PRODUCT_IMPORT,
        // Suppliers
        Permission.SUPPLIER_VIEW,
        Permission.SUPPLIER_CREATE,
        Permission.SUPPLIER_EDIT,
        Permission.SUPPLIER_DELETE,
        // Customers
        Permission.CUSTOMER_VIEW,
        Permission.CUSTOMER_CREATE,
        Permission.CUSTOMER_EDIT,
        Permission.CUSTOMER_DELETE,
        // Inventory
        Permission.INVENTORY_VIEW,
        Permission.INVENTORY_PURCHASE_IN,
        Permission.INVENTORY_STOCK_CHECK,
        Permission.INVENTORY_ADJUST,
        Permission.INVENTORY_VIEW_COST_PRICE,
        // Orders / POS
        Permission.ORDER_CREATE,
        Permission.ORDER_VIEW_OWN,
        Permission.ORDER_VIEW_ALL,
        Permission.ORDER_EDIT_PRICE,
        Permission.ORDER_APPLY_DISCOUNT,
        Permission.ORDER_REFUND,
        Permission.ORDER_CANCEL,
        // Reports
        Permission.REPORT_VIEW,
        Permission.REPORT_EXPORT,
        // Devices
        Permission.DEVICE_VIEW,
    )

    /** CASHIER — base set, có thể mở rộng bởi CashierPermissions */
    private val cashierBasePermissions: Set<Permission> = setOf(
        Permission.STORE_VIEW,
        Permission.CATEGORY_VIEW,
        Permission.PRODUCT_VIEW,
        Permission.CUSTOMER_VIEW,
        Permission.CUSTOMER_CREATE,
        Permission.ORDER_CREATE,
        Permission.ORDER_VIEW_OWN,
    )

    // =========================================================
    // Internal helpers
    // =========================================================

    private fun cashierHasPermission(
        permission: Permission,
        perms: CashierPermissions,
    ): Boolean {
        if (cashierBasePermissions.contains(permission)) return true
        return when (permission) {
            Permission.ORDER_APPLY_DISCOUNT -> perms.canApplyDiscount
            Permission.ORDER_EDIT_PRICE -> perms.canEditPrice
            Permission.ORDER_CANCEL -> perms.canCancelOrder
            Permission.ORDER_VIEW_ALL -> perms.canViewAllOrders
            Permission.INVENTORY_VIEW, Permission.INVENTORY_STOCK_CHECK -> perms.canViewStock
            Permission.INVENTORY_VIEW_COST_PRICE -> perms.canViewCostPrice
            else -> false
        }
    }

    private fun buildCashierPermissions(perms: CashierPermissions): Set<Permission> =
        Permission.entries.filter { cashierHasPermission(it, perms) }.toSet()

    private fun buildDeniedMessage(role: UserRole, permission: Permission): String {
        val roleName = when (role) {
            UserRole.OWNER -> "Chủ cửa hàng"
            UserRole.MANAGER -> "Quản lý"
            UserRole.CASHIER -> "Thu ngân"
        }
        return "Tài khoản $roleName không có quyền thực hiện thao tác này (${permission.name})"
    }
}
