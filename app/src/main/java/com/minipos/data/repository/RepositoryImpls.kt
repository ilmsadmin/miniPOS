package com.minipos.data.repository

import com.minipos.core.constants.AppConstants
import com.minipos.core.utils.DateUtils
import com.minipos.core.utils.HashUtils
import com.minipos.core.utils.UuidGenerator
import com.minipos.data.database.dao.*
import com.minipos.data.database.entity.*
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.model.*
import com.minipos.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ============ Store Repository ============

@Singleton
class StoreRepositoryImpl @Inject constructor(
    private val storeDao: StoreDao
) : StoreRepository {
    override suspend fun getStore(): Store? = storeDao.getStore()?.toDomain()

    override fun observeStore(): Flow<Store?> = storeDao.observeStore().map { it?.toDomain() }

    override suspend fun updateStore(store: Store): Result<Store> {
        return try {
            val entity = storeDao.getStore() ?: return Result.Error(ErrorCode.INVALID_INPUT, "Cửa hàng không tồn tại")
            val settingsJson = try {
                com.google.gson.Gson().toJson(store.settings)
            } catch (_: Exception) { entity.settings }
            val updated = entity.copy(
                name = store.name,
                address = store.address,
                phone = store.phone,
                settings = settingsJson,
                updatedAt = DateUtils.now(),
            )
            storeDao.update(updated)
            Result.Success(updated.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi cập nhật cửa hàng")
        }
    }
}

// ============ User Repository ============

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val prefs: AppPreferences,
) : UserRepository {

    override suspend fun createUser(storeId: String, displayName: String, pin: String, role: UserRole): Result<User> {
        return try {
            val now = DateUtils.now()
            val entity = UserEntity(
                id = UuidGenerator.generate(),
                storeId = storeId,
                displayName = displayName,
                role = role.name.lowercase(),
                pinHash = HashUtils.hashPin(pin),
                isActive = true,
                createdAt = now,
                updatedAt = now,
                deviceId = prefs.getDeviceIdSync(),
            )
            userDao.insert(entity)
            Result.Success(entity.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi tạo người dùng")
        }
    }

    override suspend fun updateUser(user: User): Result<User> {
        return try {
            val entity = userDao.getById(user.id) ?: return Result.Error(ErrorCode.INVALID_INPUT, "Không tìm thấy người dùng")
            val updated = entity.copy(
                displayName = user.displayName,
                role = user.role.name.lowercase(),
                isActive = user.isActive,
                updatedAt = DateUtils.now(),
            )
            userDao.update(updated)
            Result.Success(updated.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi cập nhật người dùng")
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            userDao.softDelete(userId, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi xoá người dùng")
        }
    }

    override suspend fun getUserById(userId: String): User? = userDao.getById(userId)?.toDomain()

    override fun observeUsers(storeId: String): Flow<List<User>> =
        userDao.observeUsers(storeId).map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveUsers(storeId: String): List<User> =
        userDao.getActiveUsers(storeId).map { it.toDomain() }

    override suspend fun resetPin(userId: String, newPin: String): Result<Unit> {
        return try {
            val entity = userDao.getById(userId) ?: return Result.Error(ErrorCode.INVALID_INPUT, "Không tìm thấy người dùng")
            val updated = entity.copy(
                pinHash = HashUtils.hashPin(newPin),
                updatedAt = DateUtils.now(),
            )
            userDao.update(updated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi reset PIN")
        }
    }
}

// ============ Category Repository ============

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val prefs: AppPreferences,
) : CategoryRepository {

    override suspend fun create(storeId: String, name: String, description: String?, parentId: String?, icon: String?, color: String?): Result<Category> {
        return try {
            val now = DateUtils.now()
            val entity = CategoryEntity(
                id = UuidGenerator.generate(),
                storeId = storeId,
                parentId = parentId,
                name = name,
                description = description,
                icon = icon,
                color = color,
                createdAt = now,
                updatedAt = now,
                deviceId = prefs.getDeviceIdSync(),
            )
            categoryDao.insert(entity)
            Result.Success(entity.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi tạo danh mục")
        }
    }

    override suspend fun update(category: Category): Result<Category> {
        return try {
            val entity = categoryDao.getById(category.id) ?: return Result.Error(ErrorCode.INVALID_INPUT, "Không tìm thấy danh mục")
            val updated = entity.copy(
                name = category.name,
                description = category.description,
                icon = category.icon,
                color = category.color,
                sortOrder = category.sortOrder,
                updatedAt = DateUtils.now(),
            )
            categoryDao.update(updated)
            Result.Success(updated.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi cập nhật danh mục")
        }
    }

    override suspend fun delete(categoryId: String): Result<Unit> {
        return try {
            val count = categoryDao.getProductCount(categoryId)
            if (count > 0) {
                return Result.Error(ErrorCode.CATEGORY_HAS_PRODUCTS, "Không thể xoá danh mục đang có $count sản phẩm")
            }
            categoryDao.softDelete(categoryId, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi xoá danh mục")
        }
    }

    override fun observeCategories(storeId: String): Flow<List<Category>> =
        categoryDao.observeCategories(storeId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(storeId: String): List<Category> =
        categoryDao.getAll(storeId).map { it.toDomain() }
}

// ============ Product Repository ============

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val inventoryDao: InventoryDao,
    private val prefs: AppPreferences,
) : ProductRepository {

    override suspend fun create(product: Product): Result<Product> {
        return try {
            val now = DateUtils.now()
            val deviceId = prefs.getDeviceIdSync()
            val entity = ProductEntity(
                id = product.id,
                storeId = product.storeId,
                categoryId = product.categoryId,
                supplierId = product.supplierId,
                sku = product.sku,
                barcode = product.barcode,
                name = product.name,
                description = product.description,
                costPrice = product.costPrice,
                sellingPrice = product.sellingPrice,
                unit = product.unit,
                imagePath = product.imagePath,
                additionalImages = product.additionalImages.takeIf { it.isNotEmpty() }?.joinToString("|"),
                minStock = product.minStock,
                maxStock = product.maxStock,
                trackInventory = product.trackInventory,
                taxRate = product.taxRate,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceId,
            )
            productDao.insert(entity)

            // Create inventory record
            if (product.trackInventory) {
                inventoryDao.insert(
                    InventoryEntity(
                        id = UuidGenerator.generate(),
                        storeId = product.storeId,
                        productId = product.id,
                        quantity = 0.0,
                        createdAt = now,
                        updatedAt = now,
                        deviceId = deviceId,
                    )
                )
            }
            Result.Success(entity.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi tạo sản phẩm")
        }
    }

    override suspend fun update(product: Product): Result<Product> {
        return try {
            val entity = productDao.getById(product.id) ?: return Result.Error(ErrorCode.INVALID_INPUT, "Không tìm thấy sản phẩm")
            val updated = entity.copy(
                name = product.name,
                categoryId = product.categoryId,
                supplierId = product.supplierId,
                barcode = product.barcode,
                description = product.description,
                costPrice = product.costPrice,
                sellingPrice = product.sellingPrice,
                unit = product.unit,
                imagePath = product.imagePath,
                additionalImages = product.additionalImages.takeIf { it.isNotEmpty() }?.joinToString("|"),
                minStock = product.minStock,
                maxStock = product.maxStock,
                trackInventory = product.trackInventory,
                taxRate = product.taxRate,
                updatedAt = DateUtils.now(),
            )
            productDao.update(updated)
            Result.Success(updated.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi cập nhật sản phẩm")
        }
    }

    override suspend fun delete(productId: String): Result<Unit> {
        return try {
            productDao.softDelete(productId, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi xoá sản phẩm")
        }
    }

    override fun observeProducts(storeId: String): Flow<List<Product>> =
        productDao.observeProducts(storeId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(storeId: String): List<Product> =
        productDao.getAll(storeId).map { it.toDomain() }

    override suspend fun getByCategory(storeId: String, categoryId: String): List<Product> =
        productDao.getByCategory(storeId, categoryId).map { it.toDomain() }

    override suspend fun search(storeId: String, query: String): List<Product> =
        productDao.search(storeId, query).map { it.toDomain() }

    override suspend fun getByBarcode(storeId: String, barcode: String): Product? =
        productDao.getByBarcode(storeId, barcode)?.toDomain()

    override suspend fun generateSku(storeId: String): String {
        val maxNum = productDao.getMaxSkuNumber(storeId) ?: 0
        return "SP${(maxNum + 1).toString().padStart(4, '0')}"
    }
}

// ============ Supplier Repository ============

@Singleton
class SupplierRepositoryImpl @Inject constructor(
    private val supplierDao: SupplierDao,
    private val prefs: AppPreferences,
) : SupplierRepository {

    override suspend fun create(supplier: Supplier): Result<Supplier> {
        return try {
            val now = DateUtils.now()
            val entity = SupplierEntity(
                id = supplier.id,
                storeId = supplier.storeId,
                name = supplier.name,
                contactPerson = supplier.contactPerson,
                phone = supplier.phone,
                email = supplier.email,
                address = supplier.address,
                taxCode = supplier.taxCode,
                notes = supplier.notes,
                createdAt = now,
                updatedAt = now,
                deviceId = prefs.getDeviceIdSync(),
            )
            supplierDao.insert(entity)
            Result.Success(entity.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi tạo nhà cung cấp")
        }
    }

    override suspend fun update(supplier: Supplier): Result<Supplier> {
        return try {
            val entity = supplierDao.getById(supplier.id) ?: return Result.Error(ErrorCode.INVALID_INPUT, "Không tìm thấy nhà cung cấp")
            val updated = entity.copy(
                name = supplier.name,
                contactPerson = supplier.contactPerson,
                phone = supplier.phone,
                email = supplier.email,
                address = supplier.address,
                taxCode = supplier.taxCode,
                notes = supplier.notes,
                updatedAt = DateUtils.now(),
            )
            supplierDao.update(updated)
            Result.Success(updated.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi cập nhật nhà cung cấp")
        }
    }

    override suspend fun delete(supplierId: String): Result<Unit> {
        return try {
            supplierDao.softDelete(supplierId, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi xoá nhà cung cấp")
        }
    }

    override fun observeSuppliers(storeId: String): Flow<List<Supplier>> =
        supplierDao.observeSuppliers(storeId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(storeId: String): List<Supplier> =
        supplierDao.getAll(storeId).map { it.toDomain() }
}

// ============ Customer Repository ============

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val prefs: AppPreferences,
) : CustomerRepository {

    override suspend fun create(customer: Customer): Result<Customer> {
        return try {
            val now = DateUtils.now()
            val entity = CustomerEntity(
                id = customer.id,
                storeId = customer.storeId,
                name = customer.name,
                phone = customer.phone,
                email = customer.email,
                address = customer.address,
                notes = customer.notes,
                createdAt = now,
                updatedAt = now,
                deviceId = prefs.getDeviceIdSync(),
            )
            customerDao.insert(entity)
            Result.Success(entity.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi tạo khách hàng")
        }
    }

    override suspend fun update(customer: Customer): Result<Customer> {
        return try {
            val entity = customerDao.getById(customer.id) ?: return Result.Error(ErrorCode.INVALID_INPUT, "Không tìm thấy khách hàng")
            val updated = entity.copy(
                name = customer.name,
                phone = customer.phone,
                email = customer.email,
                address = customer.address,
                notes = customer.notes,
                updatedAt = DateUtils.now(),
            )
            customerDao.update(updated)
            Result.Success(updated.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi cập nhật khách hàng")
        }
    }

    override suspend fun delete(customerId: String): Result<Unit> {
        return try {
            customerDao.softDelete(customerId, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi xoá khách hàng")
        }
    }

    override fun observeCustomers(storeId: String): Flow<List<Customer>> =
        customerDao.observeCustomers(storeId).map { list -> list.map { it.toDomain() } }

    override suspend fun search(storeId: String, query: String): List<Customer> =
        customerDao.search(storeId, query).map { it.toDomain() }

    override suspend fun getRecent(storeId: String, limit: Int): List<Customer> =
        customerDao.getRecent(storeId, limit).map { it.toDomain() }
}

// ============ Order Repository ============

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderDao: OrderDao,
    private val inventoryDao: InventoryDao,
    private val customerDao: CustomerDao,
    private val prefs: AppPreferences,
) : OrderRepository {

    override suspend fun createOrder(cart: Cart, userId: String, storeId: String, payments: List<OrderPayment>): Result<Order> {
        return try {
            val now = DateUtils.now()
            val deviceId = prefs.getDeviceIdSync()

            // Validate stock availability before creating order
            for (item in cart.items) {
                if (item.product.trackInventory) {
                    val inv = inventoryDao.getByProduct(storeId, item.product.id)
                    val available = inv?.quantity ?: 0.0
                    if (item.quantity > available) {
                        return Result.Error(
                            ErrorCode.INSUFFICIENT_STOCK,
                            "\"${item.product.name}\" chỉ còn ${available.toLong()} ${item.product.unit} trong kho, không đủ ${item.quantity.toLong()}"
                        )
                    }
                }
            }

            val dateStr = DateUtils.formatOrderDate(now)
            val prefix = "${AppConstants.ORDER_CODE_PREFIX}-$dateStr-"
            val sequence = orderDao.getMaxOrderSequence(storeId, prefix) + 1
            val orderCode = "$prefix${sequence.toString().padStart(3, '0')}"
            val orderId = UuidGenerator.generate()

            // Create order
            val orderEntity = OrderEntity(
                id = orderId,
                storeId = storeId,
                orderCode = orderCode,
                customerId = cart.customer?.id,
                customerName = cart.customer?.name,
                customerPhone = cart.customer?.phone,
                subtotal = cart.subtotal,
                discountType = cart.orderDiscount?.type,
                discountValue = cart.orderDiscount?.value ?: 0.0,
                discountAmount = cart.orderDiscountAmount,
                taxAmount = cart.taxAmount,
                totalAmount = cart.grandTotal,
                status = "completed",
                notes = cart.notes,
                createdBy = userId,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceId,
            )
            orderDao.insertOrder(orderEntity)

            // Create order items
            val orderItems = cart.items.map { item ->
                OrderItemEntity(
                    id = UuidGenerator.generate(),
                    orderId = orderId,
                    productId = item.product.id,
                    variantId = item.variant?.id,
                    productName = item.product.name,
                    variantName = item.variant?.variantName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    costPrice = item.product.costPrice,
                    discountType = item.discount?.type,
                    discountValue = item.discount?.value ?: 0.0,
                    discountAmount = item.discountAmount,
                    taxAmount = item.taxAmount,
                    totalPrice = item.lineTotal,
                    createdAt = now,
                    updatedAt = now,
                    deviceId = deviceId,
                )
            }
            orderDao.insertOrderItems(orderItems)

            // Create payments
            val paymentEntities = payments.map { payment ->
                OrderPaymentEntity(
                    id = payment.id,
                    orderId = orderId,
                    method = payment.method.name.lowercase(),
                    amount = payment.amount,
                    receivedAmount = payment.receivedAmount,
                    changeAmount = payment.changeAmount,
                    referenceNo = payment.referenceNo,
                    notes = payment.notes,
                    createdAt = now,
                    updatedAt = now,
                    deviceId = deviceId,
                )
            }
            orderDao.insertOrderPayments(paymentEntities)

            // Update inventory for each item
            for (item in cart.items) {
                if (item.product.trackInventory) {
                    val inv = inventoryDao.getByProduct(storeId, item.product.id)
                    if (inv != null) {
                        val quantityBefore = inv.quantity
                        inventoryDao.adjustQuantity(inv.id, -item.quantity, now)
                        // Log stock movement
                        orderDao.insertStockMovement(
                            StockMovementEntity(
                                id = UuidGenerator.generate(),
                                storeId = storeId,
                                productId = item.product.id,
                                variantId = item.variant?.id,
                                type = "sale_out",
                                quantity = -item.quantity,
                                quantityBefore = quantityBefore,
                                quantityAfter = quantityBefore - item.quantity,
                                unitCost = item.product.costPrice,
                                referenceId = orderId,
                                referenceType = "order",
                                createdBy = userId,
                                createdAt = now,
                                updatedAt = now,
                                deviceId = deviceId,
                            )
                        )
                    }
                }
            }

            // Update customer visit
            cart.customer?.let {
                customerDao.incrementVisit(it.id, cart.grandTotal, now)
            }

            val order = Order(
                id = orderId,
                storeId = storeId,
                orderCode = orderCode,
                customerId = cart.customer?.id,
                customerName = cart.customer?.name,
                customerPhone = cart.customer?.phone,
                subtotal = cart.subtotal,
                discountAmount = cart.orderDiscountAmount,
                taxAmount = cart.taxAmount,
                totalAmount = cart.grandTotal,
                status = OrderStatus.COMPLETED,
                createdBy = userId,
                createdAt = now,
                updatedAt = now,
            )
            Result.Success(order)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi tạo đơn hàng")
        }
    }

    override fun observeOrders(storeId: String): Flow<List<Order>> =
        orderDao.observeOrders(storeId).map { list -> list.map { it.toDomain() } }

    override suspend fun getOrderDetail(orderId: String): OrderDetail? {
        val order = orderDao.getById(orderId) ?: return null
        val items = orderDao.getOrderItems(orderId)
        val payments = orderDao.getOrderPayments(orderId)
        return OrderDetail(
            order = order.toDomain(),
            items = items.map { it.toDomain() },
            payments = payments.map { it.toDomain() },
        )
    }

    override suspend fun getOrdersByDateRange(storeId: String, startTime: Long, endTime: Long): List<Order> =
        orderDao.getOrdersByDateRange(storeId, startTime, endTime).map { it.toDomain() }

    override suspend fun getTodayRevenue(storeId: String): Double {
        val now = DateUtils.now()
        return orderDao.getTotalRevenue(storeId, DateUtils.startOfDay(now), DateUtils.endOfDay(now))
    }

    override suspend fun getTodayOrderCount(storeId: String): Int {
        val now = DateUtils.now()
        return orderDao.getOrderCount(storeId, DateUtils.startOfDay(now), DateUtils.endOfDay(now))
    }

    override suspend fun getDashboardData(storeId: String): DashboardData {
        val now = DateUtils.now()
        val startOfDay = DateUtils.startOfDay(now)
        val endOfDay = DateUtils.endOfDay(now)
        val revenue = orderDao.getTotalRevenue(storeId, startOfDay, endOfDay)
        val orderCount = orderDao.getOrderCount(storeId, startOfDay, endOfDay)
        val lowStock = inventoryDao.getLowStockCount(storeId)
        return DashboardData(
            todayRevenue = revenue,
            todayOrders = orderCount,
            lowStockCount = lowStock,
        )
    }
}

// ============ Inventory Repository ============

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val orderDao: OrderDao,
    private val prefs: AppPreferences,
) : InventoryRepository {

    override suspend fun getStock(storeId: String, productId: String): InventoryItem? {
        return inventoryDao.getByProduct(storeId, productId)?.let {
            InventoryItem(it.id, it.storeId, it.productId, it.variantId, it.quantity, it.reservedQty)
        }
    }

    override suspend fun adjustStock(
        storeId: String, productId: String, amount: Double,
        type: StockMovementType, userId: String, referenceId: String?, supplierId: String?
    ): Result<Unit> {
        return try {
            val now = DateUtils.now()
            val inv = inventoryDao.getByProduct(storeId, productId)
                ?: return Result.Error(ErrorCode.INVALID_INPUT, "Không tìm thấy tồn kho sản phẩm")
            val before = inv.quantity
            inventoryDao.adjustQuantity(inv.id, amount, now)
            orderDao.insertStockMovement(
                StockMovementEntity(
                    id = UuidGenerator.generate(),
                    storeId = storeId,
                    productId = productId,
                    supplierId = supplierId,
                    type = type.name.lowercase(),
                    quantity = amount,
                    quantityBefore = before,
                    quantityAfter = before + amount,
                    referenceId = referenceId,
                    createdBy = userId,
                    createdAt = now,
                    updatedAt = now,
                    deviceId = prefs.getDeviceIdSync(),
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Lỗi điều chỉnh tồn kho")
        }
    }

    override suspend fun getLowStockCount(storeId: String): Int = inventoryDao.getLowStockCount(storeId)

    override suspend fun getAllStockOverview(storeId: String): List<StockOverviewItem> {
        return inventoryDao.getAllStockWithProduct(storeId).map {
            StockOverviewItem(
                productId = it.productId,
                productName = it.productName,
                productSku = it.productSku,
                productUnit = it.productUnit,
                minStock = it.productMinStock,
                costPrice = it.productCostPrice,
                sellingPrice = it.productSellingPrice,
                currentStock = it.quantity,
                stockValue = it.quantity * it.productCostPrice,
            )
        }
    }

    override suspend fun getStockHistory(storeId: String, startTime: Long, endTime: Long): List<StockHistoryItem> {
        return inventoryDao.getStockMovements(storeId, startTime, endTime).map { it.toHistoryItem() }
    }

    override suspend fun getStockHistoryByProduct(storeId: String, productId: String): List<StockHistoryItem> {
        return inventoryDao.getStockMovementsByProduct(storeId, productId).map { it.toHistoryItem() }
    }

    override suspend fun getStockSummary(storeId: String, startTime: Long, endTime: Long): StockSummary {
        val overview = getAllStockOverview(storeId)
        val totalProducts = overview.size
        val totalStockValue = overview.sumOf { it.stockValue }
        val lowStockCount = overview.count { it.currentStock > 0 && it.currentStock <= it.minStock }
        val outOfStockCount = overview.count { it.currentStock <= 0 }
        val totalStockIn = inventoryDao.getTotalStockIn(storeId, startTime, endTime)
        val totalStockOut = inventoryDao.getTotalStockOut(storeId, startTime, endTime)
        return StockSummary(
            totalProducts = totalProducts,
            totalStockValue = totalStockValue,
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            totalStockIn = totalStockIn,
            totalStockOut = totalStockOut,
        )
    }

    private fun com.minipos.data.database.dao.StockMovementWithProduct.toHistoryItem() = StockHistoryItem(
        id = id,
        productId = productId,
        productName = productName,
        productSku = productSku,
        type = StockMovementType.fromString(type),
        quantity = quantity,
        quantityBefore = quantityBefore,
        quantityAfter = quantityAfter,
        unitCost = unitCost,
        referenceId = referenceId,
        referenceType = referenceType,
        supplierId = supplierId,
        supplierName = supplierName,
        notes = notes,
        createdBy = createdBy,
        createdAt = createdAt,
    )
}

// ============ Entity Mappers ============

fun StoreEntity.toDomain(): Store {
    val storeSettings = try {
        if (!settings.isNullOrBlank()) {
            com.google.gson.Gson().fromJson(settings, StoreSettings::class.java)
        } else StoreSettings()
    } catch (_: Exception) { StoreSettings() }
    return Store(
        id = id, name = name, code = code, address = address, phone = phone,
        logoPath = logoPath, settings = storeSettings, currency = currency,
        createdAt = createdAt, updatedAt = updatedAt,
    )
}

fun UserEntity.toDomain() = User(
    id = id, storeId = storeId, displayName = displayName, avatarPath = avatarPath,
    role = UserRole.fromString(role), isActive = isActive, lastLoginAt = lastLoginAt,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun CategoryEntity.toDomain() = Category(
    id = id, storeId = storeId, parentId = parentId, name = name,
    description = description, icon = icon, color = color, sortOrder = sortOrder,
    isActive = isActive, createdAt = createdAt, updatedAt = updatedAt,
)

fun ProductEntity.toDomain() = Product(
    id = id, storeId = storeId, categoryId = categoryId, supplierId = supplierId,
    sku = sku, barcode = barcode, name = name, description = description,
    costPrice = costPrice, sellingPrice = sellingPrice, unit = unit, imagePath = imagePath,
    additionalImages = additionalImages?.takeIf { it.isNotBlank() }
        ?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
    minStock = minStock, maxStock = maxStock, isActive = isActive,
    trackInventory = trackInventory, taxRate = taxRate, hasVariants = hasVariants,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun SupplierEntity.toDomain() = Supplier(
    id = id, storeId = storeId, name = name, contactPerson = contactPerson,
    phone = phone, email = email, address = address, taxCode = taxCode,
    notes = notes, isActive = isActive, createdAt = createdAt, updatedAt = updatedAt,
)

fun CustomerEntity.toDomain() = Customer(
    id = id, storeId = storeId, name = name, phone = phone, email = email,
    address = address, notes = notes, totalSpent = totalSpent, visitCount = visitCount,
    lastVisitAt = lastVisitAt, createdAt = createdAt, updatedAt = updatedAt,
)

fun OrderEntity.toDomain() = Order(
    id = id, storeId = storeId, orderCode = orderCode, customerId = customerId,
    customerName = customerName, customerPhone = customerPhone, subtotal = subtotal,
    discountType = discountType, discountValue = discountValue, discountAmount = discountAmount,
    taxAmount = taxAmount, totalAmount = totalAmount, status = OrderStatus.fromString(status),
    notes = notes, createdBy = createdBy, createdAt = createdAt, updatedAt = updatedAt,
)

fun OrderItemEntity.toDomain() = OrderItem(
    id = id, orderId = orderId, productId = productId, variantId = variantId,
    productName = productName, variantName = variantName, quantity = quantity,
    unitPrice = unitPrice, costPrice = costPrice, discountType = discountType,
    discountValue = discountValue, discountAmount = discountAmount, taxAmount = taxAmount,
    totalPrice = totalPrice,
)

fun OrderPaymentEntity.toDomain() = OrderPayment(
    id = id, orderId = orderId, method = PaymentMethod.fromString(method),
    amount = amount, receivedAmount = receivedAmount, changeAmount = changeAmount,
    referenceNo = referenceNo, notes = notes,
)
