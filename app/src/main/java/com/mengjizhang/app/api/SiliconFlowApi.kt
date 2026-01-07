package com.mengjizhang.app.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * 硅基流动 AI API 服务
 * 文档: https://docs.siliconflow.cn
 *
 * 支持：
 * - 文本对话（Qwen、DeepSeek等）
 * - 视觉理解（GLM-4.1V）- 用于账单识别
 * - OCR识别（DeepSeek-OCR）
 */
object SiliconFlowApi {

    private const val TAG = "SiliconFlowApi"
    private const val BASE_URL = "https://api.siliconflow.cn/v1"
    private const val API_KEY = "YOUR_SILICONFLOW_API_KEY"

    // 可用模型列表
    object Models {
        // 文本模型
        const val QWEN_7B = "Qwen/Qwen2.5-7B-Instruct"
        const val QWEN_14B = "Qwen/Qwen2.5-14B-Instruct"
        const val DEEPSEEK_V2 = "deepseek-ai/DeepSeek-V2.5"
        const val GLM_9B = "THUDM/glm-4-9b-chat"

        // 视觉模型（免费）
        const val GLM_4V_THINKING = "THUDM/GLM-4.1V-9B-Thinking"

        // OCR 模型（免费）
        const val DEEPSEEK_OCR = "deepseek-ai/DeepSeek-OCR"
    }

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    /**
     * 账单识别结果
     */
    data class ReceiptResult(
        val amount: Double?,           // 金额
        val merchant: String?,         // 商家名称
        val category: String?,         // 推断的分类
        val note: String?,             // 备注信息
        val rawText: String            // 原始识别文本
    )

    /**
     * 使用视觉模型识别账单/收据
     * @param bitmap 图片
     * @return 识别结果
     */
    suspend fun recognizeReceipt(bitmap: Bitmap): Result<ReceiptResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始识别账单...")

            // 将图片转为 Base64
            val imageBase64 = bitmapToBase64(bitmap)
            val imageUrl = "data:image/jpeg;base64,$imageBase64"

            Log.d(TAG, "图片已转换为 Base64, 长度: ${imageBase64.length}")

            // 构建多模态请求
            val contentList = listOf(
                mapOf("type" to "text", "text" to RECEIPT_PROMPT),
                mapOf(
                    "type" to "image_url",
                    "image_url" to mapOf("url" to imageUrl)
                )
            )

