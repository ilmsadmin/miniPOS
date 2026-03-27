package com.minipos.ui.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    onBack: () -> Unit,
    viewModel: CategoryListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.showForm) {
        CategoryFormDialog(
            editing = state.editingCategory,
            parentCategory = state.parentCategory,
            allCategories = state.categories,
            onSave = { name, description, icon, color, parentId ->
                viewModel.save(name, description, icon, color, parentId)
            },
            onDismiss = { viewModel.dismissForm() },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateForm() }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_category_cd))
                    }
                },
            )
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColors.TextTertiary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.no_categories), style = MaterialTheme.typography.titleMedium, color = AppColors.TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.category_empty_desc), style = MaterialTheme.typography.bodySmall, color = AppColors.TextTertiary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.showCreateForm() }, shape = RoundedCornerShape(8.dp)) {
                        Text(stringResource(R.string.create_first_category))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Build parent -> children map
                val rootCategories = state.categories.filter { it.parentId == null }
                val childrenMap = state.categories.filter { it.parentId != null }.groupBy { it.parentId }

                rootCategories.forEach { parent ->
                    item(key = parent.id) {
                        CategoryItem(
                            category = parent,
                            isSubcategory = false,
                            onClick = { viewModel.showEditForm(parent) },
                            onDelete = { viewModel.delete(parent) },
                            onAddSubcategory = { viewModel.showCreateSubcategory(parent) },
                        )
                    }
                    // Show children indented
                    val children = childrenMap[parent.id] ?: emptyList()
                    children.forEach { child ->
                        item(key = child.id) {
                            CategoryItem(
                                category = child,
                                isSubcategory = true,
                                onClick = { viewModel.showEditForm(child) },
                                onDelete = { viewModel.delete(child) },
                                onAddSubcategory = null,
                            )
                        }
                    }
                }

                // Show orphan categories (parentId set but parent not found)
                val orphans = state.categories.filter { cat ->
                    cat.parentId != null && rootCategories.none { it.id == cat.parentId }
                }
                orphans.forEach { orphan ->
                    item(key = orphan.id) {
                        CategoryItem(
                            category = orphan,
                            isSubcategory = false,
                            onClick = { viewModel.showEditForm(orphan) },
                            onDelete = { viewModel.delete(orphan) },
                            onAddSubcategory = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    isSubcategory: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAddSubcategory: (() -> Unit)?,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_category_title)) },
            text = { Text(stringResource(R.string.delete_category_confirm, category.name)) },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text(stringResource(R.string.delete_label), color = AppColors.Error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel_btn_label)) } },
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isSubcategory) 32.dp else 0.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSubcategory) 0.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSubcategory) AppColors.SurfaceVariant else MaterialTheme.colorScheme.surface
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSubcategory) {
                Icon(
                    Icons.Default.SubdirectoryArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.TextTertiary,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                category.icon ?: if (isSubcategory) "�" else "�📦",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                if (!category.description.isNullOrBlank()) {
                    Text(category.description, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
            }
            if (onAddSubcategory != null) {
                IconButton(onClick = onAddSubcategory) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Thêm danh mục con", tint = AppColors.Primary, modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_label), tint = AppColors.TextTertiary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFormDialog(
    editing: Category?,
    parentCategory: Category?,
    allCategories: List<Category>,
    onSave: (String, String?, String?, String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var icon by remember { mutableStateOf(editing?.icon ?: "") }
    var selectedParentId by remember { mutableStateOf(editing?.parentId ?: parentCategory?.id) }

    // Only root categories can be parents (no deep nesting)
    val availableParents = allCategories.filter { it.parentId == null && it.id != editing?.id }

    val title = when {
        editing != null -> stringResource(R.string.edit_category_title)
        parentCategory != null -> "Thêm danh mục con"
        else -> stringResource(R.string.add_category_title)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (parentCategory != null && editing == null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.PrimaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Danh mục cha: ${parentCategory.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.Primary,
                            )
                        }
                    }
                }

                // Parent category dropdown (only when editing or creating without preset parent)
                if (editing != null || parentCategory == null) {
                    var expanded by remember { mutableStateOf(false) }
                    val parentName = availableParents.find { it.id == selectedParentId }?.name ?: "Không có (danh mục gốc)"

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                    ) {
                        OutlinedTextField(
                            value = parentName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Danh mục cha") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Không có (danh mục gốc)", color = AppColors.TextSecondary) },
                                onClick = { selectedParentId = null; expanded = false },
                            )
                            availableParents.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.icon ?: "📦"} ${cat.name}") },
                                    onClick = { selectedParentId = cat.id; expanded = false },
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.category_name_required)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(8.dp),
                )
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text(stringResource(R.string.category_icon_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(
                        name,
                        description.ifBlank { null },
                        icon.ifBlank { null },
                        null,
                        selectedParentId,
                    )
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
            ) { Text(stringResource(R.string.save_btn)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_btn_label)) } },
    )
}
