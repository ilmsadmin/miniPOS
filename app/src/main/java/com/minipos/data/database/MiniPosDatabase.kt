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
        CustomerEntity::class,
        InventoryEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        OrderPaymentEntity::class,
        StockMovementEntity::class,
    ],
    version = 3,
    exportSchema = true
)
abstract class MiniPosDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun supplierDao(): SupplierDao
    abstract fun productDao(): ProductDao
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
    }
}
