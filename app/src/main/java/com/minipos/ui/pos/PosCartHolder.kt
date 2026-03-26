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
) {
    private val _cart = MutableStateFlow(Cart())
    val cart: StateFlow<Cart> = _cart

    // Cache for stock quantities: productId -> available quantity
    private val stockCache = mutableMapOf<String, Double>()

    private val _stockError = MutableStateFlow<String?>(null)
    val stockError: StateFlow<String?> = _stockError

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
        if (product.trackInventory) {
            val available = stockCache[product.id] ?: 0.0
            val currentInCart = _cart.value.items
                .filter { it.product.id == product.id }
                .sumOf { it.quantity }
            if (currentInCart + 1 > available) {
                _stockError.value = "\"${product.name}\" chỉ còn ${available.toLong()} ${product.unit} trong kho"
                return false
            }
        }
        _cart.update { cart ->
            val existingIndex = cart.items.indexOfFirst { it.product.id == product.id && it.variant == null }
            val newItems = if (existingIndex >= 0) {
                cart.items.toMutableList().apply {
                    val item = this[existingIndex]
                    this[existingIndex] = item.copy(quantity = item.quantity + 1)
                }
            } else {
                cart.items + CartItem(
                    product = product,
                    quantity = 1.0,
                    unitPrice = product.sellingPrice,
                    originalPrice = product.sellingPrice,
                )
            }
            cart.copy(items = newItems)
        }
        return true
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
                _stockError.value = "\"${item.product.name}\" chỉ còn ${available.toLong()} ${item.product.unit} trong kho"
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
