package com.mengjizhang.app.data.model

/**
 * 分类数据
 */
data class Category(
    val id: Int,
    val name: String,
    val emoji: String,
    val isExpense: Boolean = true
)

/**
 * 预定义的支出分类
 */
val expenseCategories = listOf(
    Category(1, "餐饮", "🍔", true),
    Category(2, "交通", "🚇", true),
    Category(3, "购物", "🛒", true),
    Category(4, "娱乐", "🎮", true),
    Category(5, "医疗", "💊", true),
    Category(6, "教育", "📚", true),
    Category(7, "居家", "🏠", true),
    Category(8, "其他", "📌", true)
)

/**
 * 预定义的收入分类
 */
val incomeCategories = listOf(
    Category(101, "工资", "💰", false),
    Category(102, "奖金", "🎁", false),
    Category(103, "理财", "📈", false),
    Category(104, "红包", "🧧", false),
    Category(105, "其他", "📌", false)
)

/**
 * 获取分类
 */
fun getCategoryById(id: Int): Category? {
    return (expenseCategories + incomeCategories).find { it.id == id }
}
