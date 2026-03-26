package com.minipos.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.domain.model.Category
import com.minipos.domain.model.Result
import com.minipos.domain.repository.CategoryRepository
import com.minipos.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryListState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val showForm: Boolean = false,
    val editingCategory: Category? = null,
)

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryListState())
    val state: StateFlow<CategoryListState> = _state

    private var storeId: String = ""

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val store = storeRepository.getStore() ?: return@launch
            storeId = store.id
            val categories = categoryRepository.getAll(storeId)
            _state.update { it.copy(categories = categories, isLoading = false) }
        }
    }

    fun showCreateForm() { _state.update { it.copy(showForm = true, editingCategory = null) } }
    fun showEditForm(category: Category) { _state.update { it.copy(showForm = true, editingCategory = category) } }
    fun dismissForm() { _state.update { it.copy(showForm = false, editingCategory = null) } }

    fun save(name: String, description: String?, icon: String?, color: String?) {
        viewModelScope.launch {
            val editing = _state.value.editingCategory
            val result = if (editing != null) {
                categoryRepository.update(editing.copy(
                    name = name,
                    description = description,
                    icon = icon,
                    color = color,
                    updatedAt = System.currentTimeMillis(),
                ))
            } else {
                categoryRepository.create(storeId, name, description, null, icon, color)
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
