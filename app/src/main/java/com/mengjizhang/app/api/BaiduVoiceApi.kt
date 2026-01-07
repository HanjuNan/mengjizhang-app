package com.mengjizhang.app.api

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 百度语音识别 REST API
 *
 * 使用前需要：
 * 1. 在 https://ai.baidu.com/ 注册账号
 * 2. 创建语音识别应用
 * 3. 获取 API Key 和 Secret Key
 * 4. 在 BaiduVoiceConfig 中配置
 */
object BaiduVoiceApi {

    // ========== 配置区域 - 百度 API 凭证 ==========
    object Config {
        const val API_KEY = "YOUR_BAIDU_API_KEY"
        const val SECRET_KEY = "YOUR_BAIDU_SECRET_KEY"

        // 是否已配置（用于检查）
        fun isConfigured(): Boolean {
            return API_KEY.isNotEmpty() && SECRET_KEY.isNotEmpty()
        }
    }
    // ========================================================

    private const val TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token"
    private const val ASR_URL = "https://vop.baidu.com/server_api"

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
     * 语音识别
     * @param audioData PCM 音频数据（16k 采样率，16bit，单声道）
     * @return 识别结果文本
     */
    suspend fun recognize(audioData: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        if (!Config.isConfigured()) {
            return@withContext Result.failure(
                Exception("请先配置百度语音 API\n\n" +
                    "1. 打开 https://ai.baidu.com/\n" +
                    "2. 注册并创建语音识别应用\n" +
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
            // 将音频数据转为 Base64
            val audioBase64 = Base64.encodeToString(audioData, Base64.NO_WRAP)

            // 构建请求体
            val requestBody = AsrRequest(
                format = "pcm",
                rate = 16000,
                channel = 1,
                cuid = "mengjizhang_app",
                token = token,
                speech = audioBase64,
                len = audioData.size
            )

            val jsonBody = gson.toJson(requestBody)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(ASR_URL)
                .post(jsonBody)
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("空响应"))

            val asrResponse = gson.fromJson(body, AsrResponse::class.java)

            when (asrResponse.errNo) {
                0 -> {
                    val result = asrResponse.result?.firstOrNull() ?: ""
                    Result.success(result)
                }
                3301 -> Result.failure(Exception("音频质量过差，请重试"))
                3302 -> Result.failure(Exception("鉴权失败，请检查 API 配置"))
                3303 -> Result.failure(Exception("语音识别服务繁忙"))
                3304 -> Result.failure(Exception("请求超限，请稍后重试"))
                3305 -> Result.failure(Exception("音频过长，请控制在60秒以内"))
                else -> Result.failure(Exception("识别失败: ${asrResponse.errMsg} (${asrResponse.errNo})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    private data class AsrRequest(
        val format: String,
        val rate: Int,
        val channel: Int,
        val cuid: String,
        val token: String,
        val speech: String,
        val len: Int
    )

    private data class AsrResponse(
        @SerializedName("err_no")
        val errNo: Int = 0,
        @SerializedName("err_msg")
        val errMsg: String?,
        val result: List<String>?
    )
}
