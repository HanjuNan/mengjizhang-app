package com.mengjizhang.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mengjizhang.app.navigation.AppNavHost
import com.mengjizhang.app.navigation.Screen
import com.mengjizhang.app.ui.components.BottomNavBar
import com.mengjizhang.app.ui.theme.MengJiZhangTheme

@Composable
fun MengJiZhangApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 需要显示底部导航栏的页面
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Records.route,
        Screen.Stats.route,
        Screen.Profile.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        if (route == Screen.Add.route) {
                            navController.navigate(route)
                        } else if (route == Screen.Home.route) {
                            // 点击首页时，清空导航栈并回到首页
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        } else {
                            // 其他标签页的导航
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = if (showBottomBar) innerPadding.calculateBottomPadding()
                    else innerPadding.calculateBottomPadding()
                )
        ) {
            AppNavHost(
                navController = navController,
                onNavigateToAI = {
                    navController.navigate(Screen.AI.route)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MengJiZhangAppPreview() {
    MengJiZhangTheme {
        MengJiZhangApp()
    }
}
