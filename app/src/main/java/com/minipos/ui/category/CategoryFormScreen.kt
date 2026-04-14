package com.minipos.ui.category

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.Category
import com.minipos.ui.components.*
import com.minipos.ui.customer.FormDeleteButton
import com.minipos.ui.customer.FormSectionLabel
import com.minipos.ui.customer.FormTextArea
import com.minipos.ui.customer.FormTextField
import com.minipos.ui.customer.FormToggleRow

// ═══════════════════════════════════════════════════════════════
// CATEGORY FORM SCREEN — Full-screen create/edit matching HTML mock
// ═══════════════════════════════════════════════════════════════

private data class IconOption(
    val icon: ImageVector,
    val name: String,
)

private val AvailableIcons = listOf(
    IconOption(Icons.Rounded.LocalCafe, "local_cafe"),
    IconOption(Icons.Rounded.Restaurant, "restaurant"),
    IconOption(Icons.Rounded.Cookie, "cookie"),
    IconOption(Icons.Rounded.CleaningServices, "cleaning_services"),
    IconOption(Icons.Rounded.WaterDrop, "water_drop"),
    IconOption(Icons.Rounded.LocalGroceryStore, "local_grocery_store"),
    IconOption(Icons.Rounded.Icecream, "icecream"),
    IconOption(Icons.Rounded.LunchDining, "lunch_dining"),
    IconOption(Icons.Rounded.Liquor, "liquor"),
    IconOption(Icons.Rounded.Medication, "medication"),
    IconOption(Icons.Rounded.Pets, "pets"),
    IconOption(Icons.Rounded.Spa, "spa"),
    IconOption(Icons.Rounded.Toys, "toys"),
    IconOption(Icons.Rounded.Checkroom, "checkroom"),
    IconOption(Icons.Rounded.MoreHoriz, "more_horiz"),
)

private val AvailableColors = listOf(
    listOf(Color(0xFF4BB8F0), Color(0xFF2196F3)),
    listOf(Color(0xFFFF8A65), Color(0xFFF44336)),
    listOf(Color(0xFFFFD54F), Color(0xFFFFB300)),
    listOf(Color(0xFF81C784), Color(0xFF388E3C)),
    listOf(Color(0xFFCE93D8), Color(0xFF8E24AA)),
    listOf(Color(0xFF0E9AA0), Color(0xFF5AEDC5)),
    listOf(Color(0xFFFF6B6B), Color(0xFFEE5A24)),
    listOf(Color(0xFF90A4AE), Color(0xFF546E7A)),
)

