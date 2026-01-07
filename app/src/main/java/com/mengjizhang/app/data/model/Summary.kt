package com.mengjizhang.app.data.model

/**
 * 日统计数据
 */
data class DailySummary(
    val date: Long,
    val totalIncome: Double,
    val totalExpense: Double,
    val records: List<Record>
)

/**
 * 月统计数据
 */
data class MonthlySummary(
    val year: Int,
    val month: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val categoryStats: List<CategoryStat>
)

/**
 * 分类统计
 */
data class CategoryStat(
    val categoryId: Int,
    val categoryName: String,
    val categoryEmoji: String,
    val amount: Double,
    val percentage: Float,
    val count: Int
)
