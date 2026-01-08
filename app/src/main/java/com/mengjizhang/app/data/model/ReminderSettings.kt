package com.mengjizhang.app.data.model

/**
 * 提醒设置数据类
 */
data class ReminderSettings(
    val isEnabled: Boolean = false,           // 是否启用提醒
    val reminderHour: Int = 20,               // 提醒时间（小时）
    val reminderMinute: Int = 0,              // 提醒时间（分钟）
    val repeatDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),  // 重复日期（1=周一...7=周日）
    val reminderType: ReminderType = ReminderType.DAILY
)

/**
 * 提醒类型
 */
enum class ReminderType {
    DAILY,      // 每日提醒
    WEEKDAYS,   // 工作日提醒
    WEEKENDS,   // 周末提醒
    CUSTOM      // 自定义
}

/**
 * 星期几的显示名称
 */
val weekDayNames = mapOf(
    1 to "周一",
    2 to "周二",
    3 to "周三",
    4 to "周四",
    5 to "周五",
    6 to "周六",
    7 to "周日"
)
