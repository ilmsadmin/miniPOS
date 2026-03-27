package com.minipos.data.database.dao

import androidx.room.*
import com.minipos.data.database.entity.*
import kotlinx.coroutines.flow.Flow

// Query result data classes for JOIN queries
data class StockWithProduct(
    val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "variant_id") val variantId: String?,
    val quantity: Double,
    @ColumnInfo(name = "reserved_qty") val reservedQty: Double,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "product_sku") val productSku: String,
    @ColumnInfo(name = "product_unit") val productUnit: String,
    @ColumnInfo(name = "product_min_stock") val productMinStock: Int,
    @ColumnInfo(name = "product_cost_price") val productCostPrice: Double,
    @ColumnInfo(name = "product_selling_price") val productSellingPrice: Double,
)

data class StockMovementWithProduct(
    val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "variant_id") val variantId: String?,
    @ColumnInfo(name = "supplier_id") val supplierId: String?,
    val type: String,
    val quantity: Double,
    @ColumnInfo(name = "quantity_before") val quantityBefore: Double,
    @ColumnInfo(name = "quantity_after") val quantityAfter: Double,
    @ColumnInfo(name = "unit_cost") val unitCost: Double?,
    @ColumnInfo(name = "reference_id") val referenceId: String?,
    @ColumnInfo(name = "reference_type") val referenceType: String?,
    val notes: String?,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "product_sku") val productSku: String,
    @ColumnInfo(name = "supplier_name") val supplierName: String?,
)

