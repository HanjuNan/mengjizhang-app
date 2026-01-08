package com.mengjizhang.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mengjizhang.app.ui.theme.MengJiZhangTheme
import com.mengjizhang.app.ui.theme.ThemeManager
import com.mengjizhang.app.widget.QuickAddWidgetProvider

class MainActivity : ComponentActivity() {

    companion object {
        var pendingAddExpense: Boolean? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化主题管理器
        ThemeManager.init(this)

        // 处理小组件点击
        handleWidgetIntent(intent)

        enableEdgeToEdge()
        setContent {
            MengJiZhangTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MengJiZhangApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        when (intent?.action) {
            QuickAddWidgetProvider.ACTION_ADD_EXPENSE -> {
                pendingAddExpense = true
            }
            QuickAddWidgetProvider.ACTION_ADD_INCOME -> {
                pendingAddExpense = false
            }
        }
    }
}
