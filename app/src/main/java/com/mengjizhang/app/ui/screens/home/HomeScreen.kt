package com.mengjizhang.app.ui.screens.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import com.mengjizhang.app.data.model.BudgetStatus
import com.mengjizhang.app.data.model.Record
import com.mengjizhang.app.ui.theme.ExpenseRed
import com.mengjizhang.app.ui.theme.IncomeGreen
import com.mengjizhang.app.ui.theme.LavenderPurple
import com.mengjizhang.app.ui.theme.MintGreen
import com.mengjizhang.app.ui.theme.PinkLight
import com.mengjizhang.app.ui.theme.PinkPrimary
import com.mengjizhang.app.ui.theme.SkyBlue
import com.mengjizhang.app.ui.theme.SunnyYellow
import com.mengjizhang.app.ui.viewmodel.RecordViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: RecordViewModel? = null,
    onNavigateToRecords: () -> Unit = {},
    onNavigateToAdd: () -> Unit = {},
    onNavigateToAI: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {}
) {
    var isBalanceVisible by remember { mutableStateOf(true) }

    // 从 ViewModel 获取数据
    val recentRecords by viewModel?.recentRecords?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val monthlyIncome by viewModel?.monthlyIncome?.collectAsState() ?: remember { mutableStateOf(0.0) }
    val monthlyExpense by viewModel?.monthlyExpense?.collectAsState() ?: remember { mutableStateOf(0.0) }
    val monthlyBalance by viewModel?.monthlyBalance?.collectAsState() ?: remember { mutableStateOf(0.0) }
    val budgetStatus by viewModel?.budgetStatus?.collectAsState() ?: remember { mutableStateOf(BudgetStatus.Empty) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(48.dp))
            HomeHeader()
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Balance Card
        item {
            BalanceCard(
                isVisible = isBalanceVisible,
                onToggleVisibility = { isBalanceVisible = !isBalanceVisible },
                balance = monthlyBalance,
                income = monthlyIncome,
                expense = monthlyExpense
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Quick Actions
        item {
            QuickActionsRow(
                onVoiceClick = { onNavigateToAdd() },
                onCameraClick = { onNavigateToAdd() },
                onManualClick = { onNavigateToAdd() },
                onAIClick = { onNavigateToAI() }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Budget Progress Card (仅当设置了预算时显示)
        if (budgetStatus.budget > 0) {
            item {
                BudgetProgressCard(budgetStatus = budgetStatus)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Recent Records Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近账单",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.clickable { onNavigateToRecords() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "查看全部",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Recent Records List
        if (recentRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📝", style = MaterialTheme.typography.displaySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "还没有记录，快去记一笔吧~",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentRecords) { record ->
                RecordItem(
                    record = record,
                    onClick = { onNavigateToDetail(record.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun HomeHeader() {
    val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
    val today = dateFormat.format(Date())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hi~ 小可爱",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = today,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(PinkLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "😊",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun BalanceCard(
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    balance: Double,
    income: Double,
    expense: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PinkPrimary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(PinkPrimary, PinkLight.copy(alpha = 0.8f))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "本月结余",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "切换可见性",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isVisible) "¥ ${String.format("%,.2f", balance)}" else "¥ ****",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    BalanceItem(
                        icon = Icons.Default.ArrowDownward,
                        label = "收入",
                        amount = if (isVisible) "¥${String.format("%,.2f", income)}" else "****",
                        iconTint = IncomeGreen
                    )
                    BalanceItem(
                        icon = Icons.Default.ArrowUpward,
                        label = "支出",
                        amount = if (isVisible) "¥${String.format("%,.2f", expense)}" else "****",
                        iconTint = ExpenseRed
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceItem(
    icon: ImageVector,
    label: String,
    amount: String,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    onVoiceClick: () -> Unit,
    onCameraClick: () -> Unit,
    onManualClick: () -> Unit,
    onAIClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionItem(
            icon = Icons.Default.Mic,
            label = "语音记账",
            backgroundColor = PinkLight,
            onClick = onVoiceClick
        )
        QuickActionItem(
            icon = Icons.Default.CameraAlt,
            label = "拍照记账",
            backgroundColor = SkyBlue.copy(alpha = 0.3f),
            onClick = onCameraClick
        )
        QuickActionItem(
            icon = Icons.Default.Edit,
            label = "手动记账",
            backgroundColor = MintGreen.copy(alpha = 0.3f),
            onClick = onManualClick
        )
        QuickActionItem(
            icon = Icons.Default.AutoAwesome,
            label = "AI助手",
            backgroundColor = LavenderPurple.copy(alpha = 0.3f),
            onClick = onAIClick
        )
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PinkPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun RecordItem(
    record: Record,
    onClick: () -> Unit = {}
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)
    val dateFormat = SimpleDateFormat("MM-dd", Locale.CHINA)
    val time = timeFormat.format(Date(record.date))
    val date = dateFormat.format(Date(record.date))

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
                    text = "$date $time",
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

/**
 * 预算进度卡片
 */
@Composable
private fun BudgetProgressCard(budgetStatus: BudgetStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本月预算",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(budgetStatus.percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        budgetStatus.isOverBudget -> ExpenseRed
                        budgetStatus.percentage > 0.8f -> SunnyYellow
                        else -> IncomeGreen
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 进度条
            LinearProgressIndicator(
                progress = { budgetStatus.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    budgetStatus.isOverBudget -> ExpenseRed
                    budgetStatus.percentage > 0.8f -> SunnyYellow
                    else -> IncomeGreen
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "已支出",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥${String.format("%.0f", budgetStatus.spent)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ExpenseRed
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (budgetStatus.isOverBudget) "已超支" else "剩余",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (budgetStatus.isOverBudget)
                            "¥${String.format("%.0f", -budgetStatus.remaining)}"
                        else
                            "¥${String.format("%.0f", budgetStatus.remaining)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (budgetStatus.isOverBudget) ExpenseRed else IncomeGreen
                    )
                }
            }
        }
    }
}
