package com.mengjizhang.app.ui.screens.ai

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mengjizhang.app.api.ChatMessage as ApiChatMessage
import com.mengjizhang.app.ui.theme.LavenderPurple
import com.mengjizhang.app.ui.theme.MengJiZhangTheme
import com.mengjizhang.app.ui.theme.PinkLight
import com.mengjizhang.app.ui.theme.PinkPrimary
import com.mengjizhang.app.ui.viewmodel.RecordViewModel
import com.mengjizhang.app.utils.AIChatHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// UI 层的消息数据类
data class UiChatMessage(
    val content: String,
    val isFromUser: Boolean,
    val analysisCard: AnalysisCard? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

data class AnalysisCard(
    val title: String,
    val items: List<Pair<String, String>>,
    val suggestion: String
)

private val quickQuestions = listOf(
    "这个月花了多少？",
    "帮我分析消费",
    "如何存更多钱？",
    "帮我制定预算"
)

@Composable
fun AIScreen(
    onBack: () -> Unit = {},
    viewModel: RecordViewModel? = null
) {
    var inputText by remember { mutableStateOf("") }
    val uiMessages = remember { mutableStateListOf<UiChatMessage>() }
    // API 对话历史（用于发送给 AI）
    val chatHistory = remember { mutableStateListOf<ApiChatMessage>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // 从 ViewModel 获取数据
    val recentRecords by viewModel?.recentRecords?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val categoryStats by viewModel?.categoryStats?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val monthlyExpense by viewModel?.monthlyExpense?.collectAsState() ?: remember { mutableStateOf(0.0) }
    val monthlyIncome by viewModel?.monthlyIncome?.collectAsState() ?: remember { mutableStateOf(0.0) }

    // 初始化欢迎消息
    LaunchedEffect(Unit) {
        if (uiMessages.isEmpty()) {
            uiMessages.add(
                UiChatMessage(
                    content = "Hi~ 我是小萌，你的AI记账助手！✨\n\n我可以帮你：\n• 分析消费习惯\n• 制定预算计划\n• 提供省钱建议\n• 解答理财问题\n\n快来问问我吧~",
                    isFromUser = false
                )
            )
        }
    }

    // 发送消息
    fun sendMessage(text: String) {
        if (text.isBlank() || isLoading) return

        val userMessage = text.trim()
        inputText = ""
        isLoading = true

        // 添加用户消息到 UI
        uiMessages.add(UiChatMessage(content = userMessage, isFromUser = true))

        // 添加到 API 历史
        chatHistory.add(ApiChatMessage(role = "user", content = userMessage))

        // 滚动到底部
        scope.launch {
            delay(100)
            listState.animateScrollToItem(uiMessages.size - 1)
        }

        // 添加加载消息
        val loadingIndex = uiMessages.size
        uiMessages.add(UiChatMessage(content = "思考中...", isFromUser = false, isLoading = true))

        scope.launch {
            // 调用真实 AI API
            val result = AIChatHelper.askAI(
                userMessage = userMessage,
                chatHistory = chatHistory.toList(),
                monthlyExpense = monthlyExpense,
                monthlyIncome = monthlyIncome,
                categoryStats = categoryStats
            )

            // 移除加载消息
            if (loadingIndex < uiMessages.size) {
                uiMessages.removeAt(loadingIndex)
            }

            result.fold(
                onSuccess = { response ->
                    // 添加 AI 回复到历史
                    chatHistory.add(ApiChatMessage(role = "assistant", content = response))

                    // 检查是否需要显示分析卡片
                    val analysisCard = if (AIChatHelper.shouldShowAnalysisCard(userMessage)) {
                        val analysis = AIChatHelper.analyzeMonthlySpending(
                            records = recentRecords,
                            categoryStats = categoryStats,
                            monthlyExpense = monthlyExpense,
                            monthlyIncome = monthlyIncome
                        )
                        AnalysisCard(
                            title = analysis.title,
                            items = analysis.items,
                            suggestion = analysis.suggestion
                        )
                    } else null

                    uiMessages.add(
                        UiChatMessage(
                            content = response,
                            isFromUser = false,
                            analysisCard = analysisCard
                        )
                    )
                },
                onFailure = { error ->
                    uiMessages.add(
                        UiChatMessage(
                            content = "抱歉，遇到了一点问题：${error.message}\n请稍后再试~",
                            isFromUser = false,
                            isError = true
                        )
                    )
                }
            )

            isLoading = false

            // 滚动到底部
            delay(100)
            listState.animateScrollToItem(uiMessages.size - 1)
        }
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AI小萌",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Powered by SiliconFlow",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = {
                uiMessages.clear()
                chatHistory.clear()
                uiMessages.add(
                    UiChatMessage(
                        content = "对话已清空~ 有什么新问题可以问我哦！😊",
                        isFromUser = false
                    )
                )
            }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "清空对话",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Chat Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            state = listState,
            reverseLayout = false
        ) {
            items(uiMessages) { message ->
                ChatBubble(message = message)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quick Questions
            item {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickQuestions) { question ->
                        FilterChip(
                            selected = false,
                            onClick = { sendMessage(question) },
                            label = { Text(question) },
                            enabled = !isLoading,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = PinkLight.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("问问小萌...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) }),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { sendMessage(inputText) },
                enabled = !isLoading && inputText.isNotBlank(),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (!isLoading && inputText.isNotBlank()) PinkPrimary
                        else PinkLight
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PinkPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = if (inputText.isNotBlank()) Color.White else PinkPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: UiChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(LavenderPurple.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (message.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = PinkPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = "AI",
                        tint = PinkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Card(
                shape = RoundedCornerShape(
                    topStart = if (message.isFromUser) 16.dp else 4.dp,
                    topEnd = if (message.isFromUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        message.isFromUser -> PinkPrimary
                        message.isLoading -> PinkLight.copy(alpha = 0.5f)
                        message.isError -> Color(0xFFFFEBEE)
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        message.isFromUser -> Color.White
                        message.isLoading -> MaterialTheme.colorScheme.onSurfaceVariant
                        message.isError -> Color(0xFFC62828)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(12.dp)
                )
            }

            message.analysisCard?.let { card ->
                Spacer(modifier = Modifier.height(8.dp))
                AnalysisCardView(card = card)
            }
        }

        if (message.isFromUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PinkLight),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "😊", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun AnalysisCardView(card: AnalysisCard) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            card.items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (label == "最大支出") PinkPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PinkLight.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "💡 ${card.suggestion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AIScreenPreview() {
    MengJiZhangTheme {
        AIScreen()
    }
}
