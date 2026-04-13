package com.minipos.core.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.minipos.domain.model.CashierPermissions
import com.minipos.domain.model.Permission
import com.minipos.domain.model.UserRole

/**
 * PermissionGuard — Composable helper để ẩn/hiện UI theo quyền.
 *
 * Ví dụ sử dụng:
 * ```
 * // Ẩn hoàn toàn nếu không có quyền
 * PermissionGuard(role = userRole, permission = Permission.PRODUCT_CREATE) {
 *     Button(onClick = { ... }) { Text("Thêm sản phẩm") }
 * }
 *
 * // Hiển thị UI khác nhau cho phép / không phép
 * PermissionGate(
 *     role = userRole,
 *     permission = Permission.REPORT_VIEW,
 *     denied = { Text("Bạn không có quyền xem báo cáo") }
 * ) {
 *     ReportScreen()
 * }
 * ```
 */

/**
 * Chỉ render [content] nếu [role] có quyền [permission].
 * Không render gì nếu không có quyền.
 */
@Composable
fun PermissionGuard(
    role: UserRole,
    permission: Permission,
    cashierPerms: CashierPermissions = CashierPermissions(),
    content: @Composable () -> Unit,
) {
    if (PermissionChecker.hasPermission(role, permission, cashierPerms)) {
        content()
    }
}

/**
 * Render [content] nếu có quyền, ngược lại render [denied].
 */
@Composable
fun PermissionGate(
    role: UserRole,
    permission: Permission,
    cashierPerms: CashierPermissions = CashierPermissions(),
    denied: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (PermissionChecker.hasPermission(role, permission, cashierPerms)) {
        content()
    } else {
        denied()
    }
}

/**
 * Kiểm tra nhiều quyền — tất cả phải thỏa mãn.
 */
@Composable
fun PermissionGuardAll(
    role: UserRole,
    permissions: List<Permission>,
    cashierPerms: CashierPermissions = CashierPermissions(),
    content: @Composable () -> Unit,
) {
    if (permissions.all { PermissionChecker.hasPermission(role, it, cashierPerms) }) {
        content()
    }
}

/**
 * Kiểm tra ít nhất 1 quyền thỏa mãn.
 */
@Composable
fun PermissionGuardAny(
    role: UserRole,
    permissions: List<Permission>,
    cashierPerms: CashierPermissions = CashierPermissions(),
    content: @Composable () -> Unit,
) {
    if (permissions.any { PermissionChecker.hasPermission(role, it, cashierPerms) }) {
        content()
    }
}

// ── Extension để kiểm tra quyền inline ─────────────────────

fun UserRole.can(
    permission: Permission,
    cashierPerms: CashierPermissions = CashierPermissions(),
): Boolean = PermissionChecker.hasPermission(this, permission, cashierPerms)

fun UserRole.canAll(
    vararg permissions: Permission,
    cashierPerms: CashierPermissions = CashierPermissions(),
): Boolean = permissions.all { PermissionChecker.hasPermission(this, it, cashierPerms) }

fun UserRole.canAny(
    vararg permissions: Permission,
    cashierPerms: CashierPermissions = CashierPermissions(),
): Boolean = permissions.any { PermissionChecker.hasPermission(this, it, cashierPerms) }
