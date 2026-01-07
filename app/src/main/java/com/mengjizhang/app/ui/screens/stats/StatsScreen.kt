package com.mengjizhang.app.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mengjizhang.app.data.local.CategoryExpense
import com.mengjizhang.app.ui.components.BarChart
import com.mengjizhang.app.ui.components.BarChartData
import com.mengjizhang.app.ui.components.CompareBarChart
import com.mengjizhang.app.ui.components.CompareBarData
import com.mengjizhang.app.ui.components.PieChart
import com.mengjizhang.app.ui.components.PieChartData
import com.mengjizhang.app.ui.components.PieChartLegend
import com.mengjizhang.app.ui.components.getCategoryColor
import com.mengjizhang.app.ui.theme.MintGreen
import com.mengjizhang.app.ui.theme.PinkLight
import com.mengjizhang.app.ui.theme.PinkPrimary
import com.mengjizhang.app.ui.theme.SunnyYellow
import com.mengjizhang.app.ui.viewmodel.DailyStats
import com.mengjizhang.app.ui.viewmodel.MonthlyStats
import com.mengjizhang.app.ui.viewmodel.RecordViewModel
import java.util.Calendar

@Composable
fun StatsScreen(
    viewModel: RecordViewModel? = null
) {
    var selectedPeriod by remember { mutableIntStateOf(1) } // 0=周, 1=月, 2=年

    val monthlyExpense by viewModel?.monthlyExpense?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(0.0) }
    val monthlyIncome by viewModel?.monthlyIncome?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(0.0) }
    val categoryStats by viewModel?.categoryStats?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(emptyList()) }

    // 周数据
    var weeklyData by remember { mutableStateOf<List<DailyStats>>(emptyList()) }
    // 月度趋势数据
    var monthlyTrendData by remember { mutableStateOf<List<MonthlyStats>>(emptyList()) }

    // 加载数据
    LaunchedEffect(selectedPeriod) {
        viewModel?.let { vm ->
            weeklyData = vm.getWeeklyData()
            monthlyTrendData = vm.getMonthlyTrendData()
        }
    }

    // 计算百分比
    val totalExpense = categoryStats.sumOf { it.totalAmount }
    val statsWithPercent = categoryStats.map { stat ->
        val percent = if (totalExpense > 0) (stat.totalAmount / totalExpense * 100).toInt() else 0
        stat to percent
    }

    // 转换为饼图数据
    val pieChartData = categoryStats.map { stat ->
        PieChartData(
            label = stat.categoryName,
            value = stat.totalAmount,
            color = getCategoryColor(stat.categoryId),
            emoji = stat.categoryEmoji
        )
    }

    // 获取今天是周几的索引
    val todayIndex = remember {
        val cal = Calendar.getInstance()
        (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 转换为周一=0的索引
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(48.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "统计分析",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    listOf("周", "月", "年").forEachIndexed { index, period ->
                        FilterChip(
                            selected = selectedPeriod == index,
                            onClick = { selectedPeriod = index },
                            label = { Text(period) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 收支概览卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (selectedPeriod) {
                            0 -> "本周收支"
                            1 -> "本月收支"
                            else -> "本年收支"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "收入",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "¥${String.format("%,.2f", monthlyIncome)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MintGreen
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "支出",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "¥${String.format("%,.2f", monthlyExpense)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PinkPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "结余",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val balance = monthlyIncome - monthlyExpense
                            Text(
                                text = "¥${String.format("%,.2f", balance)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (balance >= 0) MintGreen else PinkPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 趋势图
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (selectedPeriod) {
                            0 -> "支出趋势 (本周)"
                            1 -> "收支对比 (近6月)"
                            else -> "收支对比 (近6月)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedPeriod) {
                        0 -> {
                            // 周视图 - 柱状图
                            if (weeklyData.isNotEmpty()) {
                                BarChart(
                                    data = weeklyData.mapIndexed { index, stats ->
                                        BarChartData(
                                            label = stats.label,
                                            value = stats.expense,
                                            isHighlighted = index == weeklyData.size - 1
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                EmptyChartPlaceholder()
                            }
                        }
                        else -> {
                            // 月视图/年视图 - 收支对比图
                            if (monthlyTrendData.isNotEmpty()) {
                                CompareBarChart(
                                    data = monthlyTrendData.map { stats ->
                                        CompareBarData(
                                            label = stats.label,
                                            income = stats.income,
                                            expense = stats.expense
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                EmptyChartPlaceholder()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 分类饼图
        item {
            Text(
                text = "支出分类",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (pieChartData.isEmpty() || totalExpense <= 0) {
                    // 无数据时显示占位符
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📊",
                                style = MaterialTheme.typography.displaySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "暂无支出记录",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 饼图
                        PieChart(
                            data = pieChartData,
                            modifier = Modifier.size(140.dp),
                            strokeWidth = 40f
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // 图例
                        PieChartLegend(
                            data = pieChartData,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Category List
        if (statsWithPercent.isNotEmpty()) {
            items(statsWithPercent) { (stat, percent) ->
                CategoryStatItem(stat = stat, percent = percent)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // AI Insight
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SunnyYellow.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = PinkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI 智能洞察",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PinkPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val insightText = generateInsight(
                        categoryStats = statsWithPercent,
                        monthlyIncome = monthlyIncome,
                        monthlyExpense = monthlyExpense,
                        weeklyData = weeklyData
                    )

                    Text(
                        text = insightText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun EmptyChartPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "暂无数据",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryStatItem(stat: CategoryExpense, percent: Int) {
    val color = getCategoryColor(stat.categoryId)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stat.categoryEmoji,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stat.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${stat.count} 笔",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "¥${String.format("%.2f", stat.totalAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            }

            // 进度条
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percent / 100f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}

private fun generateInsight(
    categoryStats: List<Pair<CategoryExpense, Int>>,
    monthlyIncome: Double,
    monthlyExpense: Double,
    weeklyData: List<DailyStats>
): String {
    if (categoryStats.isEmpty()) {
        return "开始记录你的第一笔账单，我会帮你分析消费习惯~"
    }

    val insights = mutableListOf<String>()

    // 分析最高支出分类
    categoryStats.firstOrNull()?.let { (stat, percent) ->
        if (percent > 40) {
            insights.add("本月${stat.categoryName}支出占比较高($percent%)，可以适当关注这方面的开支。")
        } else if (percent > 25) {
            insights.add("${stat.categoryName}是本月主要支出分类($percent%)。")
        }
    }

    // 分析收支平衡
    val balance = monthlyIncome - monthlyExpense
    if (balance < 0) {
        insights.add("本月支出超过收入，建议适当控制开支。")
    } else if (balance > monthlyIncome * 0.3 && monthlyIncome > 0) {
        insights.add("本月储蓄率不错，继续保持！")
    }

    // 分析周数据趋势
    if (weeklyData.size >= 2) {
        val recentExpense = weeklyData.takeLast(3).sumOf { it.expense }
        val earlierExpense = weeklyData.take(3).sumOf { it.expense }
        if (recentExpense > earlierExpense * 1.5 && earlierExpense > 0) {
            insights.add("近几天支出有所增加，注意控制哦~")
        }
    }

    return if (insights.isEmpty()) {
        "本月消费比较均衡，继续保持良好的记账习惯！"
    } else {
        insights.joinToString(" ")
    }
}
