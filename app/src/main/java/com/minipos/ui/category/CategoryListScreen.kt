package com.minipos.ui.category

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.Category
import com.minipos.ui.components.*

// ─── Category icon mapping helper ───
private data class CategoryIconStyle(
    val icon: ImageVector,
    val gradientStart: Color,
    val gradientEnd: Color,
    val dotColor: Color,
)

@Composable
private fun getCategoryIconStyle(category: Category): CategoryIconStyle {
    val name = category.name.lowercase()
    val color = category.color?.lowercase() ?: ""
    return when {
        name.contains("nước") || name.contains("drink") || name.contains("uống") || color == "drinks" ->
            CategoryIconStyle(Icons.Rounded.LocalCafe, Color(0xFF4BB8F0), Color(0xFF2196F3), Color(0xFF4BB8F0))
        name.contains("thực phẩm") || name.contains("food") || name.contains("ăn") || color == "food" ->
            CategoryIconStyle(Icons.Rounded.Restaurant, Color(0xFFFF8A65), Color(0xFFF44336), Color(0xFFFF8A65))
        name.contains("bánh") || name.contains("kẹo") || name.contains("snack") || color == "snacks" ->
            CategoryIconStyle(Icons.Rounded.Cookie, Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFFD54F))
        name.contains("gia dụng") || name.contains("household") || name.contains("dụng") || color == "household" ->
            CategoryIconStyle(Icons.Rounded.CleaningServices, Color(0xFF81C784), Color(0xFF388E3C), Color(0xFF81C784))
        name.contains("sữa") || name.contains("dairy") || color == "dairy" ->
            CategoryIconStyle(Icons.Rounded.WaterDrop, Color(0xFFCE93D8), Color(0xFF8E24AA), Color(0xFFCE93D8))
        else ->
            CategoryIconStyle(Icons.Rounded.MoreHoriz, Color(0xFF90A4AE), Color(0xFF546E7A), Color(0xFF90A4AE))
    }
}

