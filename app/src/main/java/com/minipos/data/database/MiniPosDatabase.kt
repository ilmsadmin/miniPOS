package com.minipos.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.minipos.data.database.dao.*
import com.minipos.data.database.entity.*

@Database(
    entities = [
        StoreEntity::class,
        UserEntity::class,
        CategoryEntity::class,
        SupplierEntity::class,
        ProductEntity::class,
        ProductVariantEntity::class,
        CustomerEntity::class,
        InventoryEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        OrderPaymentEntity::class,
        StockMovementEntity::class,
    ],
    version = 4,
    exportSchema = true
)
abstract class MiniPosDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun supplierDao(): SupplierDao
    abstract fun productDao(): ProductDao
    abstract fun productVariantDao(): ProductVariantDao
    abstract fun customerDao(): CustomerDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun orderDao(): OrderDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add supplier_id column to stock_movements table
                db.execSQL("ALTER TABLE stock_movements ADD COLUMN supplier_id TEXT DEFAULT NULL")
                // Create index for supplier_id
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_supplier_id ON stock_movements (supplier_id)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add additional_images column to products table (pipe-separated paths)
                db.execSQL("ALTER TABLE products ADD COLUMN additional_images TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `product_variants` (
                        `id` TEXT NOT NULL,
                        `store_id` TEXT NOT NULL,
                        `product_id` TEXT NOT NULL,
                        `variant_name` TEXT NOT NULL,
                        `sku` TEXT NOT NULL,
                        `barcode` TEXT DEFAULT NULL,
                        `cost_price` REAL DEFAULT NULL,
                        `selling_price` REAL DEFAULT NULL,
                        `attributes` TEXT NOT NULL DEFAULT '{}',
                        `is_active` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL DEFAULT 0,
                        `deleted_at` INTEGER DEFAULT NULL,
                        `sync_version` INTEGER NOT NULL DEFAULT 0,
                        `device_id` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`store_id`) REFERENCES `stores`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_variants_store_id` ON `product_variants` (`store_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_variants_product_id` ON `product_variants` (`product_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_variants_barcode` ON `product_variants` (`barcode`)")
            }
        }
    }
}
