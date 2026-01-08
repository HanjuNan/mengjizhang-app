package com.mengjizhang.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mengjizhang.app.MainActivity
import com.mengjizhang.app.R
import com.mengjizhang.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * 快捷记账小组件
 */
class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // 小组件首次添加时调用
    }

    override fun onDisabled(context: Context) {
        // 最后一个小组件被移除时调用
    }

    companion object {
        const val ACTION_ADD_EXPENSE = "com.mengjizhang.app.ACTION_ADD_EXPENSE"
        const val ACTION_ADD_INCOME = "com.mengjizhang.app.ACTION_ADD_INCOME"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_add)

            // 设置点击打开应用
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_balance, openAppPendingIntent)

            // 设置支出按钮点击
            val expenseIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_ADD_EXPENSE
                putExtra("is_expense", true)
            }
            val expensePendingIntent = PendingIntent.getActivity(
                context, 1, expenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_add_expense, expensePendingIntent)

            // 设置收入按钮点击
            val incomeIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_ADD_INCOME
                putExtra("is_expense", false)
            }
            val incomePendingIntent = PendingIntent.getActivity(
                context, 2, incomeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_add_income, incomePendingIntent)

            // 异步加载数据
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val recordDao = db.recordDao()

                    // 获取本月起止时间
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfMonth = calendar.timeInMillis

                    calendar.add(Calendar.MONTH, 1)
                    val endOfMonth = calendar.timeInMillis

                    // 查询本月收支
                    val monthlyRecords = recordDao.getRecordsBetweenDatesSync(startOfMonth, endOfMonth)

                    var totalExpense = 0.0
                    var totalIncome = 0.0

                    monthlyRecords.forEach { record ->
                        if (record.isExpense) {
                            totalExpense += record.amount
                        } else {
                            totalIncome += record.amount
                        }
                    }

                    val balance = totalIncome - totalExpense
                    val formatter = NumberFormat.getCurrencyInstance(Locale.CHINA)

                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.tv_expense, formatter.format(totalExpense))
                        views.setTextViewText(R.id.tv_income, formatter.format(totalIncome))
                        views.setTextViewText(R.id.tv_balance, formatter.format(balance))
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    // 出错时显示默认值
                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.tv_expense, "¥0.00")
                        views.setTextViewText(R.id.tv_income, "¥0.00")
                        views.setTextViewText(R.id.tv_balance, "¥0.00")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }

            // 先显示默认值
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * 刷新所有小组件
         */
        fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, QuickAddWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
