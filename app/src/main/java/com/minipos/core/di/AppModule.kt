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
            .addMigrations(MiniPosDatabase.MIGRATION_1_2)
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
    @Provides fun provideCustomerDao(db: MiniPosDatabase): CustomerDao = db.customerDao()
    @Provides fun provideInventoryDao(db: MiniPosDatabase): InventoryDao = db.inventoryDao()
    @Provides fun provideOrderDao(db: MiniPosDatabase): OrderDao = db.orderDao()

    // Repositories
    @Provides
    @Singleton
    fun provideAuthRepository(
        storeDao: StoreDao, userDao: UserDao,
        inventoryDao: InventoryDao, prefs: AppPreferences
    ): AuthRepository = AuthRepositoryImpl(storeDao, userDao, inventoryDao, prefs)

    @Provides
    @Singleton
    fun provideStoreRepository(storeDao: StoreDao): StoreRepository = StoreRepositoryImpl(storeDao)

    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao, prefs: AppPreferences): UserRepository =
        UserRepositoryImpl(userDao, prefs)

    @Provides
    @Singleton
    fun provideCategoryRepository(categoryDao: CategoryDao, prefs: AppPreferences): CategoryRepository =
        CategoryRepositoryImpl(categoryDao, prefs)

    @Provides
    @Singleton
    fun provideProductRepository(productDao: ProductDao, inventoryDao: InventoryDao, prefs: AppPreferences): ProductRepository =
        ProductRepositoryImpl(productDao, inventoryDao, prefs)

    @Provides
    @Singleton
    fun provideSupplierRepository(supplierDao: SupplierDao, prefs: AppPreferences): SupplierRepository =
        SupplierRepositoryImpl(supplierDao, prefs)

    @Provides
    @Singleton
    fun provideCustomerRepository(customerDao: CustomerDao, prefs: AppPreferences): CustomerRepository =
        CustomerRepositoryImpl(customerDao, prefs)

    @Provides
    @Singleton
    fun provideOrderRepository(orderDao: OrderDao, inventoryDao: InventoryDao, customerDao: CustomerDao, prefs: AppPreferences): OrderRepository =
        OrderRepositoryImpl(orderDao, inventoryDao, customerDao, prefs)

    @Provides
    @Singleton
    fun provideInventoryRepository(inventoryDao: InventoryDao, orderDao: OrderDao, prefs: AppPreferences): InventoryRepository =
        InventoryRepositoryImpl(inventoryDao, orderDao, prefs)
}
