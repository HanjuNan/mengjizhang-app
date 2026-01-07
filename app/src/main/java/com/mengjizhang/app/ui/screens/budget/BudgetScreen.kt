package com.mengjizhang.app.ui.screens.budget

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mengjizhang.app.data.model.BudgetStatus
import com.mengjizhang.app.ui.theme.ExpenseRed
import com.mengjizhang.app.ui.theme.IncomeGreen
import com.mengjizhang.app.ui.theme.MintGreen
import com.mengjizhang.app.ui.theme.PinkLight
import com.mengjizhang.app.ui.theme.PinkPrimary
import com.mengjizhang.app.ui.theme.SunnyYellow
import com.mengjizhang.app.ui.viewmodel.RecordViewModel
import kotlinx.coroutines.launch

@Composable
fun BudgetScreen(
    viewModel: RecordViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val budgetStatus by viewModel.budgetStatus.collectAsState()

    var budgetInput by remember { mutableStateOf("") }
    var recommendedBudget by remember { mutableStateOf(0.0) }

    // 初始化输入值
    LaunchedEffect(monthlyBudget) {
        if (monthlyBudget > 0) {
            budgetInput = monthlyBudget.toLong().toString()
        }
    }

    // 获取推荐预算
    LaunchedEffect(Unit) {
        recommendedBudget = viewModel.getRecommendedBudget()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Spacer(modifier = Modifier.height(40.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "预算设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            // 占位
            Box(modifier = Modifier.size(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 当前月份显示
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PinkLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PinkPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = PinkPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${currentYear}年${currentMonth}月",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "当前月份预算设置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 预算输入
            Text(
                text = "设置月度预算",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = budgetInput,
                onValueChange = { input ->
                    // 只允许数字
                    if (input.isEmpty() || input.all { it.isDigit() }) {
                        budgetInput = input
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("请输入预算金额") },
                prefix = { Text("¥", color = PinkPrimary, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            // 快捷金额选择
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1000, 2000, 3000, 5000).forEach { amount ->
                    TextButton(
                        onClick = { budgetInput = amount.toString() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (budgetInput == amount.toString()) PinkPrimary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "¥$amount",
                            color = if (budgetInput == amount.toString()) PinkPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 推荐预算提示
            if (recommendedBudget > 0 && recommendedBudget != monthlyBudget) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MintGreen.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "上次预算: ¥${recommendedBudget.toLong()}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { budgetInput = recommendedBudget.toLong().toString() }
                        ) {
                            Text("使用", color = IncomeGreen)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 当前预算使用情况
            if (budgetStatus.budget > 0) {
                BudgetProgressCard(
                    budgetStatus = budgetStatus,
                    monthlyExpense = monthlyExpense
                )
            }
        }

        // 保存按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    val amount = budgetInput.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        viewModel.setBudget(amount)
                        Toast.makeText(context, "预算设置成功", Toast.LENGTH_SHORT).show()
                        onBack()
                    } else {
                        Toast.makeText(context, "请输入有效的预算金额", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                enabled = budgetInput.isNotEmpty()
            ) {
                Text(
                    text = "保存预算",
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BudgetProgressCard(
    budgetStatus: BudgetStatus,
    monthlyExpense: Double
) {
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
            Text(
                text = "本月预算使用情况",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 进度条
            LinearProgressIndicator(
                progress = { budgetStatus.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = when {
                    budgetStatus.isOverBudget -> ExpenseRed
                    budgetStatus.percentage > 0.8f -> SunnyYellow
                    else -> IncomeGreen
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 使用详情
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ExpenseRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "已支出",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "¥${String.format("%.2f", budgetStatus.spent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (budgetStatus.isOverBudget) ExpenseRed else IncomeGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (budgetStatus.isOverBudget) "已超支" else "剩余",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (budgetStatus.isOverBudget)
                            "¥${String.format("%.2f", -budgetStatus.remaining)}"
                        else
                            "¥${String.format("%.2f", budgetStatus.remaining)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (budgetStatus.isOverBudget) ExpenseRed else IncomeGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 使用百分比提示
            val percentText = "${(budgetStatus.percentage * 100).toInt()}%"
            val statusText = when {
                budgetStatus.isOverBudget -> "已超出预算 ${((budgetStatus.percentage - 1) * 100).toInt()}%"
                budgetStatus.percentage > 0.8f -> "预算即将用完，请注意控制支出"
                budgetStatus.percentage > 0.5f -> "已使用过半，继续保持"
                else -> "支出控制良好，继续加油"
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        budgetStatus.isOverBudget -> ExpenseRed.copy(alpha = 0.1f)
                        budgetStatus.percentage > 0.8f -> SunnyYellow.copy(alpha = 0.2f)
                        else -> MintGreen.copy(alpha = 0.3f)
                    }
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            budgetStatus.isOverBudget -> "⚠️"
                            budgetStatus.percentage > 0.8f -> "⏰"
                            else -> "✅"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "已使用 $percentText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
