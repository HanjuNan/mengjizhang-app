package com.mengjizhang.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 预算实体
 * 存储用户设定的月度预算
 */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val monthlyBudget: Double,      // 月度预算金额
    val year: Int,                  // 年份
    val month: Int,                 // 月份
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 预算与实际支出的状态
 */
data class BudgetStatus(
    val budget: Double,             // 预算金额
    val spent: Double,              // 已支出金额
    val remaining: Double,          // 剩余金额
    val percentage: Float,          // 已使用百分比 (0-1)
    val isOverBudget: Boolean       // 是否超预算
) {
    companion object {
        val Empty = BudgetStatus(
            budget = 0.0,
            spent = 0.0,
            remaining = 0.0,
            percentage = 0f,
            isOverBudget = false
        )
    }
}
