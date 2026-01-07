package com.mengjizhang.app.utils

import com.mengjizhang.app.api.ChatMessage
import com.mengjizhang.app.api.SiliconFlowApi
import com.mengjizhang.app.data.local.CategoryExpense
import com.mengjizhang.app.data.model.Record
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * AI 聊天助手
 * 使用硅基流动 API 提供智能分析和建议功能
 */
object AIChatHelper {

    // 系统提示词 - 定义AI助手的角色和能力
    private fun buildSystemPrompt(
        monthlyExpense: Double,
        monthlyIncome: Double,
        categoryStats: List<CategoryExpense>
    ): String {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)

        val categoryInfo = if (categoryStats.isNotEmpty()) {
            categoryStats.joinToString("\n") { stat ->
                val percent = if (monthlyExpense > 0) {
                    (stat.totalAmount / monthlyExpense * 100).roundToInt()
                } else 0
                "- ${stat.categoryName}: ¥${String.format("%.2f", stat.totalAmount)} (${percent}%)"
            }
        } else {
            "暂无消费记录"
        }

        return """
你是"萌记账"App的AI财务助手，名叫"小萌"。你的性格可爱、温暖、专业。

## 你的能力
1. 分析用户的消费习惯和支出结构
2. 提供个性化的理财建议和省钱技巧
3. 帮助用户制定预算计划
4. 解答日常理财问题
5. 给予情感支持和鼓励

## 用户当前财务数据（${year}年${month}月）
- 本月总支出: ¥${String.format("%.2f", monthlyExpense)}
- 本月总收入: ¥${String.format("%.2f", monthlyIncome)}
- 本月结余: ¥${String.format("%.2f", monthlyIncome - monthlyExpense)}
- 分类支出明细:
$categoryInfo

## 回复要求
1. 使用亲切可爱的语气，可以适当使用表情符号
2. 回复要简洁实用，不要过于冗长
3. 根据用户的实际财务数据给出针对性建议
4. 如果用户没有消费记录，鼓励他们开始记账
5. 不要编造用户没有的数据
6. 回复使用中文
        """.trimIndent()
    }

    /**
     * 使用 AI 回答问题（异步）
     */
    suspend fun askAI(
        userMessage: String,
        chatHistory: List<ChatMessage>,
        monthlyExpense: Double,
        monthlyIncome: Double,
        categoryStats: List<CategoryExpense>
    ): Result<String> {
        val systemPrompt = buildSystemPrompt(monthlyExpense, monthlyIncome, categoryStats)

        val messages = mutableListOf<ChatMessage>()
        messages.add(ChatMessage(role = "system", content = systemPrompt))

        // 添加历史对话（最多保留最近10轮）
        val recentHistory = chatHistory.takeLast(20)
        messages.addAll(recentHistory)

        // 添加当前用户消息
        messages.add(ChatMessage(role = "user", content = userMessage))

        return SiliconFlowApi.chat(messages)
    }

    // ========== 以下是本地分析功能（不需要网络） ==========

    data class AnalysisResult(
        val title: String,
        val items: List<Pair<String, String>>,
        val suggestion: String
    )

    /**
     * 本地分析月度消费情况
     */
    fun analyzeMonthlySpending(
        records: List<Record>,
        categoryStats: List<CategoryExpense>,
        monthlyExpense: Double,
        monthlyIncome: Double
    ): AnalysisResult {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val daysInMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val dailyAverage = if (daysInMonth > 0) monthlyExpense / daysInMonth else 0.0

        val topCategory = categoryStats.maxByOrNull { it.totalAmount }
        val topCategoryPercent = if (monthlyExpense > 0 && topCategory != null) {
            (topCategory.totalAmount / monthlyExpense * 100).roundToInt()
        } else 0

        val suggestion = generateSpendingSuggestion(topCategory, topCategoryPercent, dailyAverage)

        return AnalysisResult(
            title = "${month}月消费分析报告",
            items = listOf(
                "总支出" to "¥${String.format("%,.2f", monthlyExpense)}",
                "总收入" to "¥${String.format("%,.2f", monthlyIncome)}",
                "日均消费" to "¥${String.format("%,.2f", dailyAverage)}",
                "最大支出" to if (topCategory != null) {
                    "${topCategory.categoryName} ${topCategoryPercent}%"
                } else "暂无数据"
            ),
            suggestion = suggestion
        )
    }

    private fun generateSpendingSuggestion(
        topCategory: CategoryExpense?,
        percent: Int,
        dailyAverage: Double
    ): String {
        if (topCategory == null) {
            return "开始记录你的第一笔账单，我会帮你分析消费习惯哦~"
        }

        return when (topCategory.categoryName) {
            "餐饮" -> when {
                percent > 40 -> "餐饮支出占比较高(${percent}%)，建议每周自己做饭2-3次，预计每月可节省 ¥400-500！"
                percent > 25 -> "餐饮消费占比${percent}%，属于正常范围。可以关注一下优惠活动哦~"
                else -> "餐饮支出控制得很好！继续保持~"
            }
            "购物" -> when {
                percent > 35 -> "购物支出占比${percent}%，建议先列购物清单，避免冲动消费！"
                else -> "购物支出合理，可以考虑使用比价工具获取更多优惠~"
            }
            "交通" -> when {
                percent > 20 -> "交通费用占比${percent}%，可以考虑拼车或公共交通来节省开支~"
                else -> "交通支出控制得不错！"
            }
            "娱乐" -> when {
                percent > 25 -> "娱乐支出占比${percent}%，可以寻找一些免费的休闲活动来平衡~"
                else -> "适度的娱乐有益身心健康，继续保持！"
            }
            else -> "本月${topCategory.categoryName}支出占比最高(${percent}%)，可以适当关注这方面的开支哦~"
        }
    }

    /**
     * 检查是否需要显示分析卡片
     */
    fun shouldShowAnalysisCard(userMessage: String): Boolean {
        val keywords = listOf("分析", "报告", "统计", "花了多少", "消费多少", "支出")
        return keywords.any { userMessage.contains(it) }
    }
}
