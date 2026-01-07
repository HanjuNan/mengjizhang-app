package com.mengjizhang.app.data.repository

import com.mengjizhang.app.data.local.BudgetDao
import com.mengjizhang.app.data.model.Budget
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {

    /**
     * 获取指定月份的预算
     */
    fun getBudgetByMonth(year: Int, month: Int): Flow<Budget?> {
        return budgetDao.getBudgetByMonth(year, month)
    }

    /**
     * 设置或更新预算
     */
    suspend fun setBudget(year: Int, month: Int, amount: Double) {
        val existingBudget = budgetDao.getBudgetByMonthSync(year, month)
        if (existingBudget != null) {
            budgetDao.updateBudget(
                existingBudget.copy(
                    monthlyBudget = amount,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            budgetDao.insertBudget(
                Budget(
                    monthlyBudget = amount,
                    year = year,
                    month = month
                )
            )
        }
    }

    /**
     * 删除预算
     */
    suspend fun deleteBudget(year: Int, month: Int) {
        budgetDao.deleteBudget(year, month)
    }

    /**
     * 获取最近设置的预算（用于推荐）
     */
    suspend fun getLatestBudget(): Budget? {
        return budgetDao.getLatestBudget()
    }

    /**
     * 获取所有预算记录
     */
    fun getAllBudgets(): Flow<List<Budget>> {
        return budgetDao.getAllBudgets()
    }
}
