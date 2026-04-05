package com.minipos.data.database.entity

import androidx.room.*

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "address") val address: String? = null,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "logo_path") val logoPath: String? = null,
    @ColumnInfo(name = "settings") val settings: String? = null, // JSON
    @ColumnInfo(name = "currency") val currency: String = "VND",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_version") val syncVersion: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
)

@Entity(
    tableName = "users",
    foreignKeys = [ForeignKey(
        entity = StoreEntity::class,
        parentColumns = ["id"],
        childColumns = ["store_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["store_id"])]
)
data class UserEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "avatar_path") val avatarPath: String? = null,
    @ColumnInfo(name = "role") val role: String, // owner, manager, cashier
    @ColumnInfo(name = "pin_hash") val pinHash: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "last_login_at") val lastLoginAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_version") val syncVersion: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(entity = StoreEntity::class, parentColumns = ["id"], childColumns = ["store_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["parent_id"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index(value = ["store_id"]), Index(value = ["parent_id"])]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "parent_id") val parentId: String? = null,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "icon") val icon: String? = null,
    @ColumnInfo(name = "color") val color: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_version") val syncVersion: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
)

@Entity(
    tableName = "suppliers",
    foreignKeys = [ForeignKey(entity = StoreEntity::class, parentColumns = ["id"], childColumns = ["store_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["store_id"])]
)
data class SupplierEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "contact_person") val contactPerson: String? = null,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "mobile") val mobile: String? = null,
    @ColumnInfo(name = "email") val email: String? = null,
    @ColumnInfo(name = "address") val address: String? = null,
    @ColumnInfo(name = "tax_code") val taxCode: String? = null,
    @ColumnInfo(name = "payment_term") val paymentTerm: String? = null,
    @ColumnInfo(name = "bank_name") val bankName: String? = null,
    @ColumnInfo(name = "bank_account") val bankAccount: String? = null,
    @ColumnInfo(name = "bank_account_holder") val bankAccountHolder: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_version") val syncVersion: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(entity = StoreEntity::class, parentColumns = ["id"], childColumns = ["store_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = SupplierEntity::class, parentColumns = ["id"], childColumns = ["supplier_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index(value = ["store_id"]),
        Index(value = ["category_id"]),
        Index(value = ["supplier_id"]),
        Index(value = ["store_id", "sku"]),
        Index(value = ["barcode"]),
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    @ColumnInfo(name = "supplier_id") val supplierId: String? = null,
    @ColumnInfo(name = "sku") val sku: String,
    @ColumnInfo(name = "barcode") val barcode: String? = null,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "cost_price") val costPrice: Double = 0.0,
    @ColumnInfo(name = "selling_price") val sellingPrice: Double = 0.0,
    @ColumnInfo(name = "unit") val unit: String = "pcs",
    @ColumnInfo(name = "image_path") val imagePath: String? = null,
    @ColumnInfo(name = "additional_images") val additionalImages: String? = null,
    @ColumnInfo(name = "min_stock") val minStock: Int = 0,
    @ColumnInfo(name = "max_stock") val maxStock: Int? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "track_inventory") val trackInventory: Boolean = true,
    @ColumnInfo(name = "tax_rate") val taxRate: Double = 0.0,
    @ColumnInfo(name = "has_variants") val hasVariants: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_version") val syncVersion: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
)

@Entity(
    tableName = "product_variants",
    foreignKeys = [
        ForeignKey(entity = StoreEntity::class, parentColumns = ["id"], childColumns = ["store_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["product_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["store_id"]),
        Index(value = ["product_id"]),
        Index(value = ["barcode"]),
    ]
)
data class ProductVariantEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "variant_name") val variantName: String,
    @ColumnInfo(name = "sku") val sku: String,
    @ColumnInfo(name = "barcode") val barcode: String? = null,
    @ColumnInfo(name = "cost_price") val costPrice: Double? = null,
    @ColumnInfo(name = "selling_price") val sellingPrice: Double? = null,
    @ColumnInfo(name = "attributes") val attributes: String = "{}",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_version") val syncVersion: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
)

