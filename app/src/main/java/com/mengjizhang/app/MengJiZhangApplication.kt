package com.mengjizhang.app

import android.app.Application
import com.mengjizhang.app.data.local.AppDatabase
import com.mengjizhang.app.reminder.NotificationHelper
import com.mengjizhang.app.reminder.ReminderManager

class MengJiZhangApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化提醒管理器
        ReminderManager.init(this)

        // 创建通知渠道
        NotificationHelper.createNotificationChannel(this)
    }

    companion object {
        lateinit var instance: MengJiZhangApplication
            private set
    }
}
