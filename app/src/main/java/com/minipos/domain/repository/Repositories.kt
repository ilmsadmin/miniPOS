package com.minipos.domain.repository

import com.minipos.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun createStore(
        storeName: String, storeCode: String, address: String?, phone: String?,
        ownerName: String, ownerPin: String, ownerPassword: String
    ): Result<Store>
    suspend fun ensureDefaultStore()
    suspend fun login(userId: String, pin: String): Result<AuthSession>
    suspend fun autoLogin()
    suspend fun logout()
    suspend fun getCurrentSession(): AuthSession?
    fun isOnboarded(): Flow<Boolean>
}

interface StoreRepository {
    suspend fun getStore(): Store?
    fun observeStore(): Flow<Store?>
    suspend fun updateStore(store: Store): Result<Store>
}

interface UserRepository {
    suspend fun createUser(storeId: String, displayName: String, pin: String, role: UserRole): Result<User>
    suspend fun updateUser(user: User): Result<User>
    suspend fun deleteUser(userId: String): Result<Unit>
    suspend fun getUserById(userId: String): User?
    fun observeUsers(storeId: String): Flow<List<User>>
    suspend fun getActiveUsers(storeId: String): List<User>
    suspend fun resetPin(userId: String, newPin: String): Result<Unit>
    suspend fun clearPin(userId: String): Result<Unit>
    suspend fun hasPin(userId: String): Boolean
    suspend fun verifyPin(userId: String, pin: String): Boolean
}

interface CategoryRepository {
    suspend fun create(storeId: String, name: String, description: String?, parentId: String?, icon: String?, color: String?): Result<Category>
    suspend fun update(category: Category): Result<Category>
    suspend fun delete(categoryId: String): Result<Unit>
    fun observeCategories(storeId: String): Flow<List<Category>>
    suspend fun getAll(storeId: String): List<Category>
}

interface ProductRepository {
    suspend fun create(product: Product): Result<Product>
    suspend fun update(product: Product): Result<Product>
    suspend fun delete(productId: String): Result<Unit>
    suspend fun getById(productId: String): Product?
    fun observeProducts(storeId: String): Flow<List<Product>>
    suspend fun getAll(storeId: String): List<Product>
    suspend fun getByCategory(storeId: String, categoryId: String): List<Product>
    suspend fun search(storeId: String, query: String): List<Product>
    suspend fun getByBarcode(storeId: String, barcode: String): Product?
    suspend fun generateSku(storeId: String): String
    // Variants
    suspend fun createVariant(variant: ProductVariant): Result<ProductVariant>
    suspend fun updateVariant(variant: ProductVariant): Result<ProductVariant>
    suspend fun deleteVariant(variantId: String): Result<Unit>
    fun observeVariants(productId: String): Flow<List<ProductVariant>>
    suspend fun getVariants(productId: String): List<ProductVariant>
    suspend fun getVariantByBarcode(storeId: String, barcode: String): ProductVariant?
}

interface SupplierRepository {
    suspend fun create(supplier: Supplier): Result<Supplier>
    suspend fun update(supplier: Supplier): Result<Supplier>
    suspend fun delete(supplierId: String): Result<Unit>
    fun observeSuppliers(storeId: String): Flow<List<Supplier>>
    suspend fun getAll(storeId: String): List<Supplier>
}

interface CustomerRepository {
    suspend fun create(customer: Customer): Result<Customer>
    suspend fun update(customer: Customer): Result<Customer>
    suspend fun delete(customerId: String): Result<Unit>
    fun observeCustomers(storeId: String): Flow<List<Customer>>
    suspend fun search(storeId: String, query: String): List<Customer>
    suspend fun getRecent(storeId: String, limit: Int = 10): List<Customer>
}

interface InventoryRepository {
    suspend fun getStock(storeId: String, productId: String): InventoryItem?
    suspend fun adjustStock(storeId: String, productId: String, amount: Double, type: StockMovementType, userId: String, referenceId: String?, supplierId: String? = null): Result<Unit>
    suspend fun getLowStockCount(storeId: String): Int
    suspend fun getAllStockOverview(storeId: String): List<StockOverviewItem>
    suspend fun getStockHistory(storeId: String, startTime: Long, endTime: Long): List<StockHistoryItem>
    suspend fun getStockHistoryByProduct(storeId: String, productId: String): List<StockHistoryItem>
    suspend fun getStockSummary(storeId: String, startTime: Long, endTime: Long): StockSummary
}

interface OrderRepository {
    suspend fun createOrder(cart: Cart, userId: String, storeId: String, payments: List<OrderPayment>): Result<Order>
    fun observeOrders(storeId: String): Flow<List<Order>>
    suspend fun getOrderDetail(orderId: String): OrderDetail?
    suspend fun getOrdersByDateRange(storeId: String, startTime: Long, endTime: Long): List<Order>
    suspend fun getTodayRevenue(storeId: String): Double
    suspend fun getTodayOrderCount(storeId: String): Int
    suspend fun getDashboardData(storeId: String): DashboardData
}
