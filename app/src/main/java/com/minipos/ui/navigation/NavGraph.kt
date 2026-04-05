package com.minipos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.minipos.ui.home.HomeScreen
import com.minipos.ui.pos.*
import com.minipos.ui.product.ProductListScreen
import com.minipos.ui.product.ProductFormScreen
import com.minipos.ui.category.CategoryListScreen
import com.minipos.ui.category.CategoryFormScreen
import com.minipos.ui.supplier.SupplierListScreen
import com.minipos.ui.supplier.SupplierFormScreen
import com.minipos.ui.customer.CustomerListScreen
import com.minipos.ui.customer.CustomerFormScreen
import com.minipos.ui.customer.CustomerDetailScreen
import com.minipos.ui.order.OrderListScreen
import com.minipos.ui.order.OrderDetailScreen
import com.minipos.ui.inventory.InventoryScreen
import com.minipos.ui.inventory.InventoryHubScreen
import com.minipos.ui.purchase.PurchaseScreen
import com.minipos.ui.report.ReportScreen
import com.minipos.ui.settings.SettingsScreen
import com.minipos.ui.barcode.BarcodeScreen
import com.minipos.ui.scan.ScanToPosScreen
import com.minipos.ui.home.StoreManagementScreen
import com.minipos.ui.stockmanagement.StockManagementScreen
import com.minipos.ui.stockaudit.StockAuditScreen

@Composable
fun MiniPosNavGraph(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) },
            )
        }

        // POS Flow — Home IS the POS. Payment + Complete screens remain.
        composable(Screen.PosStep4.route) {
            PosStep4Screen(
                onPaymentSuccess = { navController.navigate(Screen.PosStep5.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.PosStep5.route) {
            PosStep5Screen(
                onNewOrder = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onGoHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
            )
        }

        // Products
        composable(Screen.ProductList.route) {
            ProductListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToForm = { productId ->
                    navController.navigate(Screen.ProductForm.createRoute(productId))
                },
            )
        }

        // Product Form (Create / Edit)
        composable(
            route = Screen.ProductForm.route,
            arguments = listOf(
                androidx.navigation.navArgument("id") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("id")
            ProductFormScreen(
                productId = productId,
                onBack = { navController.popBackStack() },
            )
        }

        // Categories
        composable(Screen.CategoryList.route) {
            CategoryListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToForm = { categoryId, parentId ->
                    navController.navigate(Screen.CategoryForm.createRoute(categoryId, parentId))
                },
            )
        }

        // Category Form (Create / Edit)
        composable(
            route = Screen.CategoryForm.route,
            arguments = listOf(
                androidx.navigation.navArgument("id") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("parentId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("id")
            val parentId = backStackEntry.arguments?.getString("parentId")
            CategoryFormScreen(
                categoryId = categoryId,
                parentId = parentId,
                onBack = { navController.popBackStack() },
            )
        }

        // Suppliers
        composable(Screen.SupplierList.route) {
            SupplierListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToForm = { supplierId ->
                    navController.navigate(Screen.SupplierForm.createRoute(supplierId))
                },
                onNavigateToPurchase = {
                    navController.navigate(Screen.PurchaseOrder.route)
                },
            )
        }

        // Supplier Form (Create / Edit)
        composable(
            route = Screen.SupplierForm.route,
            arguments = listOf(
                androidx.navigation.navArgument("id") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val supplierId = backStackEntry.arguments?.getString("id")
            SupplierFormScreen(
                supplierId = supplierId,
                onBack = { navController.popBackStack() },
            )
        }

        // Customers
        composable(Screen.CustomerList.route) {
            CustomerListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToForm = { customerId ->
                    navController.navigate(Screen.CustomerForm.createRoute(customerId))
                },
                onNavigateToDetail = { customerId ->
                    navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                },
            )
        }

        // Customer Form (Create / Edit)
        composable(
            route = Screen.CustomerForm.route,
            arguments = listOf(
                androidx.navigation.navArgument("id") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("id")
            CustomerFormScreen(
                customerId = customerId,
                onBack = { navController.popBackStack() },
            )
        }

        // Customer Detail
        composable(
            route = Screen.CustomerDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("id") {
                    type = androidx.navigation.NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("id") ?: ""
            CustomerDetailScreen(
                customerId = customerId,
                onBack = { navController.popBackStack() },
                onEdit = { id ->
                    navController.navigate(Screen.CustomerForm.createRoute(id))
                },
            )
        }

        // Orders
        composable(Screen.OrderList.route) {
            OrderListScreen(
                onBack = { navController.popBackStack() },
                onOrderClick = { orderId ->
                    navController.navigate(Screen.OrderDetail.createRoute(orderId))
                },
            )
        }
        composable(Screen.OrderDetail.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("id") ?: ""
            OrderDetailScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() },
            )
        }

        // Inventory Hub
        composable(Screen.InventoryHub.route) {
            InventoryHubScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) },
            )
        }

        // Inventory Detail
        composable(Screen.InventoryOverview.route) {
            InventoryScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.PurchaseOrder.route) {
            PurchaseScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // Reports
        composable(Screen.Reports.route) {
            ReportScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToStoreSettings = { navController.navigate(Screen.StoreSettings.route) },
            )
        }

        // Store Settings
        composable(Screen.StoreSettings.route) {
            com.minipos.ui.settings.StoreSettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // Barcode management
        composable(Screen.BarcodeManagement.route) {
            BarcodeScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // Store Management hub
        composable(Screen.StoreManagement.route) {
            StoreManagementScreen(
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() },
            )
        }

        // Stock Management
        composable(Screen.StockManagement.route) {
            StockManagementScreen(
                onBack = { navController.popBackStack() },
                onNavigateToStockAudit = {
                    navController.navigate(Screen.StockAudit.route)
                },
            )
        }

        // Stock Audit (Kiểm kho / Điều chỉnh)
        composable(Screen.StockAudit.route) {
            StockAuditScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // Scan to POS
        composable(Screen.ScanToPos.route) {
            ScanToPosScreen(
                onBack = { navController.popBackStack() },
                onGoToPos = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
            )
        }
    }
}
