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
    object SupplierList : Screen("suppliers")
    object SupplierForm : Screen("suppliers/form?id={id}") {
        fun createRoute(id: String? = null) = if (id != null) "suppliers/form?id=$id" else "suppliers/form"
    }
    object CustomerList : Screen("customers")
    object CustomerForm : Screen("customers/form?id={id}") {
        fun createRoute(id: String? = null) = if (id != null) "customers/form?id=$id" else "customers/form"
    }

    // Inventory
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
    object UserManagement : Screen("settings/users")
}
