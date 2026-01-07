package com.mengjizhang.app.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * 语音识别助手类
 * 封装 Android SpeechRecognizer 的使用
 *
 * 注意：Android 原生语音识别依赖 Google 服务，
 * 在国内设备或没有 Google Play Services 的设备上可能无法使用
 */
class VoiceRecognitionHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListening: (Boolean) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 检查设备是否支持语音识别
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * 开始语音识别
     */
    fun startListening() {
        // 确保在主线程执行
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startListening() }
            return
        }

        if (isListening) {
            stopListening()
            return
        }

        if (!isAvailable()) {
            onError("设备不支持语音识别\n\n可能原因：\n1. 没有安装 Google 服务\n2. 请尝试安装 Google 应用或 Google 语音服务")
            return
        }

        try {
            // 销毁旧的识别器
            speechRecognizer?.destroy()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

            if (speechRecognizer == null) {
                onError("无法创建语音识别服务")
                return
            }

            speechRecognizer?.setRecognitionListener(createRecognitionListener())

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // 设置中文
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // 增加超时时间
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            onListening(true)
        } catch (e: Exception) {
            onError("启动语音识别失败: ${e.message}")
            isListening = false
            onListening(false)
        }
    }

    /**
     * 停止语音识别
     */
    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore
            }
            isListening = false
            onListening(false)
        }
    }

    /**
     * 释放资源
     */
    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // Ignore
            }
            speechRecognizer = null
            isListening = false
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // 准备就绪，可以开始说话
            }

            override fun onBeginningOfSpeech() {
                // 检测到开始说话
            }

            override fun onRmsChanged(rmsdB: Float) {
                // 音量变化，可用于显示音量动画
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // 收到音频数据
            }

            override fun onEndOfSpeech() {
                // 说话结束
                isListening = false
                onListening(false)
            }

            override fun onError(error: Int) {
                isListening = false
                onListening(false)

                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "录音错误，请检查麦克风权限"
                    SpeechRecognizer.ERROR_CLIENT -> "客户端错误，请重试"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足，请授权录音权限"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误，语音识别需要网络连接"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时，请检查网络连接"
                    SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音内容，请重试"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务繁忙，请稍后重试"
                    SpeechRecognizer.ERROR_SERVER -> "服务器错误，请稍后重试"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到语音输入，请对着麦克风说话"
                    else -> "语音识别失败（错误码: $error）"
                }
                onError(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                onListening(false)

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                } else {
                    onError("未能识别语音内容")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    // 部分结果，可以用于实时显示
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // 其他事件
            }
        }
    }
}

/**
 * 解析语音输入中的金额和类别
 * 例如: "午餐花了35块" -> 金额35，分类餐饮
 * 支持中文数字: "三十五元" -> 金额35
 */
object VoiceParser {

    data class ParseResult(
        val amount: Double?,
        val categoryKeyword: String?,
        val note: String
    )

    // 中文数字映射
    private val chineseDigits = mapOf(
        '零' to 0, '一' to 1, '二' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9,
        '两' to 2, '〇' to 0
    )

    // 金额匹配模式 - 阿拉伯数字
    private val amountPatterns = listOf(
        Regex("""(\d+(?:\.\d{1,2})?)\s*[块元钱]"""),
        Regex("""花[了费]?\s*(\d+(?:\.\d{1,2})?)"""),
        Regex("""消费[了]?\s*(\d+(?:\.\d{1,2})?)"""),
        Regex("""支出[了]?\s*(\d+(?:\.\d{1,2})?)"""),
        Regex("""收入[了]?\s*(\d+(?:\.\d{1,2})?)"""),
        Regex("""赚[了]?\s*(\d+(?:\.\d{1,2})?)"""),
        Regex("""入账[了]?\s*(\d+(?:\.\d{1,2})?)"""),
        Regex("""(\d+(?:\.\d{1,2})?)\s*[块元钱]?$"""),
        Regex("""^(\d+(?:\.\d{1,2})?)$""")
    )

    // 中文数字金额匹配模式
    private val chineseAmountPattern = Regex("""([零一二三四五六七八九两〇十百千万]+)[块元钱]""")
    private val chineseAmountPattern2 = Regex("""花[了费]?\s*([零一二三四五六七八九两〇十百千万]+)""")
    private val chineseAmountPattern3 = Regex("""消费[了]?\s*([零一二三四五六七八九两〇十百千万]+)""")

