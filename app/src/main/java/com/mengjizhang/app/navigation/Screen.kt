package com.mengjizhang.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Records : Screen("records")
    data object Add : Screen("add")
    data object Edit : Screen("edit")  // 编辑账单
    data object Stats : Screen("stats")
    data object Profile : Screen("profile")
    data object AI : Screen("ai")
    data object Camera : Screen("camera")
    data object RecordDetail : Screen("record_detail")
    data object Budget : Screen("budget")  // 预算设置
    data object Export : Screen("export")  // 数据导出/备份
    data object CloudSync : Screen("cloud_sync")  // 云同步
    data object Search : Screen("search")  // 搜索
    data object ThemeSettings : Screen("theme_settings")  // 主题设置
    data object CategoryManagement : Screen("category_management")  // 分类管理
    data object TagManagement : Screen("tag_management")  // 标签管理
    data object ReminderSettings : Screen("reminder_settings")  // 提醒设置
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isCenter: Boolean = false
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        label = "首页",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        route = Screen.Records.route,
        label = "账单",
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt
    ),
    BottomNavItem(
        route = Screen.Add.route,
        label = "记账",
        selectedIcon = Icons.Filled.Add,
        unselectedIcon = Icons.Filled.Add,
        isCenter = true
    ),
    BottomNavItem(
        route = Screen.Stats.route,
        label = "统计",
        selectedIcon = Icons.Filled.PieChart,
        unselectedIcon = Icons.Outlined.PieChart
    ),
    BottomNavItem(
        route = Screen.Profile.route,
        label = "我的",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)
