package com.mengjizhang.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mengjizhang.app.ui.theme.CategoryEntertainment
import com.mengjizhang.app.ui.theme.CategoryFood
import com.mengjizhang.app.ui.theme.CategoryOther
import com.mengjizhang.app.ui.theme.CategoryShopping
import com.mengjizhang.app.ui.theme.CategoryTransport
import com.mengjizhang.app.ui.theme.LavenderPurple
import com.mengjizhang.app.ui.theme.MintGreen
import com.mengjizhang.app.ui.theme.PinkPrimary
import com.mengjizhang.app.ui.theme.SunnyYellow

/**
 * 饼图数据
 */
data class PieChartData(
    val label: String,
    val value: Double,
    val color: Color,
    val emoji: String = ""
)

/**
 * 饼图组件
 */
@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 60f,
    animationDuration: Int = 1000
) {
    val total = data.sumOf { it.value }
    if (total <= 0) return

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasSize = size.minDimension
            val radius = (canvasSize - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            var startAngle = -90f

            data.forEach { item ->
                val sweepAngle = (item.value / total * 360f * animatedProgress.value).toFloat()

                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )

                startAngle += sweepAngle
            }
        }

        // 中心显示总额
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "¥${String.format("%,.0f", total)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PinkPrimary
            )
            Text(
                text = "总支出",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 饼图图例
 */
@Composable
fun PieChartLegend(
    data: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.value }

    Column(modifier = modifier) {
        data.take(5).forEach { item ->
            val percent = if (total > 0) (item.value / total * 100).toInt() else 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(item.color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${item.emoji} ${item.label}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 趋势折线图数据
 */
data class LineChartData(
    val label: String,
    val value: Double
)

/**
 * 趋势折线图组件
 */
@Composable
fun LineChart(
    data: List<LineChartData>,
    modifier: Modifier = Modifier,
    lineColor: Color = PinkPrimary,
    fillColor: Color = PinkPrimary.copy(alpha = 0.2f),
    animationDuration: Int = 1000
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull { it.value } ?: 0.0
    val minValue = 0.0

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            if (data.isEmpty() || maxValue <= 0) return@Canvas

            val padding = 20f
            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2

            val points = data.mapIndexed { index, item ->
                val x = padding + (chartWidth / (data.size - 1).coerceAtLeast(1)) * index
                val normalizedValue = ((item.value - minValue) / (maxValue - minValue)).coerceIn(0.0, 1.0)
                val y = padding + chartHeight * (1 - normalizedValue * animatedProgress.value).toFloat()
                Offset(x, y)
            }

            // 绘制填充区域
            if (points.size > 1) {
                val fillPath = Path().apply {
                    moveTo(points.first().x, size.height - padding)
                    points.forEach { point ->
                        lineTo(point.x, point.y)
                    }
                    lineTo(points.last().x, size.height - padding)
                    close()
                }
                drawPath(fillPath, fillColor)

                // 绘制线条
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = lineColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }

                // 绘制点
                points.forEach { point ->
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = point
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 4f,
                        center = point
                    )
                }
            }
        }

        // X轴标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { item ->
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 柱状图数据
 */
data class BarChartData(
    val label: String,
    val value: Double,
    val isHighlighted: Boolean = false
)

/**
 * 柱状图组件
 */
@Composable
fun BarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    barColor: Color = PinkPrimary.copy(alpha = 0.5f),
    highlightColor: Color = PinkPrimary,
    animationDuration: Int = 800
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull { it.value } ?: 1.0

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
        )
    }

    Row(
        modifier = modifier.height(140.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { item ->
            val heightFraction = if (maxValue > 0) (item.value / maxValue).toFloat() else 0f

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 数值标签
                if (item.value > 0) {
                    Text(
                        text = String.format("%.0f", item.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isHighlighted) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                // 柱状条
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height((100 * heightFraction * animatedProgress.value).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (item.isHighlighted) highlightColor else barColor)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 日期标签
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 收支对比双柱图数据
 */
data class CompareBarData(
    val label: String,
    val income: Double,
    val expense: Double
)

/**
 * 收支对比双柱图
 */
@Composable
fun CompareBarChart(
    data: List<CompareBarData>,
    modifier: Modifier = Modifier,
    incomeColor: Color = MintGreen,
    expenseColor: Color = PinkPrimary,
    animationDuration: Int = 800
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { item ->
                val incomeHeight = if (maxValue > 0) (item.income / maxValue).toFloat() else 0f
                val expenseHeight = if (maxValue > 0) (item.expense / maxValue).toFloat() else 0f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // 收入柱
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height((90 * incomeHeight * animatedProgress.value).dp.coerceAtLeast(2.dp))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(incomeColor)
                        )
                        // 支出柱
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height((90 * expenseHeight * animatedProgress.value).dp.coerceAtLeast(2.dp))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(expenseColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 图例
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(incomeColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "收入",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(expenseColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "支出",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 获取分类颜色
 */
fun getCategoryColor(categoryId: Int): Color {
    return when (categoryId) {
        1 -> CategoryFood
        2 -> CategoryTransport
        3 -> CategoryShopping
        4 -> CategoryEntertainment
        5 -> SunnyYellow
        6 -> LavenderPurple
        7 -> MintGreen
        else -> CategoryOther
    }
}
