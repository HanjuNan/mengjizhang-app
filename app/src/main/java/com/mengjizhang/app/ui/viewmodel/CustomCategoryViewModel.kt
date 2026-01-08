package com.mengjizhang.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mengjizhang.app.data.local.AppDatabase
import com.mengjizhang.app.data.model.CustomCategory
import com.mengjizhang.app.data.repository.CustomCategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomCategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CustomCategoryRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CustomCategoryRepository(database.customCategoryDao())
    }

    // 支出分类
    val expenseCategories: StateFlow<List<CustomCategory>> = repository.getExpenseCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 收入分类
    val incomeCategories: StateFlow<List<CustomCategory>> = repository.getIncomeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当前编辑的分类
    private val _editingCategory = MutableStateFlow<CustomCategory?>(null)
    val editingCategory: StateFlow<CustomCategory?> = _editingCategory.asStateFlow()

    // 是否显示添加/编辑对话框
    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    // 当前选择的类型（支出/收入）
    private val _isExpenseType = MutableStateFlow(true)
    val isExpenseType: StateFlow<Boolean> = _isExpenseType.asStateFlow()

    fun setExpenseType(isExpense: Boolean) {
        _isExpenseType.value = isExpense
    }

    fun showAddDialog() {
        _editingCategory.value = null
        _showDialog.value = true
    }

    fun showEditDialog(category: CustomCategory) {
        _editingCategory.value = category
        _showDialog.value = true
    }

    fun hideDialog() {
        _showDialog.value = false
        _editingCategory.value = null
    }

    fun addCategory(name: String, emoji: String) {
        viewModelScope.launch {
            val category = CustomCategory(
                name = name,
                emoji = emoji,
                isExpense = _isExpenseType.value
            )
            repository.addCategory(category)
            hideDialog()
        }
    }

    fun updateCategory(id: Long, name: String, emoji: String) {
        viewModelScope.launch {
            val existing = repository.getCategoryById(id) ?: return@launch
            repository.updateCategory(existing.copy(name = name, emoji = emoji))
            hideDialog()
        }
    }

    fun deleteCategory(category: CustomCategory) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun toggleCategoryActive(category: CustomCategory) {
        viewModelScope.launch {
            repository.setCategoryActive(category.id, !category.isActive)
        }
    }
}
