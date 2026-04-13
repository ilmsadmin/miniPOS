package com.minipos.data.repository

import android.content.Context
import com.minipos.R
import com.minipos.core.constants.AppConstants
import com.minipos.core.utils.DateUtils
import com.minipos.core.utils.HashUtils
import com.minipos.core.utils.UuidGenerator
import com.minipos.data.database.dao.*
import com.minipos.data.database.entity.*
import com.minipos.data.preferences.AppPreferences
import com.minipos.domain.model.*
import com.minipos.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ============ Store Repository ============

@Singleton
class StoreRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storeDao: StoreDao
) : StoreRepository {
    override suspend fun getStore(): Store? = storeDao.getStore()?.toDomain()

    override fun observeStore(): Flow<Store?> = storeDao.observeStore().map { it?.toDomain() }

    override suspend fun updateStore(store: Store): Result<Store> {
        return try {
            val entity = storeDao.getStore() ?: return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_store_not_found))
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
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_update_store))
        }
    }
}

// ============ User Repository ============

@Singleton
class UserRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
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
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_create_user))
        }
    }

    override suspend fun updateUser(user: User): Result<User> {
        return try {
            val entity = userDao.getById(user.id) ?: return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_user_not_found))
            val updated = entity.copy(
                displayName = user.displayName,
                role = user.role.name.lowercase(),
                isActive = user.isActive,
                updatedAt = DateUtils.now(),
            )
            userDao.update(updated)
            Result.Success(updated.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_update_user))
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            userDao.softDelete(userId, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_delete_user))
        }
    }

    override suspend fun getUserById(userId: String): User? = userDao.getById(userId)?.toDomain()

    override fun observeUsers(storeId: String): Flow<List<User>> =
        userDao.observeUsers(storeId).map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveUsers(storeId: String): List<User> =
        userDao.getActiveUsers(storeId).map { it.toDomain() }

    override suspend fun resetPin(userId: String, newPin: String): Result<Unit> {
        return try {
            val entity = userDao.getById(userId) ?: return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_user_not_found))
            val updated = entity.copy(
                pinHash = HashUtils.hashPin(newPin),
                updatedAt = DateUtils.now(),
            )
            userDao.update(updated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_reset_pin))
        }
    }

    override suspend fun clearPin(userId: String): Result<Unit> {
        return try {
            val entity = userDao.getById(userId) ?: return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_user_not_found))
            userDao.update(entity.copy(pinHash = "", updatedAt = DateUtils.now()))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_reset_pin))
        }
    }

    override suspend fun hasPin(userId: String): Boolean {
        return userDao.getById(userId)?.pinHash?.isNotBlank() == true
    }

    override suspend fun verifyPin(userId: String, pin: String): Boolean {
        val entity = userDao.getById(userId) ?: return false
        if (entity.pinHash.isBlank()) return false
        return HashUtils.verifyPin(pin, entity.pinHash)
    }
}