    // 支出类别关键词映射
    private val expenseCategoryKeywords = mapOf(
        "餐饮" to listOf("吃饭", "午餐", "晚餐", "早餐", "早饭", "午饭", "晚饭", "外卖", "餐厅",
            "饭", "菜", "食", "饮料", "咖啡", "奶茶", "零食", "水果", "夜宵", "点餐", "下馆子"),
        "交通" to listOf("打车", "地铁", "公交", "出租车", "滴滴", "加油", "停车", "高铁",
            "火车", "飞机", "机票", "车票", "油费", "过路费", "通勤"),
        "购物" to listOf("买", "购物", "衣服", "鞋子", "包", "超市", "商场", "网购",
            "淘宝", "京东", "拼多多", "日用品", "化妆品"),
        "娱乐" to listOf("电影", "游戏", "KTV", "唱歌", "玩", "旅游", "门票", "演唱会",
            "音乐", "视频会员", "直播", "娱乐"),
        "居家" to listOf("水电", "煤气", "房租", "物业", "维修", "家具", "电器", "水费",
            "电费", "燃气", "暖气", "网费", "话费"),
        "医疗" to listOf("医院", "药", "看病", "挂号", "体检", "医疗", "门诊", "住院"),
        "教育" to listOf("学费", "课程", "培训", "书", "学习", "教育", "考试", "资料"),
        "其他" to listOf()
    )

    // 收入类别关键词映射
    private val incomeCategoryKeywords = mapOf(
        "工资" to listOf("工资", "薪水", "薪资", "发工资", "月薪", "底薪"),
        "奖金" to listOf("奖金", "年终奖", "绩效", "提成", "分红"),
        "理财" to listOf("理财", "利息", "收益", "股票", "基金", "投资"),
        "红包" to listOf("红包", "转账", "收款", "微信红包", "支付宝红包"),
        "其他" to listOf("收入", "入账", "进账", "赚")
    )

    /**
     * 将中文数字转换为阿拉伯数字
     * 支持: 一、二、三...十、百、千、万
     */
    private fun chineseToNumber(chinese: String): Double? {
        if (chinese.isEmpty()) return null

        var result = 0.0
        var temp = 0.0
        var section = 0.0

        for (char in chinese) {
            when (char) {
                '十' -> {
                    if (temp == 0.0) temp = 1.0
                    temp *= 10
                }
                '百' -> {
                    if (temp == 0.0) temp = 1.0
                    temp *= 100
                }
                '千' -> {
                    if (temp == 0.0) temp = 1.0
                    temp *= 1000
                }
                '万' -> {
                    section = (section + temp) * 10000
                    temp = 0.0
                }
                else -> {
                    val digit = chineseDigits[char]
                    if (digit != null) {
                        if (temp > 0) {
                            section += temp
                        }
                        temp = digit.toDouble()
                    }
                }
            }
        }

        result = section + temp
        return if (result > 0) result else null
    }

    fun parse(text: String): ParseResult {
        var amount: Double? = null
        var categoryKeyword: String? = null

        // 1. 先尝试匹配阿拉伯数字金额
        for (pattern in amountPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                amount = match.groupValues[1].toDoubleOrNull()
                if (amount != null) break
            }
        }

        // 2. 如果没找到，尝试匹配中文数字金额
        if (amount == null) {
            val chinesePatterns = listOf(chineseAmountPattern, chineseAmountPattern2, chineseAmountPattern3)
            for (pattern in chinesePatterns) {
                val match = pattern.find(text)
                if (match != null) {
                    amount = chineseToNumber(match.groupValues[1])
                    if (amount != null) break
                }
            }
        }

        // 3. 判断是收入还是支出，并识别类别
        val isIncome = text.contains("收入") || text.contains("入账") ||
                       text.contains("工资") || text.contains("奖金") ||
                       text.contains("红包") || text.contains("赚")

        val categoryMap = if (isIncome) incomeCategoryKeywords else expenseCategoryKeywords

        for ((category, keywords) in categoryMap) {
            for (keyword in keywords) {
                if (text.contains(keyword)) {
                    categoryKeyword = category
                    break
                }
            }
            if (categoryKeyword != null) break
        }

        return ParseResult(
            amount = amount,
            categoryKeyword = categoryKeyword,
            note = text
        )
    }
}
