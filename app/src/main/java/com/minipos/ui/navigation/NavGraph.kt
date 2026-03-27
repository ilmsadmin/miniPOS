package com.minipos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.minipos.ui.home.HomeScreen
import com.minipos.ui.pos.*
import com.minipos.ui.product.ProductListScreen
import com.minipos.ui.category.CategoryListScreen
import com.minipos.ui.supplier.SupplierListScreen
import com.minipos.ui.customer.CustomerListScreen
import com.minipos.ui.order.OrderListScreen
import com.minipos.ui.order.OrderDetailScreen
import com.minipos.ui.inventory.InventoryScreen
import com.minipos.ui.purchase.PurchaseScreen
import com.minipos.ui.report.ReportScreen
import com.minipos.ui.settings.SettingsScreen
import com.minipos.ui.barcode.BarcodeScreen
import com.minipos.ui.scan.ScanToPosScreen
import com.minipos.ui.home.StoreManagementScreen

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

        // POS Flow
        composable(Screen.PosStep1.route) {
            PosStep1Screen(
                onNext = { navController.navigate(Screen.PosStep2.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.PosStep2.route) {
            PosStep2Screen(
                onNext = { navController.navigate(Screen.PosStep3.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.PosStep3.route) {
            PosStep3Screen(
                onNext = { navController.navigate(Screen.PosStep4.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.PosStep4.route) {
            PosStep4Screen(
                onPaymentSuccess = { navController.navigate(Screen.PosStep5.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.PosStep5.route) {
            PosStep5Screen(
                onNewOrder = {
                    navController.navigate(Screen.PosStep1.route) {
                        popUpTo(Screen.Home.route)
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
            )
        }

        // Categories
        composable(Screen.CategoryList.route) {
            CategoryListScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // Suppliers
        composable(Screen.SupplierList.route) {
            SupplierListScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // Customers
        composable(Screen.CustomerList.route) {
            CustomerListScreen(
                onBack = { navController.popBackStack() },
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

        // Inventory
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

        // Scan to POS
        composable(Screen.ScanToPos.route) {
            ScanToPosScreen(
                onBack = { navController.popBackStack() },
                onGoToPos = {
                    navController.navigate(Screen.PosStep1.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
            )
        }
    }
}
