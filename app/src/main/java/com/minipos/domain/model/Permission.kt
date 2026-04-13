package com.minipos.domain.model

/**
 * Danh sách toàn bộ permissions trong app.
 * Format: MODULE_ACTION
 */
enum class Permission {
    // ── Store ─────────────────────────────────────
    STORE_VIEW,
    STORE_EDIT,
    STORE_SETTINGS,
    STORE_DELETE,

    // ── Users / Staff ─────────────────────────────
    USER_VIEW,
    USER_CREATE,
    USER_EDIT,
    USER_DEACTIVATE,
    USER_RESET_PIN,

    // ── Categories ────────────────────────────────
    CATEGORY_VIEW,
    CATEGORY_CREATE,
    CATEGORY_EDIT,
    CATEGORY_DELETE,

    // ── Products ──────────────────────────────────
    PRODUCT_VIEW,
    PRODUCT_CREATE,
    PRODUCT_EDIT,
    PRODUCT_DELETE,
    PRODUCT_IMPORT,

    // ── Suppliers ─────────────────────────────────
    SUPPLIER_VIEW,
    SUPPLIER_CREATE,
    SUPPLIER_EDIT,
    SUPPLIER_DELETE,

    // ── Customers ─────────────────────────────────
    CUSTOMER_VIEW,
    CUSTOMER_CREATE,
    CUSTOMER_EDIT,
    CUSTOMER_DELETE,

    // ── Inventory ─────────────────────────────────
    INVENTORY_VIEW,
    INVENTORY_PURCHASE_IN,
    INVENTORY_STOCK_CHECK,
    INVENTORY_ADJUST,
    INVENTORY_VIEW_COST_PRICE,

    // ── Orders / POS ──────────────────────────────
    ORDER_CREATE,
    ORDER_VIEW_OWN,
    ORDER_VIEW_ALL,
    ORDER_EDIT_PRICE,
    ORDER_APPLY_DISCOUNT,
    ORDER_REFUND,
    ORDER_CANCEL,

    // ── Reports ───────────────────────────────────
    REPORT_VIEW,
    REPORT_EXPORT,

    // ── Devices ───────────────────────────────────
    DEVICE_VIEW,
    DEVICE_APPROVE,
    DEVICE_REMOVE,

    // ── Data / Backup ─────────────────────────────
    DATA_BACKUP,
    DATA_RESTORE,
    DATA_DELETE_ALL,
    DATA_GOOGLE_CONNECT,
    DATA_BACKUP_SETTINGS,
}
