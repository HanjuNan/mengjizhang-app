package com.mengjizhang.app.reminder

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 提醒 Worker - 执行定时提醒任务
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val settings = ReminderManager.settings.value

        // 检查今天是否需要提醒
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        // Calendar.DAY_OF_WEEK: 1=周日, 2=周一...7=周六
        // 转换为我们的格式: 1=周一...7=周日
        val dayOfWeek = if (today == Calendar.SUNDAY) 7 else today - 1

        if (settings.isEnabled && dayOfWeek in settings.repeatDays) {
            NotificationHelper.showReminderNotification(applicationContext)
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "reminder_work"
    }
}

/**
 * 提醒调度器
 */
object ReminderScheduler {

    /**
     * 调度提醒任务
     */
    fun scheduleReminder(context: Context) {
        val settings = ReminderManager.settings.value

        if (!settings.isEnabled) {
            cancelReminder(context)
            return
        }

        // 计算到下次提醒的延迟时间
        val delay = calculateInitialDelay(settings.reminderHour, settings.reminderMinute)

        // 创建周期性任务（每天执行一次）
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(ReminderWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }

    /**
     * 取消提醒任务
     */
    fun cancelReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ReminderWorker.WORK_NAME)
    }

    /**
     * 计算到指定时间的延迟（毫秒）
     */
    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 如果目标时间已过，设置为明天
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
