package com.mengjizhang.app.ui.screens.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mengjizhang.app.data.model.Record
import com.mengjizhang.app.ui.theme.ExpenseRed
import com.mengjizhang.app.ui.theme.IncomeGreen
import com.mengjizhang.app.ui.theme.MintGreen
import com.mengjizhang.app.ui.theme.PinkLight
import com.mengjizhang.app.ui.theme.PinkPrimary
import com.mengjizhang.app.ui.viewmodel.RecordViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun RecordsScreen(
    viewModel: RecordViewModel? = null,
    onNavigateToDetail: (Long) -> Unit = {}
) {
    val currentYear by viewModel?.currentYear?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    val currentMonth by viewModel?.currentMonth?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    val monthlyIncome by viewModel?.monthlyIncome?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0.0) }
    val monthlyExpense by viewModel?.monthlyExpense?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0.0) }
    val monthlyBalance by viewModel?.monthlyBalance?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0.0) }
    val recentRecords by viewModel?.recentRecords?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList()) }

    // 按日期分组
    val groupedRecords = recentRecords.groupBy { record ->
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = record.date
        Triple(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }.toSortedMap(compareByDescending<Triple<Int, Int, Int>> { it.first }
        .thenByDescending { it.second }
        .thenByDescending { it.third })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "账单明细",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "筛选",
                    tint = PinkPrimary
                )
            }
        }

        // Month Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel?.previousMonth() }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
            }
            Text(
                text = "${currentYear}年${currentMonth}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = { viewModel?.nextMonth() }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
            }
        }

        // Month Summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryItem(label = "收入", value = "¥${String.format("%,.2f", monthlyIncome)}", color = IncomeGreen)
                SummaryItem(label = "支出", value = "¥${String.format("%,.2f", monthlyExpense)}", color = ExpenseRed)
                SummaryItem(label = "结余", value = "¥${String.format("%,.2f", monthlyBalance)}", color = PinkPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Records by Date
        if (recentRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📝", style = MaterialTheme.typography.displaySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "本月还没有记录~",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                groupedRecords.forEach { (dateKey, records) ->
                    val (year, month, day) = dateKey
                    val calendar = Calendar.getInstance()
                    calendar.set(year, month, day)

                    val dateStr = formatDateHeader(calendar)
                    val dayTotal = records.filter { it.isExpense }.sumOf { it.amount }
                    val dayIncome = records.filter { !it.isExpense }.sumOf { it.amount }

                    item {
                        DateGroupHeader(
                            date = dateStr,
                            summary = buildString {
                                if (dayIncome > 0) append("收入 ¥${String.format("%,.2f", dayIncome)}")
                                if (dayIncome > 0 && dayTotal > 0) append(" | ")
                                if (dayTotal > 0) append("支出 ¥${String.format("%,.2f", dayTotal)}")
                            }
                        )
                    }

                    items(records) { record ->
                        RecordListItem(
                            record = record,
                            onClick = { onNavigateToDetail(record.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

private fun formatDateHeader(calendar: Calendar): String {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }

    val dayOfWeek = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    val week = dayOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1]

    return when {
        isSameDay(calendar, today) -> "今天 · ${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日 $week"
        isSameDay(calendar, yesterday) -> "昨天 · ${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日 $week"
        else -> "${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日 $week"
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
            cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun DateGroupHeader(date: String, summary: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecordListItem(
    record: Record,
    onClick: () -> Unit = {}
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)
    val time = timeFormat.format(Date(record.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (record.isExpense) PinkLight.copy(alpha = 0.5f) else MintGreen.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = record.categoryEmoji, style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (record.note.isNotEmpty()) "${record.categoryName} - ${record.note}" else record.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (record.isExpense) "-¥${String.format("%.2f", record.amount)}"
                else "+¥${String.format("%,.2f", record.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (record.isExpense) ExpenseRed else IncomeGreen
            )
        }
    }
}