// ============ Category Repository ============

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
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
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_create_category))
        }
    }

    override suspend fun update(category: Category): Result<Category> {
        return try {
            val entity = categoryDao.getById(category.id) ?: return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_category_not_found))
            val updated = entity.copy(
                name = category.name,
                description = category.description,
                parentId = category.parentId,
                icon = category.icon,
                color = category.color,
                sortOrder = category.sortOrder,
                updatedAt = DateUtils.now(),
            )
            categoryDao.update(updated)
            Result.Success(updated.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_update_category))
        }
    }

    override suspend fun delete(categoryId: String): Result<Unit> {
        return try {
            val count = categoryDao.getProductCount(categoryId)
            if (count > 0) {
                return Result.Error(ErrorCode.CATEGORY_HAS_PRODUCTS, context.getString(R.string.error_category_has_products, count))
            }
            categoryDao.softDelete(categoryId, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_delete_category))
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
    @ApplicationContext private val context: Context,
    private val productDao: ProductDao,
    private val productVariantDao: ProductVariantDao,
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
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_create_product))
        }
    }

    override suspend fun update(product: Product): Result<Product> {
        return try {
            val entity = productDao.getById(product.id) ?: return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_product_not_found_repo))
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
                hasVariants = product.hasVariants,
                updatedAt = DateUtils.now(),
            )
            productDao.update(updated)
            Result.Success(updated.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_update_product))
        }
    }

    override suspend fun delete(productId: String): Result<Unit> {
        return try {
            productDao.softDelete(productId, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_delete_product))
        }
    }

    override fun observeProducts(storeId: String): Flow<List<Product>> =
        productDao.observeProducts(storeId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(productId: String): Product? =
        productDao.getById(productId)?.toDomain()

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

    // ---- Variants ----

    override suspend fun createVariant(variant: ProductVariant): Result<ProductVariant> {
        return try {
            val now = DateUtils.now()
            val deviceId = prefs.getDeviceIdSync()
            val entity = ProductVariantEntity(
                id = variant.id,
                storeId = variant.storeId,
                productId = variant.productId,
                variantName = variant.variantName,
                sku = variant.sku,
                barcode = variant.barcode,
                costPrice = variant.costPrice,
                sellingPrice = variant.sellingPrice,
                attributes = variant.attributes,
                isActive = variant.isActive,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceId,
            )
            productVariantDao.insert(entity)
            Result.Success(entity.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Error creating variant")
        }
    }

    override suspend fun updateVariant(variant: ProductVariant): Result<ProductVariant> {
        return try {
            val entity = productVariantDao.getById(variant.id)
                ?: return Result.Error(ErrorCode.INVALID_INPUT, "Variant not found")
            val updated = entity.copy(
                variantName = variant.variantName,
                sku = variant.sku,
                barcode = variant.barcode,
                costPrice = variant.costPrice,
                sellingPrice = variant.sellingPrice,
                attributes = variant.attributes,
                isActive = variant.isActive,
                updatedAt = DateUtils.now(),
            )
            productVariantDao.update(updated)
            Result.Success(updated.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Error updating variant")
        }
    }

    override suspend fun deleteVariant(variantId: String): Result<Unit> {
        return try {
            productVariantDao.softDelete(variantId, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Error deleting variant")
        }
    }

    override suspend fun deleteAllVariants(productId: String): Result<Unit> {
        return try {
            productVariantDao.softDeleteByProductId(productId, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Error deleting all variants")
        }
    }

    override fun observeVariants(productId: String): Flow<List<ProductVariant>> =
        productVariantDao.observeByProductId(productId).map { list -> list.map { it.toDomain() } }

    override suspend fun getVariants(productId: String): List<ProductVariant> =
        productVariantDao.getByProductId(productId).map { it.toDomain() }

    override suspend fun getVariantByBarcode(storeId: String, barcode: String): ProductVariant? =
        productVariantDao.getByBarcode(storeId, barcode)?.toDomain()

    override suspend fun getVariantCount(productId: String): Int =
        productVariantDao.getCountByProductId(productId)

    override suspend fun getNextVariantSku(productId: String, baseSku: String): String {
        val maxSuffix = productVariantDao.getMaxSkuSuffix(productId, baseSku) ?: 0
        return "$baseSku-${maxSuffix + 1}"
    }
}

// ============ Supplier Repository ============

@Singleton
class SupplierRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
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
                mobile = supplier.mobile,
                email = supplier.email,
                address = supplier.address,
                taxCode = supplier.taxCode,
                paymentTerm = supplier.paymentTerm,
                bankName = supplier.bankName,
                bankAccount = supplier.bankAccount,
                bankAccountHolder = supplier.bankAccountHolder,
                notes = supplier.notes,
                createdAt = now,
                updatedAt = now,
                deviceId = prefs.getDeviceIdSync(),
            )
            supplierDao.insert(entity)
            Result.Success(entity.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_create_supplier))
        }
    }

    override suspend fun update(supplier: Supplier): Result<Supplier> {
        return try {
            val entity = supplierDao.getById(supplier.id) ?: return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_supplier_not_found))
            val updated = entity.copy(
                name = supplier.name,
                contactPerson = supplier.contactPerson,
                phone = supplier.phone,
                mobile = supplier.mobile,
                email = supplier.email,
                address = supplier.address,
                taxCode = supplier.taxCode,
                paymentTerm = supplier.paymentTerm,
                bankName = supplier.bankName,
                bankAccount = supplier.bankAccount,
                bankAccountHolder = supplier.bankAccountHolder,
                notes = supplier.notes,
                updatedAt = DateUtils.now(),
            )
            supplierDao.update(updated)
            Result.Success(updated.toDomain())
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_update_supplier))
        }
    }

    override suspend fun delete(supplierId: String): Result<Unit> {
        return try {
            supplierDao.softDelete(supplierId, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_delete_supplier))
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
    @ApplicationContext private val context: Context,
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
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_create_customer))
        }
    }

    override suspend fun update(customer: Customer): Result<Customer> {
        return try {
            val entity = customerDao.getById(customer.id) ?: return Result.Error(ErrorCode.INVALID_INPUT, context.getString(R.string.error_customer_not_found))
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
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_update_customer))
        }
    }

    override suspend fun delete(customerId: String): Result<Unit> {
        return try {
            customerDao.softDelete(customerId, DateUtils.now())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_delete_customer))
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
    @ApplicationContext private val context: Context,
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
                    val inv = if (item.variant != null) {
                        inventoryDao.getByVariant(storeId, item.product.id, item.variant.id)
                            ?: inventoryDao.getByProduct(storeId, item.product.id)
                    } else {
                        inventoryDao.getByProduct(storeId, item.product.id)
                    }
                    val available = inv?.quantity ?: 0.0
                    if (item.quantity > available) {
                        return Result.Error(
                            ErrorCode.INSUFFICIENT_STOCK,
                            context.getString(R.string.error_insufficient_stock, item.product.name, available.toLong(), item.product.unit, item.quantity.toLong())
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
                    costPrice = item.variant?.costPrice ?: item.product.costPrice,
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
                    // Try variant-level first, fallback to product-level
                    val inv = if (item.variant != null) {
                        inventoryDao.getByVariant(storeId, item.product.id, item.variant.id)
                            ?: inventoryDao.getByProduct(storeId, item.product.id)
                    } else {
                        inventoryDao.getByProduct(storeId, item.product.id)
                    }
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
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_create_order))
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
    @ApplicationContext private val context: Context,
    private val inventoryDao: InventoryDao,
    private val orderDao: OrderDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val prefs: AppPreferences,
) : InventoryRepository {

    override suspend fun getStock(storeId: String, productId: String): InventoryItem? {
        return inventoryDao.getByProduct(storeId, productId)?.let {
            InventoryItem(it.id, it.storeId, it.productId, it.variantId, it.quantity, it.reservedQty)
        }
    }

    override suspend fun getTotalStock(storeId: String, productId: String, hasVariants: Boolean): Double {
        return if (hasVariants) {
            inventoryDao.getTotalStockForProduct(storeId, productId)
        } else {
            inventoryDao.getProductLevelStock(storeId, productId)
        }
    }

    override suspend fun getVariantStock(storeId: String, productId: String, variantId: String): InventoryItem? {
        return inventoryDao.getByVariant(storeId, productId, variantId)?.let {
            InventoryItem(it.id, it.storeId, it.productId, it.variantId, it.quantity, it.reservedQty)
        }
    }

    override fun observeInventoryChanges(storeId: String): Flow<Long> {
        return inventoryDao.observeAllInventory(storeId).map { list ->
            // Emit the max updatedAt as a change signal; any inventory row change triggers re-emission
            list.maxOfOrNull { it.updatedAt } ?: 0L
        }
    }

    override suspend fun adjustStock(
        storeId: String, productId: String, amount: Double,
        type: StockMovementType, userId: String, referenceId: String?, supplierId: String?,
        notes: String?
    ): Result<Unit> {
        return try {
            val now = DateUtils.now()
            val deviceId = prefs.getDeviceIdSync()
            // Find or create product-level inventory record
            val inv = inventoryDao.getByProduct(storeId, productId)
                ?: run {
                    val newInv = InventoryEntity(
                        id = UuidGenerator.generate(),
                        storeId = storeId,
                        productId = productId,
                        variantId = null,
                        quantity = 0.0,
                        createdAt = now,
                        updatedAt = now,
                        deviceId = deviceId,
                    )
                    inventoryDao.insert(newInv)
                    newInv
                }
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
                    notes = notes,
                    createdBy = userId,
                    createdAt = now,
                    updatedAt = now,
                    deviceId = deviceId,
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_adjust_inventory))
        }
    }

    override suspend fun adjustVariantStock(
        storeId: String, productId: String, variantId: String, amount: Double,
        type: StockMovementType, userId: String, referenceId: String?, supplierId: String?
    ): Result<Unit> {
        return try {
            val now = DateUtils.now()
            val deviceId = prefs.getDeviceIdSync()
            // Find or create variant-level inventory record
            val inv = inventoryDao.getByVariant(storeId, productId, variantId)
                ?: run {
                    val newInv = InventoryEntity(
                        id = UuidGenerator.generate(),
                        storeId = storeId,
                        productId = productId,
                        variantId = variantId,
                        quantity = 0.0,
                        createdAt = now,
                        updatedAt = now,
                        deviceId = deviceId,
                    )
                    inventoryDao.insert(newInv)
                    newInv
                }
            val before = inv.quantity
            inventoryDao.adjustQuantity(inv.id, amount, now)
            orderDao.insertStockMovement(
                StockMovementEntity(
                    id = UuidGenerator.generate(),
                    storeId = storeId,
                    productId = productId,
                    variantId = variantId,
                    supplierId = supplierId,
                    type = type.name.lowercase(),
                    quantity = amount,
                    quantityBefore = before,
                    quantityAfter = before + amount,
                    referenceId = referenceId,
                    createdBy = userId,
                    createdAt = now,
                    updatedAt = now,
                    deviceId = deviceId,
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: context.getString(R.string.error_adjust_inventory))
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

    override suspend fun mergeVariantStockToProduct(storeId: String, productId: String): Result<Unit> {
        return try {
            val now = DateUtils.now()
            val deviceId = prefs.getDeviceIdSync()
            // Sum all variant-level stock
            val variantStockTotal = inventoryDao.getVariantStockSum(storeId, productId)
            // Get or create product-level record
            val productInv = inventoryDao.getByProduct(storeId, productId)
            if (productInv != null) {
                // Add variant stock to product-level
                inventoryDao.adjustQuantity(productInv.id, variantStockTotal, now)
            } else if (variantStockTotal > 0) {
                inventoryDao.insert(
                    InventoryEntity(
                        id = UuidGenerator.generate(),
                        storeId = storeId,
                        productId = productId,
                        variantId = null,
                        quantity = variantStockTotal,
                        createdAt = now,
                        updatedAt = now,
                        deviceId = deviceId,
                    )
                )
            }
            // Delete all variant inventory records
            inventoryDao.deleteVariantInventory(storeId, productId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Error merging variant stock")
        }
    }

    override suspend fun savePurchaseOrder(
        storeId: String, code: String, supplierId: String?, supplierName: String?,
        totalAmount: Double, totalItems: Int, notes: String?, userId: String,
        items: List<PurchaseOrderItem>,
    ): Result<String> {
        return try {
            val now = DateUtils.now()
            val deviceId = prefs.getDeviceIdSync()
            val poId = UuidGenerator.generate()

            val entity = PurchaseOrderEntity(
                id = poId,
                storeId = storeId,
                code = code,
                supplierId = supplierId,
                supplierName = supplierName,
                totalAmount = totalAmount,
                totalItems = totalItems,
                notes = notes,
                status = "confirmed",
                createdBy = userId,
                confirmedAt = now,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceId,
            )
            purchaseOrderDao.insert(entity)

            val itemEntities = items.map { item ->
                PurchaseOrderItemEntity(
                    id = item.id,
                    purchaseOrderId = poId,
                    productId = item.productId,
                    variantId = item.variantId,
                    productName = item.productName,
                    variantName = item.variantName,
                    quantity = item.quantity,
                    unitCost = item.unitCost,
                    totalCost = item.totalCost,
                    createdAt = now,
                    updatedAt = now,
                    deviceId = deviceId,
                )
            }
            purchaseOrderDao.insertItems(itemEntities)

            Result.Success(poId)
        } catch (e: Exception) {
            Result.Error(ErrorCode.DATABASE_ERROR, e.message ?: "Error saving purchase order")
        }
    }

    override suspend fun getRecentPurchaseOrders(storeId: String, limit: Int): List<PurchaseOrder> {
        return purchaseOrderDao.getRecent(storeId, limit).map { entity ->
            PurchaseOrder(
                id = entity.id,
                storeId = entity.storeId,
                code = entity.code,
                supplierId = entity.supplierId,
                supplierName = entity.supplierName,
                totalAmount = entity.totalAmount,
                totalItems = entity.totalItems,
                notes = entity.notes,
                status = entity.status,
                createdBy = entity.createdBy,
                confirmedAt = entity.confirmedAt,
                createdAt = entity.createdAt,
            )
        }
    }

    override suspend fun getPurchaseOrderById(id: String): PurchaseOrder? {
        return purchaseOrderDao.getById(id)?.let { entity ->
            PurchaseOrder(
                id = entity.id,
                storeId = entity.storeId,
                code = entity.code,
                supplierId = entity.supplierId,
                supplierName = entity.supplierName,
                totalAmount = entity.totalAmount,
                totalItems = entity.totalItems,
                notes = entity.notes,
                status = entity.status,
                createdBy = entity.createdBy,
                confirmedAt = entity.confirmedAt,
                createdAt = entity.createdAt,
            )
        }
    }

    override suspend fun getPurchaseOrderItems(orderId: String): List<PurchaseOrderItem> {
        return purchaseOrderDao.getItemsByOrderId(orderId).map { entity ->
            PurchaseOrderItem(
                id = entity.id,
                purchaseOrderId = entity.purchaseOrderId,
                productId = entity.productId,
                variantId = entity.variantId,
                productName = entity.productName,
                variantName = entity.variantName,
                quantity = entity.quantity,
                unitCost = entity.unitCost,
                totalCost = entity.totalCost,
            )
        }
    }

    override suspend fun generatePurchaseOrderCode(storeId: String): String {
        val prefix = "PO-"
        val maxSeq = purchaseOrderDao.getMaxSequence(storeId, prefix)
        return "$prefix${String.format("%04d", maxSeq + 1)}"
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

fun ProductVariantEntity.toDomain() = ProductVariant(
    id = id, storeId = storeId, productId = productId, variantName = variantName,
    sku = sku, barcode = barcode, costPrice = costPrice, sellingPrice = sellingPrice,
    attributes = attributes, isActive = isActive, createdAt = createdAt, updatedAt = updatedAt,
)

fun SupplierEntity.toDomain() = Supplier(
    id = id, storeId = storeId, name = name, contactPerson = contactPerson,
    phone = phone, mobile = mobile, email = email, address = address, taxCode = taxCode,
    paymentTerm = paymentTerm, bankName = bankName, bankAccount = bankAccount,
    bankAccountHolder = bankAccountHolder, notes = notes, isActive = isActive,
    createdAt = createdAt, updatedAt = updatedAt,
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
