package com.minipos.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.model.Category
import com.minipos.domain.model.Result
import com.minipos.domain.repository.CategoryRepository
import com.minipos.domain.repository.ProductRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryListState(
    val categories: List<Category> = emptyList(),
    val productCountMap: Map<String, Int> = emptyMap(), // categoryId -> product count
    val isLoading: Boolean = true,
    val showForm: Boolean = false,
    val editingCategory: Category? = null,
    val parentCategory: Category? = null,
    val searchQuery: String = "",
    val expandedCategoryIds: Set<String> = emptySet(),
) {
    val rootCategories: List<Category>
        get() {
            val filtered = if (searchQuery.isBlank()) categories
            else categories.filter { it.name.contains(searchQuery, ignoreCase = true) }
            return filtered.filter { it.parentId == null }
        }

    val childrenMap: Map<String?, List<Category>>
        get() {
            val filtered = if (searchQuery.isBlank()) categories
            else categories.filter { it.name.contains(searchQuery, ignoreCase = true) }
            return filtered.filter { it.parentId != null }.groupBy { it.parentId }
        }

    val totalRootCount: Int get() = categories.count { it.parentId == null }
    val totalSubCount: Int get() = categories.count { it.parentId != null }
    val totalProductCount: Int get() = productCountMap.values.sum()
}

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryListState())
    val state: StateFlow<CategoryListState> = _state

    private var storeId: String = ""

    init { observeData() }

    private fun observeData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            categoryRepository.observeCategories(storeId).collectLatest { categories ->
                val allProducts = productRepository.getAll(storeId)
                val countMap = mutableMapOf<String, Int>()
                categories.forEach { cat ->
                    val productsInCat = allProducts.filter { it.categoryId == cat.id }
                    var total = 0
                    productsInCat.forEach { product ->
                        // Each product counts as 1 (root variant) + number of child variants
                        val variantCount = productRepository.getVariantCount(product.id)
                        total += 1 + variantCount
                    }
                    countMap[cat.id] = total
                }
                _state.update {
                    it.copy(
                        categories = categories,
                        productCountMap = countMap,
                        isLoading = false,
                    )
                }
            }
        }
    }

    /** Force refresh (e.g. after save on same VM instance) */
    private fun loadData() {
        viewModelScope.launch {
            if (storeId.isBlank()) return@launch
            val categories = categoryRepository.getAll(storeId)
            val allProducts = productRepository.getAll(storeId)
            val countMap = mutableMapOf<String, Int>()
            categories.forEach { cat ->
                val productsInCat = allProducts.filter { it.categoryId == cat.id }
                var total = 0
                productsInCat.forEach { product ->
                    // Each product counts as 1 (root variant) + number of child variants
                    val variantCount = productRepository.getVariantCount(product.id)
                    total += 1 + variantCount
                }
                countMap[cat.id] = total
            }
            _state.update {
                it.copy(
                    categories = categories,
                    productCountMap = countMap,
                    isLoading = false,
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun toggleExpanded(categoryId: String) {
        _state.update { s ->
            val newSet = s.expandedCategoryIds.toMutableSet()
            if (newSet.contains(categoryId)) newSet.remove(categoryId) else newSet.add(categoryId)
            s.copy(expandedCategoryIds = newSet)
        }
    }

    fun showCreateForm() { _state.update { it.copy(showForm = true, editingCategory = null, parentCategory = null) } }
    fun showCreateSubcategory(parent: Category) { _state.update { it.copy(showForm = true, editingCategory = null, parentCategory = parent) } }
    fun showEditForm(category: Category) { _state.update { it.copy(showForm = true, editingCategory = category, parentCategory = null) } }
    fun dismissForm() { _state.update { it.copy(showForm = false, editingCategory = null, parentCategory = null) } }

    fun save(name: String, description: String?, icon: String?, color: String?, parentId: String?) {
        viewModelScope.launch {
            val editing = _state.value.editingCategory
            val result = if (editing != null) {
                categoryRepository.update(editing.copy(
                    name = name,
                    description = description,
                    icon = icon,
                    color = color,
                    parentId = parentId ?: editing.parentId,
                    updatedAt = System.currentTimeMillis(),
                ))
            } else {
                categoryRepository.create(storeId, name, description, parentId, icon, color)
            }
            if (result is Result.Success) {
                dismissForm()
                loadData()
            }
        }
    }

    fun delete(category: Category) {
        viewModelScope.launch {
            categoryRepository.delete(category.id)
            loadData()
        }
    }
}
