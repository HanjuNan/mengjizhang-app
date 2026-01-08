package com.mengjizhang.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 自定义分类实体
 */
@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val isExpense: Boolean = true,  // true=支出分类, false=收入分类
    val sortOrder: Int = 0,         // 排序顺序
    val isActive: Boolean = true,   // 是否启用
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 转换为 Category 对象（用于 AddScreen 显示）
     * 使用 10000+ 的 ID 范围避免与预定义分类冲突
     */
    fun toCategory(): Category = Category(
        id = (10000 + id).toInt(),
        name = name,
        emoji = emoji,
        isExpense = isExpense
    )
}

/**
 * 可用的 Emoji 列表（供用户选择）
 */
val availableEmojis = listOf(
    // 食物饮料
    "🍔", "🍕", "🍜", "🍱", "🍰", "🍦", "☕", "🍺", "🥗", "🍳",
    // 交通出行
    "🚗", "🚇", "🚌", "🚕", "✈️", "🚲", "⛽", "🚀", "🛵", "🚢",
    // 购物消费
    "🛒", "👗", "👟", "💄", "🎁", "💍", "👜", "🧴", "📱", "💻",
    // 娱乐休闲
    "🎮", "🎬", "🎵", "📚", "🎨", "🏃", "⚽", "🎯", "🎪", "🎭",
    // 生活服务
    "🏠", "💡", "📞", "🔧", "🧹", "🌡️", "💊", "🏥", "✂️", "🧺",
    // 教育学习
    "📖", "✏️", "🎓", "💼", "📝", "🔬", "🌐", "📊", "🗂️", "📋",
    // 金融理财
    "💰", "💵", "💳", "🏦", "📈", "📉", "💹", "🧧", "💎", "🪙",
    // 其他
    "❤️", "⭐", "🔥", "✨", "🎉", "🌈", "🌸", "🍀", "🐱", "🐶"
)
