package com.mengjizhang.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mengjizhang.app.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    /**
     * 获取指定月份的预算
     */
    @Query("SELECT * FROM budgets WHERE year = :year AND month = :month LIMIT 1")
    fun getBudgetByMonth(year: Int, month: Int): Flow<Budget?>

    /**
     * 获取指定月份的预算（同步版本）
     */
    @Query("SELECT * FROM budgets WHERE year = :year AND month = :month LIMIT 1")
    suspend fun getBudgetByMonthSync(year: Int, month: Int): Budget?

    /**
     * 插入预算
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    /**
     * 更新预算
     */
    @Update
    suspend fun updateBudget(budget: Budget)

    /**
     * 删除指定月份的预算
     */
    @Query("DELETE FROM budgets WHERE year = :year AND month = :month")
    suspend fun deleteBudget(year: Int, month: Int)

    /**
     * 获取所有预算记录
     */
    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC")
    fun getAllBudgets(): Flow<List<Budget>>

    /**
     * 获取最近一次设置的预算（用于自动填充新月份预算）
     */
    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC LIMIT 1")
    suspend fun getLatestBudget(): Budget?
}
