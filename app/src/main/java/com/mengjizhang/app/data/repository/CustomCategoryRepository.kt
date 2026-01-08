package com.mengjizhang.app.data.repository

import com.mengjizhang.app.data.local.CustomCategoryDao
import com.mengjizhang.app.data.model.CustomCategory
import kotlinx.coroutines.flow.Flow

class CustomCategoryRepository(private val dao: CustomCategoryDao) {

    fun getExpenseCategories(): Flow<List<CustomCategory>> = dao.getCategoriesByType(true)

    fun getIncomeCategories(): Flow<List<CustomCategory>> = dao.getCategoriesByType(false)

    fun getAllActiveCategories(): Flow<List<CustomCategory>> = dao.getAllActiveCategories()

    fun getAllCategories(): Flow<List<CustomCategory>> = dao.getAllCategories()

    suspend fun getCategoryById(id: Long): CustomCategory? = dao.getCategoryById(id)

    suspend fun addCategory(category: CustomCategory): Long {
        val maxOrder = dao.getMaxSortOrder(category.isExpense) ?: 0
        return dao.insertCategory(category.copy(sortOrder = maxOrder + 1))
    }

    suspend fun updateCategory(category: CustomCategory) = dao.updateCategory(category)

    suspend fun deleteCategory(category: CustomCategory) = dao.deleteCategory(category)

    suspend fun setCategoryActive(id: Long, isActive: Boolean) = dao.setCategoryActive(id, isActive)

    suspend fun updateSortOrder(id: Long, sortOrder: Int) = dao.updateSortOrder(id, sortOrder)
}
