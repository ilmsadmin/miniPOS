package com.minipos.domain.model

data class Product(
    val id: String,
    val storeId: String,
    val categoryId: String? = null,
    val supplierId: String? = null,
    val sku: String,
    val barcode: String? = null,
    val name: String,
    val description: String? = null,
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val unit: String = "cái",
    val imagePath: String? = null,
    val additionalImages: List<String> = emptyList(),
    val minStock: Int = 0,
    val maxStock: Int? = null,
    val isActive: Boolean = true,
    val trackInventory: Boolean = true,
    val taxRate: Double = 0.0,
    val hasVariants: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class ProductVariant(
    val id: String,
    val storeId: String,
    val productId: String,
    val variantName: String,
    val sku: String,
    val barcode: String? = null,
    val costPrice: Double? = null,
    val sellingPrice: Double? = null,
    val attributes: String = "{}",
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class ProductWithStock(
    val product: Product,
    val currentStock: Double = 0.0,
    val categoryName: String? = null,
)
