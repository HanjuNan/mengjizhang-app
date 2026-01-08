package com.mengjizhang.app.reminder

import android.content.Context
import android.content.SharedPreferences
import com.mengjizhang.app.data.model.ReminderSettings
import com.mengjizhang.app.data.model.ReminderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 提醒设置管理器
 */
object ReminderManager {
    private const val PREFS_NAME = "reminder_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_REPEAT_DAYS = "repeat_days"
    private const val KEY_REMINDER_TYPE = "reminder_type"

    private lateinit var prefs: SharedPreferences

    private val _settings = MutableStateFlow(ReminderSettings())
    val settings: StateFlow<ReminderSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSettings()
    }

    private fun loadSettings() {
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        val hour = prefs.getInt(KEY_HOUR, 20)
        val minute = prefs.getInt(KEY_MINUTE, 0)
        val repeatDaysString = prefs.getString(KEY_REPEAT_DAYS, "1,2,3,4,5,6,7") ?: "1,2,3,4,5,6,7"
        val repeatDays = repeatDaysString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        val reminderType = try {
            ReminderType.valueOf(prefs.getString(KEY_REMINDER_TYPE, "DAILY") ?: "DAILY")
        } catch (e: Exception) {
            ReminderType.DAILY
        }

        _settings.value = ReminderSettings(
            isEnabled = enabled,
            reminderHour = hour,
            reminderMinute = minute,
            repeatDays = repeatDays,
            reminderType = reminderType
        )
    }

    fun saveSettings(settings: ReminderSettings) {
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, settings.isEnabled)
            putInt(KEY_HOUR, settings.reminderHour)
            putInt(KEY_MINUTE, settings.reminderMinute)
            putString(KEY_REPEAT_DAYS, settings.repeatDays.joinToString(","))
            putString(KEY_REMINDER_TYPE, settings.reminderType.name)
            apply()
        }
        _settings.value = settings
    }

    fun setEnabled(enabled: Boolean) {
        saveSettings(_settings.value.copy(isEnabled = enabled))
    }

    fun setTime(hour: Int, minute: Int) {
        saveSettings(_settings.value.copy(reminderHour = hour, reminderMinute = minute))
    }

    fun setRepeatDays(days: Set<Int>) {
        saveSettings(_settings.value.copy(repeatDays = days))
    }

    fun setReminderType(type: ReminderType) {
        val days = when (type) {
            ReminderType.DAILY -> setOf(1, 2, 3, 4, 5, 6, 7)
            ReminderType.WEEKDAYS -> setOf(1, 2, 3, 4, 5)
            ReminderType.WEEKENDS -> setOf(6, 7)
            ReminderType.CUSTOM -> _settings.value.repeatDays
        }
        saveSettings(_settings.value.copy(reminderType = type, repeatDays = days))
    }
}
