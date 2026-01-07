package com.mengjizhang.app.api

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * 百度 OCR 文字识别 REST API
 *
 * 使用与语音识别相同的 API Key 和 Secret Key
 * 需要在百度控制台开通文字识别服务
 *
 * 免费额度: 每日 50,000 次通用文字识别
 */
object BaiduOcrApi {

    // 复用语音识别应用的凭证（同一应用可开通多个服务）
    object Config {
        val API_KEY: String get() = BaiduVoiceApi.Config.API_KEY
        val SECRET_KEY: String get() = BaiduVoiceApi.Config.SECRET_KEY

        fun isConfigured(): Boolean {
            return API_KEY.isNotEmpty() && SECRET_KEY.isNotEmpty()
        }
    }

    private const val TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token"
    // 通用文字识别（标准版）
    private const val OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // 缓存 access token
    private var cachedToken: String? = null
    private var tokenExpireTime: Long = 0

    /**
     * 获取 Access Token
     */
    private suspend fun getAccessToken(): Result<String> = withContext(Dispatchers.IO) {
        // 检查缓存的 token 是否有效
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return@withContext Result.success(cachedToken!!)
        }

        try {
            val formBody = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", Config.API_KEY)
                .add("client_secret", Config.SECRET_KEY)
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("空响应"))

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("获取 Token 失败: ${response.code}"))
            }

            val tokenResponse = gson.fromJson(body, TokenResponse::class.java)

            if (tokenResponse.accessToken != null) {
                cachedToken = tokenResponse.accessToken
                // Token 有效期通常是 30 天，我们提前 1 小时刷新
                tokenExpireTime = System.currentTimeMillis() + (tokenResponse.expiresIn - 3600) * 1000
                Result.success(tokenResponse.accessToken)
            } else {
                Result.failure(Exception("Token 响应异常: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 图片文字识别
     * @param bitmap 要识别的图片
     * @return 识别结果（所有文字拼接）
     */
    suspend fun recognize(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        if (!Config.isConfigured()) {
            return@withContext Result.failure(
                Exception("请先配置百度 API\n\n" +
                    "1. 打开 https://ai.baidu.com/\n" +
                    "2. 注册并创建文字识别应用\n" +
                    "3. 在 BaiduVoiceApi.kt 中填入 API_KEY 和 SECRET_KEY")
            )
        }

        // 获取 token
        val tokenResult = getAccessToken()
        if (tokenResult.isFailure) {
            return@withContext Result.failure(tokenResult.exceptionOrNull()!!)
        }
        val token = tokenResult.getOrNull()!!

        try {
            // 将图片压缩并转为 Base64
            val imageBase64 = bitmapToBase64(bitmap)

            // 构建请求
            val formBody = FormBody.Builder()
                .add("image", imageBase64)
                .add("language_type", "CHN_ENG")  // 中英文混合
                .add("detect_direction", "true")  // 检测方向
                .add("paragraph", "false")        // 不输出段落信息
                .add("probability", "false")      // 不返回置信度
                .build()

            val request = Request.Builder()
                .url("$OCR_URL?access_token=$token")
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("空响应"))

            val ocrResponse = gson.fromJson(body, OcrResponse::class.java)

            // 检查错误
            if (ocrResponse.errorCode != null && ocrResponse.errorCode != 0) {
                val errorMsg = when (ocrResponse.errorCode) {
                    1 -> "服务器内部错误"
                    2 -> "服务暂不可用"
                    3 -> "调用的API不存在"
                    4 -> "集群超限额"
                    17 -> "每天请求量超限额"
                    18 -> "QPS超限额"
                    19 -> "请求总量超限额"
                    100 -> "无效的access_token"
                    110 -> "access_token无效"
                    111 -> "access_token过期"
                    216100 -> "请求中包含非法参数"
                    216101 -> "缺少必须的参数"
                    216102 -> "请求了不支持的服务"
                    216103 -> "请求中某些参数过长"
                    216110 -> "appid不存在"
                    216200 -> "图片为空或格式错误"
                    216201 -> "图片格式不支持"
                    216202 -> "图片大小超限"
                    216630 -> "识别错误"
                    216631 -> "识别银行卡错误"
                    216633 -> "识别身份证错误"
                    216634 -> "检测错误"
                    282000 -> "内部错误"
                    282003 -> "请求参数缺失"
                    282005 -> "处理过程出错"
                    282006 -> "图片处理失败"
                    282114 -> "图片过大"
                    else -> "识别失败 (${ocrResponse.errorCode}): ${ocrResponse.errorMsg}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            // 拼接所有识别结果
            val words = ocrResponse.wordsResult?.mapNotNull { it.words } ?: emptyList()
            if (words.isEmpty()) {
                Result.failure(Exception("未识别到文字内容"))
            } else {
                Result.success(words.joinToString("\n"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 将 Bitmap 转换为 Base64 字符串
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // 压缩图片，百度OCR限制图片大小不超过4MB
        var quality = 90
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        // 如果图片太大，逐步降低质量
        while (outputStream.size() > 3 * 1024 * 1024 && quality > 20) {
            outputStream.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // ========== 数据类 ==========

    private data class TokenResponse(
        @SerializedName("access_token")
        val accessToken: String?,
        @SerializedName("expires_in")
        val expiresIn: Long = 0,
        @SerializedName("error")
        val error: String?,
        @SerializedName("error_description")
        val errorDescription: String?
    )

    private data class OcrResponse(
        @SerializedName("error_code")
        val errorCode: Int?,
        @SerializedName("error_msg")
        val errorMsg: String?,
        @SerializedName("words_result_num")
        val wordsResultNum: Int?,
        @SerializedName("words_result")
        val wordsResult: List<WordResult>?
    )

    private data class WordResult(
        val words: String?
    )
}