@Dao
interface StoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(store: StoreEntity)

    @Update
    suspend fun update(store: StoreEntity)

    @Query("SELECT * FROM stores WHERE is_deleted = 0 LIMIT 1")
    suspend fun getStore(): StoreEntity?

    @Query("SELECT * FROM stores WHERE is_deleted = 0 LIMIT 1")
    fun observeStore(): Flow<StoreEntity?>

    @Query("SELECT * FROM stores WHERE code = :code AND is_deleted = 0 LIMIT 1")
    suspend fun getStoreByCode(code: String): StoreEntity?
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE store_id = :storeId AND is_deleted = 0 ORDER BY role, display_name")
    fun observeUsers(storeId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE store_id = :storeId AND is_deleted = 0 AND is_active = 1 ORDER BY display_name")
    suspend fun getActiveUsers(storeId: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE store_id = :storeId AND is_deleted = 0 ORDER BY role, display_name")
    suspend fun getAllUsers(storeId: String): List<UserEntity>

    @Query("UPDATE users SET last_login_at = :timestamp, updated_at = :timestamp WHERE id = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long)

    @Query("UPDATE users SET is_deleted = 1, deleted_at = :timestamp, updated_at = :timestamp WHERE id = :userId")
    suspend fun softDelete(userId: String, timestamp: Long)
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE store_id = :storeId AND is_deleted = 0 AND is_active = 1 ORDER BY sort_order, name")
    fun observeCategories(storeId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE store_id = :storeId AND is_deleted = 0 AND is_active = 1 ORDER BY sort_order, name")
    suspend fun getAll(storeId: String): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM products WHERE category_id = :categoryId AND is_deleted = 0 AND is_active = 1")
    suspend fun getProductCount(categoryId: String): Int

    @Query("UPDATE categories SET is_deleted = 1, deleted_at = :timestamp, updated_at = :timestamp WHERE id = :categoryId")
    suspend fun softDelete(categoryId: String, timestamp: Long)
}

@Dao
interface SupplierDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplier: SupplierEntity)

    @Update
    suspend fun update(supplier: SupplierEntity)

    @Query("SELECT * FROM suppliers WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE store_id = :storeId AND is_deleted = 0 AND is_active = 1 ORDER BY name")
    fun observeSuppliers(storeId: String): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE store_id = :storeId AND is_deleted = 0 ORDER BY name")
    suspend fun getAll(storeId: String): List<SupplierEntity>

    @Query("UPDATE suppliers SET is_deleted = 1, deleted_at = :timestamp, updated_at = :timestamp WHERE id = :supplierId")
    suspend fun softDelete(supplierId: String, timestamp: Long)
}

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    @Update
    suspend fun update(product: ProductEntity)

    @Query("SELECT * FROM products WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE store_id = :storeId AND is_deleted = 0 AND is_active = 1 ORDER BY name")
    fun observeProducts(storeId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE store_id = :storeId AND is_deleted = 0 AND is_active = 1 ORDER BY name")
    suspend fun getAll(storeId: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE store_id = :storeId AND category_id = :categoryId AND is_deleted = 0 AND is_active = 1 ORDER BY name")
    suspend fun getByCategory(storeId: String, categoryId: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE store_id = :storeId AND is_deleted = 0 AND is_active = 1 AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') ORDER BY name")
    suspend fun search(storeId: String, query: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE barcode = :barcode AND store_id = :storeId AND is_deleted = 0 LIMIT 1")
    suspend fun getByBarcode(storeId: String, barcode: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM products WHERE store_id = :storeId AND is_deleted = 0")
    suspend fun getProductCount(storeId: String): Int

    @Query("SELECT MAX(CAST(REPLACE(sku, 'SP', '') AS INTEGER)) FROM products WHERE store_id = :storeId AND sku LIKE 'SP%'")
    suspend fun getMaxSkuNumber(storeId: String): Int?

    @Query("UPDATE products SET is_deleted = 1, deleted_at = :timestamp, updated_at = :timestamp WHERE id = :productId")
    suspend fun softDelete(productId: String, timestamp: Long)
}

@Dao
interface ProductVariantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(variant: ProductVariantEntity)

    @Update
    suspend fun update(variant: ProductVariantEntity)

    @Query("SELECT * FROM product_variants WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): ProductVariantEntity?

    @Query("SELECT * FROM product_variants WHERE product_id = :productId AND is_deleted = 0 AND is_active = 1 ORDER BY variant_name")
    fun observeByProductId(productId: String): Flow<List<ProductVariantEntity>>

    @Query("SELECT * FROM product_variants WHERE product_id = :productId AND is_deleted = 0 AND is_active = 1 ORDER BY variant_name")
    suspend fun getByProductId(productId: String): List<ProductVariantEntity>

    @Query("SELECT * FROM product_variants WHERE barcode = :barcode AND store_id = :storeId AND is_deleted = 0 LIMIT 1")
    suspend fun getByBarcode(storeId: String, barcode: String): ProductVariantEntity?

    @Query("UPDATE product_variants SET is_deleted = 1, deleted_at = :timestamp, updated_at = :timestamp WHERE id = :variantId")
    suspend fun softDelete(variantId: String, timestamp: Long)

    @Query("UPDATE product_variants SET is_deleted = 1, deleted_at = :timestamp, updated_at = :timestamp WHERE product_id = :productId")
    suspend fun softDeleteByProductId(productId: String, timestamp: Long)
}

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE store_id = :storeId AND is_deleted = 0 ORDER BY name")
    fun observeCustomers(storeId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE store_id = :storeId AND is_deleted = 0 ORDER BY last_visit_at DESC LIMIT :limit")
    suspend fun getRecent(storeId: String, limit: Int = 10): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE store_id = :storeId AND is_deleted = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name")
    suspend fun search(storeId: String, query: String): List<CustomerEntity>

    @Query("UPDATE customers SET total_spent = total_spent + :amount, visit_count = visit_count + 1, last_visit_at = :timestamp, updated_at = :timestamp WHERE id = :customerId")
    suspend fun incrementVisit(customerId: String, amount: Double, timestamp: Long)

    @Query("UPDATE customers SET is_deleted = 1, deleted_at = :timestamp, updated_at = :timestamp WHERE id = :customerId")
    suspend fun softDelete(customerId: String, timestamp: Long)
}

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inventory: InventoryEntity)

    @Query("SELECT * FROM inventory WHERE product_id = :productId AND store_id = :storeId AND variant_id IS NULL LIMIT 1")
    suspend fun getByProduct(storeId: String, productId: String): InventoryEntity?

    @Query("SELECT * FROM inventory WHERE product_id = :productId AND store_id = :storeId AND variant_id = :variantId LIMIT 1")
    suspend fun getByVariant(storeId: String, productId: String, variantId: String): InventoryEntity?

    @Query("UPDATE inventory SET quantity = quantity + :amount, updated_at = :timestamp WHERE id = :inventoryId")
    suspend fun adjustQuantity(inventoryId: String, amount: Double, timestamp: Long)

    @Query("UPDATE inventory SET quantity = :newQuantity, updated_at = :timestamp WHERE id = :inventoryId")
    suspend fun setQuantity(inventoryId: String, newQuantity: Double, timestamp: Long)

    @Query("""
        SELECT COUNT(*) 
        FROM inventory i
        INNER JOIN products p ON p.id = i.product_id
        WHERE i.store_id = :storeId AND p.is_deleted = 0 AND p.is_active = 1
        AND p.track_inventory = 1 AND i.quantity <= p.min_stock
    """)
    suspend fun getLowStockCount(storeId: String): Int

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT i.*, p.name AS product_name, p.sku AS product_sku, p.unit AS product_unit, 
               p.min_stock AS product_min_stock, p.cost_price AS product_cost_price,
               p.selling_price AS product_selling_price
        FROM inventory i
        INNER JOIN products p ON p.id = i.product_id
        WHERE i.store_id = :storeId AND p.is_deleted = 0 AND p.is_active = 1 AND p.track_inventory = 1
        ORDER BY i.quantity ASC
    """)
    suspend fun getAllStockWithProduct(storeId: String): List<StockWithProduct>

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT sm.*, p.name AS product_name, p.sku AS product_sku, s.name AS supplier_name
        FROM stock_movements sm
        INNER JOIN products p ON p.id = sm.product_id
        LEFT JOIN suppliers s ON s.id = sm.supplier_id
        WHERE sm.store_id = :storeId AND sm.created_at BETWEEN :startTime AND :endTime
        ORDER BY sm.created_at DESC
    """)
    suspend fun getStockMovements(storeId: String, startTime: Long, endTime: Long): List<StockMovementWithProduct>

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT sm.*, p.name AS product_name, p.sku AS product_sku, s.name AS supplier_name
        FROM stock_movements sm
        INNER JOIN products p ON p.id = sm.product_id
        LEFT JOIN suppliers s ON s.id = sm.supplier_id
        WHERE sm.store_id = :storeId AND sm.product_id = :productId
        ORDER BY sm.created_at DESC
        LIMIT :limit
    """)
    suspend fun getStockMovementsByProduct(storeId: String, productId: String, limit: Int = 50): List<StockMovementWithProduct>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN sm.type IN ('purchase_in', 'return_in', 'adjustment_in') THEN sm.quantity ELSE 0 END), 0)
        FROM stock_movements sm
        WHERE sm.store_id = :storeId AND sm.created_at BETWEEN :startTime AND :endTime
    """)
    suspend fun getTotalStockIn(storeId: String, startTime: Long, endTime: Long): Double

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN sm.type IN ('sale_out', 'return_out', 'adjustment_out', 'damage_out') THEN ABS(sm.quantity) ELSE 0 END), 0)
        FROM stock_movements sm
        WHERE sm.store_id = :storeId AND sm.created_at BETWEEN :startTime AND :endTime
    """)
    suspend fun getTotalStockOut(storeId: String, startTime: Long, endTime: Long): Double
}

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderPayments(payments: List<OrderPaymentEntity>)

    @Query("SELECT * FROM orders WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE store_id = :storeId AND is_deleted = 0 ORDER BY created_at DESC")
    fun observeOrders(storeId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE store_id = :storeId AND is_deleted = 0 AND created_at BETWEEN :startTime AND :endTime ORDER BY created_at DESC")
    suspend fun getOrdersByDateRange(storeId: String, startTime: Long, endTime: Long): List<OrderEntity>

    @Query("SELECT * FROM order_items WHERE order_id = :orderId")
    suspend fun getOrderItems(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM order_payments WHERE order_id = :orderId")
    suspend fun getOrderPayments(orderId: String): List<OrderPaymentEntity>

    @Query("SELECT COUNT(*) FROM orders WHERE store_id = :storeId AND is_deleted = 0 AND created_at BETWEEN :startTime AND :endTime")
    suspend fun getOrderCount(storeId: String, startTime: Long, endTime: Long): Int

    @Query("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE store_id = :storeId AND is_deleted = 0 AND status = 'completed' AND created_at BETWEEN :startTime AND :endTime")
    suspend fun getTotalRevenue(storeId: String, startTime: Long, endTime: Long): Double

    @Query("SELECT COALESCE(MAX(CAST(REPLACE(order_code, :prefix, '') AS INTEGER)), 0) FROM orders WHERE store_id = :storeId AND order_code LIKE :prefix || '%'")
    suspend fun getMaxOrderSequence(storeId: String, prefix: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity)
}