@Composable
fun CategoryListScreen(
    onBack: () -> Unit,
    onNavigateToForm: (String?, String?) -> Unit = { _, _ -> },
    viewModel: CategoryListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding(),
    ) {
        // ─── Top Bar ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = AppColors.TextSecondary,
                )
            }
            Spacer(Modifier.width(4.dp))
            // Title
            Text(
                text = stringResource(R.string.category_title_screen),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            // Add button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MiniPosGradients.primary())
                    .clickable { onNavigateToForm(null, null) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.add_category_cd),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else if (state.categories.isEmpty()) {
            // ─── Empty state ───
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(AppColors.SurfaceElevated),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Category,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = AppColors.TextTertiary,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.no_categories),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.category_empty_desc),
                        fontSize = 12.sp,
                        color = AppColors.TextTertiary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MiniPosGradientButton(
                        text = stringResource(R.string.create_first_category),
                        onClick = { onNavigateToForm(null, null) },
                        modifier = Modifier.width(200.dp),
                        height = 44.dp,
                    )
                }
            }
        } else {
            // ─── Content ───
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            ) {
                // ─── Stat bar ───
                item(key = "stat_bar") {
                    StatBar(
                        rootCount = state.totalRootCount,
                        subCount = state.totalSubCount,
                        productCount = state.totalProductCount,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ─── Search bar ───
                item(key = "search_bar") {
                    MiniPosSearchBar(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = stringResource(R.string.category_search_hint),
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ─── Category tree ───
                val rootCategories = state.rootCategories
                val childrenMap = state.childrenMap

                rootCategories.forEach { parent ->
                    val children = childrenMap[parent.id] ?: emptyList()
                    val isExpanded = state.expandedCategoryIds.contains(parent.id)
                    val productCount = (state.productCountMap[parent.id] ?: 0) +
                        children.sumOf { state.productCountMap[it.id] ?: 0 }
                    val subCount = children.size

                    item(key = "parent_${parent.id}") {
                        ParentCategoryItem(
                            category = parent,
                            productCount = productCount,
                            subCategoryCount = subCount,
                            isExpanded = isExpanded,
                            onToggle = { viewModel.toggleExpanded(parent.id) },
                            onEdit = { onNavigateToForm(parent.id, null) },
                            onDelete = { viewModel.delete(parent) },
                        )
                    }

                    // Sub-categories with animation
                    if (isExpanded && children.isNotEmpty()) {
                        item(key = "children_${parent.id}") {
                            val borderColor = AppColors.BorderLight
                            Column(
                                modifier = Modifier
                                    .padding(start = 28.dp, top = 4.dp)
                                    .drawBehind {
                                        // Vertical tree line
                                        drawLine(
                                            color = borderColor,
                                            start = Offset(-16.dp.toPx(), 0f),
                                            end = Offset(-16.dp.toPx(), size.height),
                                            strokeWidth = 1.dp.toPx(),
                                        )
                                    },
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                children.forEach { child ->
                                    SubCategoryItem(
                                        category = child,
                                        parentCategory = parent,
                                        productCount = state.productCountMap[child.id] ?: 0,
                                        onEdit = { onNavigateToForm(child.id, parent.id) },
                                        onDelete = { viewModel.delete(child) },
                                    )
                                }
                            }
                        }
                    }

                    item(key = "spacer_${parent.id}") {
                        Spacer(Modifier.height(4.dp))
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════
// STAT BAR
// ═══════════════════════════════════════

@Composable
private fun StatBar(
    rootCount: Int,
    subCount: Int,
    productCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatItem(
            value = rootCount.toString(),
            label = stringResource(R.string.category_stat_categories),
            modifier = Modifier.weight(1f),
        )
        StatItem(
            value = subCount.toString(),
            label = stringResource(R.string.category_stat_subcategories),
            modifier = Modifier.weight(1f),
        )
        StatItem(
            value = productCount.toString(),
            label = stringResource(R.string.category_stat_products),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            style = LocalTextStyle.current.copy(
                brush = MiniPosGradients.primary(),
            ),
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextTertiary,
        )
    }
}

// ═══════════════════════════════════════
// PARENT CATEGORY ITEM
// ═══════════════════════════════════════

@Composable
private fun ParentCategoryItem(
    category: Category,
    productCount: Int,
    subCategoryCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
            containerColor = AppColors.Surface,
            title = { Text(stringResource(R.string.delete_category_title)) },
            text = { Text(stringResource(R.string.delete_category_confirm, category.name)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.delete_label), color = AppColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel_btn_label))
                }
            },
        )
    }

    val style = getCategoryIconStyle(category)
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "arrow_rotation",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon with gradient background
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                .background(
                    Brush.linearGradient(listOf(style.gradientStart, style.gradientEnd))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                style.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        // Category name & count
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (subCategoryCount > 0)
                    stringResource(R.string.category_product_sub_count, productCount, subCategoryCount)
                else
                    stringResource(R.string.category_product_count_simple, productCount),
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
        }

        // Action buttons
        // Edit button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onEdit() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Edit,
                contentDescription = stringResource(R.string.edit_category_action),
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(4.dp))

        // Delete button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { showDeleteConfirm = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.delete_category_action),
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(4.dp))

        // Arrow
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = AppColors.TextTertiary,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(arrowRotation),
            )
        }
    }
}

// ═══════════════════════════════════════
// SUB-CATEGORY ITEM
// ═══════════════════════════════════════

@Composable
private fun SubCategoryItem(
    category: Category,
    parentCategory: Category,
    productCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
            containerColor = AppColors.Surface,
            title = { Text(stringResource(R.string.delete_category_title)) },
            text = { Text(stringResource(R.string.delete_category_confirm, category.name)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.delete_label), color = AppColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel_btn_label))
                }
            },
        )
    }

    val parentStyle = getCategoryIconStyle(parentCategory)
    val borderColor = AppColors.BorderLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Horizontal tree connector line
                drawLine(
                    color = borderColor,
                    start = Offset(-16.dp.toPx(), size.height / 2),
                    end = Offset(-4.dp.toPx(), size.height / 2),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .background(AppColors.InputBackground)
            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(MiniPosTokens.RadiusMd))
            .clickable { onEdit() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dot with parent color
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(parentStyle.dotColor),
        )

        Spacer(Modifier.width(8.dp))

        // Name
        Text(
            text = category.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // Product count
        Text(
            text = productCount.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextTertiary,
        )
    }
}

// CategoryFormDialog removed — now uses full-screen CategoryFormScreen via navigation