            val requestMap = mapOf(
                "model" to Models.GLM_4V_THINKING,
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to contentList
                    )
                ),
                "max_tokens" to 1024,
                "temperature" to 0.1
            )

            val jsonBody = gson.toJson(requestMap)
            Log.d(TAG, "发送请求到视觉模型...")

            val httpRequest = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "响应码: ${response.code}")

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errorResponse = gson.fromJson(responseBody, ErrorResponse::class.java)
                    errorResponse.error?.message ?: "请求失败: ${response.code}"
                } catch (e: Exception) {
                    responseBody ?: "请求失败: ${response.code}"
                }
                Log.e(TAG, "API 错误: $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
            val content = chatResponse.choices?.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("无识别结果"))

            Log.d(TAG, "AI 返回: $content")

            // 解析 AI 返回的响应
            val result = parseAIResponse(content)
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "识别失败", e)
            Result.failure(e)
        }
    }

    /**
     * 使用 DeepSeek-OCR 模型进行纯文字识别
     * @param bitmap 图片
     * @return 识别的文字内容
     */
    suspend fun recognizeText(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            val imageBase64 = bitmapToBase64(bitmap)
            val imageUrl = "data:image/jpeg;base64,$imageBase64"

            val contentList = listOf(
                mapOf("type" to "text", "text" to "请识别图片中的所有文字内容，保持原有格式。"),
                mapOf(
                    "type" to "image_url",
                    "image_url" to mapOf("url" to imageUrl)
                )
            )

            val requestMap = mapOf(
                "model" to Models.DEEPSEEK_OCR,
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to contentList
                    )
                ),
                "max_tokens" to 2048,
                "temperature" to 0.1
            )

            val jsonBody = gson.toJson(requestMap)

            val httpRequest = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("请求失败: ${response.code}"))
            }

            val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
            val content = chatResponse.choices?.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("无识别结果"))

            Result.success(content)

        } catch (e: Exception) {
            Log.e(TAG, "OCR 识别失败", e)
            Result.failure(e)
        }
    }

    /**
     * 发送聊天请求（文本）
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        model: String = Models.QWEN_7B,
        temperature: Float = 0.7f,
        maxTokens: Int = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = ChatRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                maxTokens = maxTokens,
                stream = false
            )

            val jsonBody = gson.toJson(request)
            Log.d(TAG, "Request: $jsonBody")

            val httpRequest = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Response code: ${response.code}")
            Log.d(TAG, "Response body: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                val content = chatResponse.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    Result.success(content)
                } else {
                    Result.failure(Exception("AI 返回内容为空"))
                }
            } else {
                val errorMsg = try {
                    val errorResponse = gson.fromJson(responseBody, ErrorResponse::class.java)
                    errorResponse.error?.message ?: "请求失败: ${response.code}"
                } catch (e: Exception) {
                    "请求失败: ${response.code}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "API request failed", e)
            Result.failure(e)
        }
    }

    /**
     * 简单聊天接口
     */
    suspend fun simpleChat(
        userMessage: String,
        systemPrompt: String? = null
    ): Result<String> {
        val messages = mutableListOf<ChatMessage>()

        if (systemPrompt != null) {
            messages.add(ChatMessage(role = "system", content = systemPrompt))
        }
        messages.add(ChatMessage(role = "user", content = userMessage))

        return chat(messages)
    }

    /**
     * 解析 AI 返回的响应
     */
    private fun parseAIResponse(content: String): ReceiptResult {
        // 尝试从响应中提取 JSON
        val jsonPattern = Regex("""\{[\s\S]*?"amount"[\s\S]*?\}""")
        val jsonMatch = jsonPattern.find(content)

        if (jsonMatch != null) {
            try {
                val parsed = gson.fromJson(jsonMatch.value, ParsedReceipt::class.java)
                Log.d(TAG, "解析到 JSON: amount=${parsed.amount}, merchant=${parsed.merchant}")
                return ReceiptResult(
                    amount = parsed.amount,
                    merchant = parsed.merchant,
                    category = parsed.category,
                    note = parsed.note,
                    rawText = content
                )
            } catch (e: Exception) {
                Log.w(TAG, "JSON 解析失败: ${e.message}")
            }
        }

        // 备用方案：使用正则从文本中提取金额
        // 优先匹配带货币符号的金额
        val currencyPattern = Regex("""[¥￥]\s*[-]?(\d+\.?\d*)""")
        val currencyMatch = currencyPattern.find(content)
        if (currencyMatch != null) {
            val amount = currencyMatch.groupValues[1].toDoubleOrNull()
            if (amount != null && amount in 0.01..100000.0) {
                Log.d(TAG, "从货币符号提取到金额: $amount")
                return ReceiptResult(
                    amount = amount,
                    merchant = null,
                    category = null,
                    note = null,
                    rawText = content
                )
            }
        }

        // 匹配"金额"、"合计"等关键词后的数字
        val keywordPattern = Regex("""(?:金额|合计|实付|应付|总计)[：:]*\s*[-]?(\d+\.?\d*)""")
        val keywordMatch = keywordPattern.find(content)
        if (keywordMatch != null) {
            val amount = keywordMatch.groupValues[1].toDoubleOrNull()
            if (amount != null && amount in 0.01..100000.0) {
                Log.d(TAG, "从关键词提取到金额: $amount")
                return ReceiptResult(
                    amount = amount,
                    merchant = null,
                    category = null,
                    note = null,
                    rawText = content
                )
            }
        }

        // 最后尝试提取合理范围内的金额
        val amountPattern = Regex("""[-]?(\d+\.\d{1,2})(?!\d)""")
        val amounts = amountPattern.findAll(content)
            .mapNotNull { it.groupValues[1].toDoubleOrNull() }
            .filter { it in 0.01..100000.0 }
            .toList()

        val amount = amounts.firstOrNull()
        Log.d(TAG, "备用方案提取到金额: $amount")

        return ReceiptResult(
            amount = amount,
            merchant = null,
            category = null,
            note = null,
            rawText = content
        )
    }

    /**
     * 将 Bitmap 转换为 Base64 字符串
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        var quality = 85
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        // 如果图片太大，降低质量
        while (outputStream.size() > 4 * 1024 * 1024 && quality > 20) {
            outputStream.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    // ========== 提示词 ==========

    private const val RECEIPT_PROMPT = """请仔细分析这张账单/收据/消费记录图片，提取以下信息：

1. **金额**：找出实际消费/交易金额
   - 注意区分：订单号、流水号、时间戳等长数字串不是金额
   - 优先查找"实付"、"应付"、"合计"、"总计"、"-¥"等字样后的金额
   - 金额通常是小数点后1-2位的数字
   - 负数金额表示支出，取其绝对值

2. **商家**：商家/店铺名称

3. **分类**：根据消费内容推断分类（餐饮/交通/购物/娱乐/居家/医疗/教育/其他）

4. **备注**：简要描述消费内容

请以JSON格式返回，示例：
{"amount": 9.52, "merchant": "美团外卖", "category": "餐饮", "note": "午餐"}

重要提示：
- amount 必须是数字（不带货币符号），表示消费金额
- 不要把订单号(如20260106...)当成金额
- 如果无法识别某项，该字段返回null"""

    private data class ParsedReceipt(
        val amount: Double?,
        val merchant: String?,
        val category: String?,
        val note: String?
    )
}

// ========== 请求/响应数据类 ==========

data class ChatMessage(
    val role: String,  // "system", "user", "assistant"
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    @SerializedName("max_tokens")
    val maxTokens: Int = 1024,
    val stream: Boolean = false
)

data class ChatResponse(
    val id: String?,
    val model: String?,
    val choices: List<Choice>?,
    val usage: Usage?
)

data class Choice(
    val index: Int?,
    val message: ChatMessage?,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int?,
    @SerializedName("completion_tokens")
    val completionTokens: Int?,
    @SerializedName("total_tokens")
    val totalTokens: Int?
)

data class ErrorResponse(
    val error: ErrorDetail?
)

data class ErrorDetail(
    val message: String?,
    val type: String?,
    val code: String?
)
