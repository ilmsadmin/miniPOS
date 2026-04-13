package com.minipos.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps a Material icon name (stored in DB) to an actual [ImageVector].
 * Returns null if the name is not recognized.
 */
fun categoryIconFromName(iconName: String?): ImageVector? {
    if (iconName.isNullOrBlank()) return null
    return CategoryIconMap[iconName]
}

private val CategoryIconMap: Map<String, ImageVector> = mapOf(
    "local_cafe" to Icons.Rounded.LocalCafe,
    "restaurant" to Icons.Rounded.Restaurant,
    "cookie" to Icons.Rounded.Cookie,
    "cleaning_services" to Icons.Rounded.CleaningServices,
    "water_drop" to Icons.Rounded.WaterDrop,
    "local_grocery_store" to Icons.Rounded.LocalGroceryStore,
    "icecream" to Icons.Rounded.Icecream,
    "lunch_dining" to Icons.Rounded.LunchDining,
    "liquor" to Icons.Rounded.Liquor,
    "medication" to Icons.Rounded.Medication,
    "pets" to Icons.Rounded.Pets,
    "spa" to Icons.Rounded.Spa,
    "toys" to Icons.Rounded.Toys,
    "checkroom" to Icons.Rounded.Checkroom,
    "more_horiz" to Icons.Rounded.MoreHoriz,
    // Additional common icons
    "shopping_cart" to Icons.Rounded.ShoppingCart,
    "phone_android" to Icons.Rounded.PhoneAndroid,
    "computer" to Icons.Rounded.Computer,
    "devices" to Icons.Rounded.Devices,
    "local_shipping" to Icons.Rounded.LocalShipping,
    "category" to Icons.Rounded.Category,
)
