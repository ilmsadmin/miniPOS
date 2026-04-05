package com.minipos.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object CreateStore : Screen("create_store")
    object JoinStore : Screen("join_store")
    object Login : Screen("login")
    object Home : Screen("home")

    // POS Flow
    object PosStep1 : Screen("pos/step1")
    object PosStep2 : Screen("pos/step2")
    object PosStep3 : Screen("pos/step3")
    object PosStep4 : Screen("pos/step4")
    object PosStep5 : Screen("pos/step5")

    // Management
    object ProductList : Screen("products")
    object ProductForm : Screen("products/form?id={id}") {
        fun createRoute(id: String? = null) = if (id != null) "products/form?id=$id" else "products/form"
    }
    object CategoryList : Screen("categories")
    object CategoryForm : Screen("categories/form?id={id}&parentId={parentId}") {
        fun createRoute(id: String? = null, parentId: String? = null): String {
            val params = mutableListOf<String>()
            if (id != null) params.add("id=$id")
            if (parentId != null) params.add("parentId=$parentId")
            return if (params.isEmpty()) "categories/form" else "categories/form?${params.joinToString("&")}"
        }
    }
    object SupplierList : Screen("suppliers")
    object SupplierForm : Screen("suppliers/form?id={id}") {
        fun createRoute(id: String? = null) = if (id != null) "suppliers/form?id=$id" else "suppliers/form"
    }
    object CustomerList : Screen("customers")
    object CustomerForm : Screen("customers/form?id={id}") {
        fun createRoute(id: String? = null) = if (id != null) "customers/form?id=$id" else "customers/form"
    }
    object CustomerDetail : Screen("customers/detail/{id}") {
        fun createRoute(id: String) = "customers/detail/$id"
    }

    // Inventory
    object InventoryHub : Screen("inventory_hub")
    object InventoryOverview : Screen("inventory")
    object PurchaseOrder : Screen("inventory/purchase")

    // Orders
    object OrderList : Screen("orders")
    object OrderDetail : Screen("orders/{id}") {
        fun createRoute(id: String) = "orders/$id"
    }

    // Reports
    object Reports : Screen("reports")

    // Settings
    object Settings : Screen("settings")
    object StoreSettings : Screen("settings/store")
    object UserManagement : Screen("settings/users")

    // Barcode
    object BarcodeManagement : Screen("barcode")

    // Store Management hub
    object StoreManagement : Screen("store_management")

    // Stock Management
    object StockManagement : Screen("stock_management")

    // Stock Audit (Kiểm kho / Điều chỉnh)
    object StockAudit : Screen("stock_audit")

    // Scan to POS
    object ScanToPos : Screen("scan_to_pos")
}
