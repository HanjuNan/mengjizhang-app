package com.mengjizhang.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mengjizhang.app.ui.screens.add.AddScreen
import com.mengjizhang.app.ui.screens.ai.AIScreen
import com.mengjizhang.app.ui.screens.budget.BudgetScreen
import com.mengjizhang.app.ui.screens.camera.CameraScreen
import com.mengjizhang.app.ui.screens.export.ExportScreen
import com.mengjizhang.app.ui.screens.detail.RecordDetailScreen
import com.mengjizhang.app.ui.screens.home.HomeScreen
import com.mengjizhang.app.ui.screens.profile.ProfileScreen
import com.mengjizhang.app.ui.screens.records.RecordsScreen
import com.mengjizhang.app.ui.screens.search.SearchScreen
import com.mengjizhang.app.ui.screens.stats.StatsScreen
import com.mengjizhang.app.ui.viewmodel.RecordViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    recordViewModel: RecordViewModel = viewModel(),
    onNavigateToAI: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = recordViewModel,
                onNavigateToRecords = { navController.navigate(Screen.Records.route) },
                onNavigateToAdd = { navController.navigate(Screen.Add.route) },
                onNavigateToAI = onNavigateToAI,
                onNavigateToDetail = { recordId ->
                    navController.navigate("${Screen.RecordDetail.route}/$recordId")
                }
            )
        }

        composable(Screen.Records.route) {
            RecordsScreen(
                viewModel = recordViewModel,
                onNavigateToDetail = { recordId ->
                    navController.navigate("${Screen.RecordDetail.route}/$recordId")
                },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) }
            )
        }

        composable(Screen.Add.route) { backStackEntry ->
            // 监听从 CameraScreen 返回的数据
            var ocrAmount by remember { mutableStateOf<Double?>(null) }
            var ocrCategory by remember { mutableStateOf<String?>(null) }
            var ocrNote by remember { mutableStateOf<String?>(null) }
            var ocrImagePath by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(backStackEntry) {
                backStackEntry.savedStateHandle.apply {
                    get<Double>("ocr_amount")?.let { ocrAmount = it }
                    get<String>("ocr_category")?.let { ocrCategory = it }
                    get<String>("ocr_note")?.let { ocrNote = it }
                    get<String>("ocr_image_path")?.let { ocrImagePath = it }

                    // 清除数据，避免重复读取
                    remove<Double>("ocr_amount")
                    remove<String>("ocr_category")
                    remove<String>("ocr_note")
                    remove<String>("ocr_image_path")
                }
            }

            AddScreen(
                onBack = { navController.popBackStack() },
                onSave = { amount, categoryId, isExpense, note, imagePath, inputMethod ->
                    recordViewModel.addRecord(
                        amount = amount,
                        categoryId = categoryId,
                        isExpense = isExpense,
                        note = note,
                        imagePath = imagePath,
                        inputMethod = inputMethod
                    )
                },
                onNavigateToCamera = { mode ->
                    navController.navigate("${Screen.Camera.route}?mode=$mode")
                },
                initialAmount = ocrAmount,
                initialNote = ocrNote,
                initialImagePath = ocrImagePath,
                initialCategory = ocrCategory
            )
        }

        composable("${Screen.Camera.route}?mode={mode}") { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "camera"
            CameraScreen(
                mode = mode,
                onBack = { navController.popBackStack() },
                onResult = { amount, category, note, imagePath ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("ocr_amount", amount)
                        set("ocr_category", category)
                        set("ocr_note", note)
                        set("ocr_image_path", imagePath)
                    }
                    navController.popBackStack()
                }
            )
        }

        // 编辑记录页面
        composable(
            route = "${Screen.Edit.route}/{recordId}",
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
            val recentRecords by recordViewModel.recentRecords.collectAsState()
            val monthlyRecords by recordViewModel.monthlyRecords.collectAsState()

            // 从记录中查找要编辑的数据
            val record = recentRecords.find { it.id == recordId }
                ?: monthlyRecords.find { it.id == recordId }

            record?.let {
                AddScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { _, _, _, _, _, _ -> },  // 编辑模式不使用
                    onUpdate = { id, amount, categoryId, isExpense, note, imagePath ->
                        recordViewModel.updateRecord(
                            recordId = id,
                            amount = amount,
                            categoryId = categoryId,
                            isExpense = isExpense,
                            note = note,
                            imagePath = imagePath
                        )
                    },
                    onNavigateToCamera = { mode ->
                        navController.navigate("${Screen.Camera.route}?mode=$mode")
                    },
                    initialAmount = it.amount,
                    initialNote = it.note,
                    initialImagePath = it.imagePath,
                    editRecordId = it.id,
                    editCategoryId = it.categoryId,
                    editIsExpense = it.isExpense
                )
            }
        }

        composable(
            route = "${Screen.RecordDetail.route}/{recordId}",
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
            val recentRecords by recordViewModel.recentRecords.collectAsState()
            val monthlyRecords by recordViewModel.monthlyRecords.collectAsState()

            // 从最近记录或月度记录中查找
            val record = recentRecords.find { it.id == recordId }
                ?: monthlyRecords.find { it.id == recordId }

            RecordDetailScreen(
                record = record,
                onBack = { navController.popBackStack() },
                onDelete = { recordToDelete ->
                    recordViewModel.deleteRecord(recordToDelete)
                },
                onEdit = { recordId ->
                    navController.navigate("${Screen.Edit.route}/$recordId")
                }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(viewModel = recordViewModel)
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToBudget = { navController.navigate(Screen.Budget.route) },
                onNavigateToExport = { navController.navigate(Screen.Export.route) }
            )
        }

        composable(Screen.Export.route) {
            ExportScreen(
                viewModel = recordViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Budget.route) {
            BudgetScreen(
                viewModel = recordViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AI.route) {
            AIScreen(
                onBack = { navController.popBackStack() },
                viewModel = recordViewModel
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                viewModel = recordViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { recordId ->
                    navController.navigate("${Screen.RecordDetail.route}/$recordId")
                }
            )
        }
    }
}