private val ColorNames = listOf("blue", "red", "amber", "green", "purple", "teal", "orange", "grey")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormScreen(
    categoryId: String? = null,
    parentId: String? = null,
    onBack: () -> Unit,
    viewModel: CategoryListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val isEditing = categoryId != null

    // Form state
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("local_cafe") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var selectedParentId by remember { mutableStateOf(parentId) }
    var showIconGrid by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(true) }

    // Load category for edit (wait for categories to be loaded)
    LaunchedEffect(categoryId, state.categories) {
        if (categoryId != null && state.categories.isNotEmpty()) {
            val cat = state.categories.find { it.id == categoryId }
            cat?.let {
                // Set the editing context in ViewModel so save() works correctly
                viewModel.showEditForm(it)
                name = it.name
                description = it.description ?: ""
                selectedIcon = it.icon ?: "local_cafe"
                selectedColorIndex = ColorNames.indexOf(it.color ?: "blue").coerceAtLeast(0)
                selectedParentId = it.parentId
                isActive = it.isActive
            }
        }
    }

    val availableParents = state.categories.filter { it.parentId == null && it.id != categoryId }
    val currentIconOption = AvailableIcons.find { it.name == selectedIcon } ?: AvailableIcons.first()
    val currentColors = AvailableColors[selectedColorIndex % AvailableColors.size]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        // ─── Top Bar ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = AppColors.TextSecondary)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (isEditing) stringResource(R.string.edit_category_title) else stringResource(R.string.add_category_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                    .background(MiniPosGradients.primary())
                    .clickable(enabled = name.isNotBlank()) {
                        viewModel.save(
                            name,
                            description.ifBlank { null },
                            selectedIcon,
                            ColorNames[selectedColorIndex % ColorNames.size],
                            selectedParentId,
                        )
                        onBack()
                    }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.save), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        // ─── Body ───
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ─── Icon Picker ───
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
                        .background(Brush.linearGradient(currentColors))
                        .clickable { showIconGrid = !showIconGrid },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(currentIconOption.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    // Edit badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(AppColors.Surface)
                            .border(2.dp, AppColors.Border, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.catf_tap_change_icon), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextTertiary)

                // Icon grid (expandable)
                if (showIconGrid) {
                    Spacer(Modifier.height(12.dp))
                    IconGrid(
                        selectedIcon = selectedIcon,
                        onSelect = { iconName ->
                            selectedIcon = iconName
                            showIconGrid = false
                        },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ═══ CATEGORY INFO SECTION ═══
            FormSectionLabel(stringResource(R.string.catf_section_info))

            FormTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.category_name_required),
                placeholder = "e.g. Beverages",
                required = true,
            )

            FormTextArea(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.description_label),
                placeholder = "Short description of the category (optional)...",
            )

            Spacer(Modifier.height(20.dp))

            // ═══ COLOR SECTION ═══
            FormSectionLabel(stringResource(R.string.catf_color))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AvailableColors.forEachIndexed { index, colors ->
                    ColorOption(
                        colors = colors,
                        selected = index == selectedColorIndex,
                        onClick = { selectedColorIndex = index },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ═══ PARENT CATEGORY SECTION ═══
            FormSectionLabel(stringResource(R.string.catf_parent_category))

            // Parent category selector — MiniPosSelectBox
            val noParentLabel = stringResource(R.string.catf_no_parent)
            val parentTitle = stringResource(R.string.catf_parent_category)

            val parentItems = buildList {
                add(
                    SelectListItem(
                        id = "__none__",
                        name = noParentLabel,
                        icon = Icons.Rounded.FolderOff,
                        iconTint = AppColors.TextTertiary,
                    )
                )
                availableParents.forEach { cat ->
                    val catIcon = categoryIconFromName(cat.icon) ?: Icons.Rounded.FolderOpen
                    add(
                        SelectListItem(
                            id = cat.id,
                            name = cat.name,
                            icon = catIcon,
                            iconTint = AppColors.Primary,
                        )
                    )
                }
            }

            MiniPosSelectBox(
                label = "",
                title = parentTitle,
                items = parentItems,
                selectedId = selectedParentId ?: "__none__",
                placeholder = noParentLabel,
                onSelect = { item ->
                    selectedParentId = if (item.id == "__none__") null else item.id
                },
            )

            Spacer(Modifier.height(20.dp))

            // ═══ SUB-CATEGORIES PREVIEW (edit mode) ═══
            if (isEditing) {
                val subCategories = state.categories.filter { it.parentId == categoryId }
                if (subCategories.isNotEmpty()) {
                    FormSectionLabel("${stringResource(R.string.catf_subcategories)} (${subCategories.size})")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg)),
                    ) {
                        subCategories.forEachIndexed { index, sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(currentColors[0]),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(sub.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                                Text(
                                    "${state.productCountMap[sub.id] ?: 0}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextTertiary,
                                )
                            }
                            if (index < subCategories.lastIndex) {
                                HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            // Active toggle
            FormToggleRow(
                title = stringResource(R.string.catf_active),
                description = stringResource(R.string.catf_active_desc),
                checked = isActive,
                onCheckedChange = { isActive = it },
            )

            // Delete button (only in edit mode)
            if (isEditing) {
                Spacer(Modifier.height(12.dp))
                FormDeleteButton(
                    text = stringResource(R.string.catf_delete_category),
                    onClick = {
                        val cat = state.categories.find { it.id == categoryId }
                        cat?.let { viewModel.delete(it) }
                        onBack()
                    },
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ICON GRID
// ═══════════════════════════════════════════════════════════════

@Composable
private fun IconGrid(
    selectedIcon: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(12.dp),
    ) {
        // 5 columns grid
        val rows = AvailableIcons.chunked(5)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { option ->
                    val isSelected = option.name == selectedIcon
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                            .background(
                                if (isSelected) AppColors.Primary.copy(alpha = 0.12f) else AppColors.InputBackground
                            )
                            .border(
                                2.dp,
                                if (isSelected) AppColors.Primary else Color.Transparent,
                                RoundedCornerShape(MiniPosTokens.RadiusMd)
                            )
                            .clickable { onSelect(option.name) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            option.icon,
                            contentDescription = null,
                            tint = if (isSelected) AppColors.PrimaryLight else AppColors.TextSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                // Fill remaining cells if row is not complete
                repeat(5 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// COLOR OPTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ColorOption(
    colors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .background(Brush.linearGradient(colors))
            .then(
                if (selected) Modifier.border(3.dp, AppColors.TextPrimary, RoundedCornerShape(MiniPosTokens.RadiusMd))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
