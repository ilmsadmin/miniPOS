package com.minipos.core.di

import android.content.Context
import androidx.room.Room
import com.minipos.core.constants.AppConstants
import com.minipos.core.utils.UuidGenerator
import com.minipos.data.database.MiniPosDatabase
import com.minipos.data.database.dao.*
import com.minipos.data.preferences.AppPreferences
import com.minipos.data.repository.*
import com.minipos.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MiniPosDatabase {
        return Room.databaseBuilder(
            context,
            MiniPosDatabase::class.java,
            AppConstants.DB_NAME
        )
            .addMigrations(MiniPosDatabase.MIGRATION_1_2, MiniPosDatabase.MIGRATION_2_3, MiniPosDatabase.MIGRATION_3_4, MiniPosDatabase.MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        val prefs = AppPreferences(context)
        // Initialize device ID if not set
        runBlocking {
            if (prefs.getDeviceIdSync().isEmpty()) {
                prefs.setDeviceId(UuidGenerator.generate())
            }
        }
        return prefs
    }

    // DAOs
    @Provides fun provideStoreDao(db: MiniPosDatabase): StoreDao = db.storeDao()
    @Provides fun provideUserDao(db: MiniPosDatabase): UserDao = db.userDao()
    @Provides fun provideCategoryDao(db: MiniPosDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideSupplierDao(db: MiniPosDatabase): SupplierDao = db.supplierDao()
    @Provides fun provideProductDao(db: MiniPosDatabase): ProductDao = db.productDao()
    @Provides fun provideProductVariantDao(db: MiniPosDatabase): ProductVariantDao = db.productVariantDao()
    @Provides fun provideCustomerDao(db: MiniPosDatabase): CustomerDao = db.customerDao()
    @Provides fun provideInventoryDao(db: MiniPosDatabase): InventoryDao = db.inventoryDao()
    @Provides fun provideOrderDao(db: MiniPosDatabase): OrderDao = db.orderDao()

    // Repositories
    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        storeDao: StoreDao, userDao: UserDao,
        inventoryDao: InventoryDao, prefs: AppPreferences
    ): AuthRepository = AuthRepositoryImpl(context, storeDao, userDao, inventoryDao, prefs)

    @Provides
    @Singleton
    fun provideStoreRepository(
        @ApplicationContext context: Context,
        storeDao: StoreDao
    ): StoreRepository = StoreRepositoryImpl(context, storeDao)

    @Provides
    @Singleton
    fun provideUserRepository(
        @ApplicationContext context: Context,
        userDao: UserDao, prefs: AppPreferences
    ): UserRepository =
        UserRepositoryImpl(context, userDao, prefs)

    @Provides
    @Singleton
    fun provideCategoryRepository(
        @ApplicationContext context: Context,
        categoryDao: CategoryDao, prefs: AppPreferences
    ): CategoryRepository =
        CategoryRepositoryImpl(context, categoryDao, prefs)

    @Provides
    @Singleton
    fun provideProductRepository(
        @ApplicationContext context: Context,
        productDao: ProductDao, productVariantDao: ProductVariantDao, inventoryDao: InventoryDao, prefs: AppPreferences
    ): ProductRepository =
        ProductRepositoryImpl(context, productDao, productVariantDao, inventoryDao, prefs)

    @Provides
    @Singleton
    fun provideSupplierRepository(
        @ApplicationContext context: Context,
        supplierDao: SupplierDao, prefs: AppPreferences
    ): SupplierRepository =
        SupplierRepositoryImpl(context, supplierDao, prefs)

    @Provides
    @Singleton
    fun provideCustomerRepository(
        @ApplicationContext context: Context,
        customerDao: CustomerDao, prefs: AppPreferences
    ): CustomerRepository =
        CustomerRepositoryImpl(context, customerDao, prefs)

    @Provides
    @Singleton
    fun provideOrderRepository(
        @ApplicationContext context: Context,
        orderDao: OrderDao, inventoryDao: InventoryDao, customerDao: CustomerDao, prefs: AppPreferences
    ): OrderRepository =
        OrderRepositoryImpl(context, orderDao, inventoryDao, customerDao, prefs)

    @Provides
    @Singleton
    fun provideInventoryRepository(
        @ApplicationContext context: Context,
        inventoryDao: InventoryDao, orderDao: OrderDao, prefs: AppPreferences
    ): InventoryRepository =
        InventoryRepositoryImpl(context, inventoryDao, orderDao, prefs)
}