@Entity(
    tableName = "customers",
    foreignKeys = [ForeignKey(entity = StoreEntity::class, parentColumns = ["id"], childColumns = ["store_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["store_id"]), Index(value = ["store_id", "phone"])]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "email") val email: String? = null,
    @ColumnInfo(name = "address") val address: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "total_spent") val totalSpent: Double = 0.0,
    @ColumnInfo(name = "visit_count") val visitCount: Int = 0,
    @ColumnInfo(name = "last_visit_at") val lastVisitAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_version") val syncVersion: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
)

@Entity(
    tableName = "inventory",
    foreignKeys = [
        ForeignKey(entity = StoreEntity::class, parentColumns = ["id"], childColumns = ["store_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["product_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["product_id"]), Index(value = ["store_id", "product_id", "variant_id"], unique = true)]
)
data class InventoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "variant_id") val variantId: String? = null,
    @ColumnInfo(name = "quantity") val quantity: Double = 0.0,
    @ColumnInfo(name = "reserved_qty") val reservedQty: Double = 0.0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_version") val syncVersion: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
)

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(entity = StoreEntity::class, parentColumns = ["id"], childColumns = ["store_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["store_id"]),
        Index(value = ["order_code"]),
        Index(value = ["created_at"]),
        Index(value = ["customer_id"]),
    ]
)
data class OrderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "order_code") val orderCode: String,
    @ColumnInfo(name = "customer_id") val customerId: String? = null,
    @ColumnInfo(name = "customer_name") val customerName: String? = null,
    @ColumnInfo(name = "customer_phone") val customerPhone: String? = null,
    @ColumnInfo(name = "subtotal") val subtotal: Double = 0.0,
    @ColumnInfo(name = "discount_type") val discountType: String? = null,
    @ColumnInfo(name = "discount_value") val discountValue: Double = 0.0,
    @ColumnInfo(name = "discount_amount") val discountAmount: Double = 0.0,
    @ColumnInfo(name = "tax_amount") val taxAmount: Double = 0.0,
    @ColumnInfo(name = "total_amount") val totalAmount: Double = 0.0,
    @ColumnInfo(name = "status") val status: String = "completed",
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_version") val syncVersion: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["order_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["order_id"]), Index(value = ["product_id"])]
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "order_id") val orderId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "variant_id") val variantId: String? = null,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "variant_name") val variantName: String? = null,
    @ColumnInfo(name = "quantity") val quantity: Double,
    @ColumnInfo(name = "unit_price") val unitPrice: Double,
    @ColumnInfo(name = "cost_price") val costPrice: Double = 0.0,
    @ColumnInfo(name = "discount_type") val discountType: String? = null,
    @ColumnInfo(name = "discount_value") val discountValue: Double = 0.0,
    @ColumnInfo(name = "discount_amount") val discountAmount: Double = 0.0,
    @ColumnInfo(name = "tax_amount") val taxAmount: Double = 0.0,
    @ColumnInfo(name = "total_price") val totalPrice: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_version") val syncVersion: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
)

@Entity(
    tableName = "order_payments",
    foreignKeys = [
        ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["order_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["order_id"])]
)
data class OrderPaymentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "order_id") val orderId: String,
    @ColumnInfo(name = "method") val method: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "received_amount") val receivedAmount: Double? = null,
    @ColumnInfo(name = "change_amount") val changeAmount: Double = 0.0,
    @ColumnInfo(name = "reference_no") val referenceNo: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_version") val syncVersion: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
)

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["product_id"]), Index(value = ["created_at"]), Index(value = ["supplier_id"])]
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "variant_id") val variantId: String? = null,
    @ColumnInfo(name = "supplier_id") val supplierId: String? = null,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "quantity") val quantity: Double,
    @ColumnInfo(name = "quantity_before") val quantityBefore: Double,
    @ColumnInfo(name = "quantity_after") val quantityAfter: Double,
    @ColumnInfo(name = "unit_cost") val unitCost: Double? = null,
    @ColumnInfo(name = "reference_id") val referenceId: String? = null,
    @ColumnInfo(name = "reference_type") val referenceType: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_version") val syncVersion: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
)
