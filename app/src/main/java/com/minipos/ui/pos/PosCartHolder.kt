package com.minipos.ui.pos

import com.minipos.domain.model.*
import com.minipos.domain.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared POS state across all POS steps.
 * Using a Singleton holder so all POS step ViewModels share the same cart.
 */
@Singleton
class PosCartHolder @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val storeRepository: StoreRepository,
) {
    private val _cart = MutableStateFlow(Cart())
    val cart: StateFlow<Cart> = _cart

    // Cache for stock quantities: productId -> available quantity
    private val stockCache = mutableMapOf<String, Double>()

    // Cached store settings for tax
    private var cachedStoreSettings: StoreSettings? = null

    suspend fun refreshStoreSettings() {
        cachedStoreSettings = storeRepository.getStore()?.settings
    }

    private val _stockError = MutableStateFlow<String?>(null)
    val stockError: StateFlow<String?> = _stockError

    // Store the last created order ID for receipt printing/sharing
    var lastOrderId: String? = null
        private set

    fun setLastOrderId(orderId: String) {
        lastOrderId = orderId
    }

    fun clearStockError() {
        _stockError.value = null
    }

    suspend fun loadStock(storeId: String, products: List<Product>) {
        stockCache.clear()
        for (product in products) {
            if (product.trackInventory) {
                val stock = inventoryRepository.getStock(storeId, product.id)
                stockCache[product.id] = stock?.quantity ?: 0.0
            }
        }
    }

    fun getAvailableStock(productId: String): Double? = stockCache[productId]

    fun addItem(product: Product): Boolean {
        // If product has variants, this should not be called directly
        // Use addItemWithVariant instead
        return addItemWithVariant(product, null)
    }

    fun addItemWithVariant(product: Product, variant: ProductVariant?): Boolean {
        // Apply store default tax rate if product has no specific rate
        val effectiveProduct = applyStoreTaxRate(product)

        if (effectiveProduct.trackInventory) {
            val available = stockCache[effectiveProduct.id] ?: 0.0
            val currentInCart = _cart.value.items
                .filter { it.product.id == effectiveProduct.id }
                .sumOf { it.quantity }
            if (currentInCart + 1 > available) {
                _stockError.value = "\"${effectiveProduct.name}\" only has ${available.toLong()} ${effectiveProduct.unit} in stock"
                return false
            }
        }

        // Determine price: variant price overrides product price
        val unitPrice = variant?.sellingPrice ?: effectiveProduct.sellingPrice

        _cart.update { cart ->
            val existingIndex = cart.items.indexOfFirst {
                it.product.id == effectiveProduct.id && it.variant?.id == variant?.id
            }
            val newItems = if (existingIndex >= 0) {
                cart.items.toMutableList().apply {
                    val item = this[existingIndex]
                    this[existingIndex] = item.copy(quantity = item.quantity + 1)
                }
            } else {
                cart.items + CartItem(
                    product = effectiveProduct,
                    variant = variant,
                    quantity = 1.0,
                    unitPrice = unitPrice,
                    originalPrice = unitPrice,
                )
            }
            cart.copy(items = newItems)
        }
        return true
    }

    /**
     * If the store has tax enabled and a default tax rate,
     * apply it to products that don't have their own specific tax rate.
     */
    private fun applyStoreTaxRate(product: Product): Product {
        val settings = cachedStoreSettings ?: return product
        if (!settings.taxEnabled) return product
        if (product.taxRate > 0.0) return product // product has its own rate
        if (settings.defaultTaxRate <= 0.0) return product
        return product.copy(taxRate = settings.defaultTaxRate)
    }

    fun updateItemQuantity(index: Int, quantity: Double): Boolean {
        if (quantity <= 0) {
            _cart.update { cart ->
                if (index in cart.items.indices) {
                    cart.copy(items = cart.items.toMutableList().apply { removeAt(index) })
                } else {
                    cart
                }
            }
            return true
        }
        val cart = _cart.value
        if (index !in cart.items.indices) return false
        val item = cart.items[index]
        if (item.product.trackInventory) {
            val available = stockCache[item.product.id] ?: 0.0
            val otherQty = cart.items.filterIndexed { i, it -> i != index && it.product.id == item.product.id }.sumOf { it.quantity }
            if (otherQty + quantity > available) {
                _stockError.value = "\"${item.product.name}\" only has ${available.toLong()} ${item.product.unit} in stock"
                return false
            }
        }
        _cart.update { c ->
            if (index in c.items.indices) {
                val newItems = c.items.toMutableList().apply {
                    this[index] = this[index].copy(quantity = quantity)
                }
                c.copy(items = newItems)
            } else {
                c
            }
        }
        return true
    }

    fun updateItemPrice(index: Int, price: Double) {
        _cart.update { cart ->
            if (index in cart.items.indices) {
                val newItems = cart.items.toMutableList().apply {
                    this[index] = this[index].copy(unitPrice = price)
                }
                cart.copy(items = newItems)
            } else {
                cart
            }
        }
    }

    fun removeItem(index: Int) {
        _cart.update { cart ->
            if (index in cart.items.indices) {
                cart.copy(items = cart.items.toMutableList().apply { removeAt(index) })
            } else {
                cart
            }
        }
    }

    fun setCustomer(customer: Customer?) {
        _cart.update { it.copy(customer = customer) }
    }

    fun setOrderDiscount(discount: Discount?) {
        _cart.update { it.copy(orderDiscount = discount) }
    }

    fun clearCart() {
        _cart.value = Cart()
    }
}
