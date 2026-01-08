package com.mengjizhang.app.data.local

import androidx.room.*
import com.mengjizhang.app.data.model.CustomCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCategoryDao {

    @Query("SELECT * FROM custom_categories WHERE isExpense = :isExpense AND isActive = 1 ORDER BY sortOrder ASC, createdAt ASC")
    fun getCategoriesByType(isExpense: Boolean): Flow<List<CustomCategory>>

    @Query("SELECT * FROM custom_categories WHERE isActive = 1 ORDER BY isExpense DESC, sortOrder ASC")
    fun getAllActiveCategories(): Flow<List<CustomCategory>>

    @Query("SELECT * FROM custom_categories ORDER BY isExpense DESC, sortOrder ASC")
    fun getAllCategories(): Flow<List<CustomCategory>>

    @Query("SELECT * FROM custom_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CustomCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CustomCategory): Long

    @Update
    suspend fun updateCategory(category: CustomCategory)

    @Delete
    suspend fun deleteCategory(category: CustomCategory)

    @Query("UPDATE custom_categories SET isActive = :isActive WHERE id = :id")
    suspend fun setCategoryActive(id: Long, isActive: Boolean)

    @Query("UPDATE custom_categories SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("SELECT MAX(sortOrder) FROM custom_categories WHERE isExpense = :isExpense")
    suspend fun getMaxSortOrder(isExpense: Boolean): Int?
}
