package com.mengjizhang.app.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.mengjizhang.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object SupabaseClient {

    data class SupabaseSession(
        val accessToken: String,
        val refreshToken: String?,
        val userId: String,
        val email: String?
    )

    private const val STORAGE_BUCKET = "backups"

    private val gson = Gson()
    private val client = OkHttpClient()

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    private fun baseUrl(): HttpUrl = BuildConfig.SUPABASE_URL.trimEnd('/').toHttpUrl()

    private fun authUrl(): HttpUrl = baseUrl().newBuilder()
        .addPathSegments("auth/v1")
        .build()

    private fun storageUrl(): HttpUrl = baseUrl().newBuilder()
        .addPathSegments("storage/v1")
        .build()

    private fun requireConfigured(): Result<Unit> {
        if (!isConfigured) {
            return Result.failure(IllegalStateException("SUPABASE_URL / SUPABASE_ANON_KEY 未配置"))
        }
        return Result.success(Unit)
    }

    suspend fun signInWithPassword(email: String, password: String): Result<SupabaseSession> {
        return withContext(Dispatchers.IO) {
            requireConfigured().getOrElse { return@withContext Result.failure(it) }

            val url = authUrl().newBuilder()
                .addPathSegment("token")
                .addQueryParameter("grant_type", "password")
                .build()

            val payload = JsonObject().apply {
                addProperty("email", email)
                addProperty("password", password)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful || body.isNullOrBlank()) {
                        return@withContext Result.failure(Exception("登录失败: HTTP ${response.code}"))
                    }

                    val token = gson.fromJson(body, AuthTokenResponse::class.java)
                    if (token.accessToken.isNullOrBlank() || token.user?.id.isNullOrBlank()) {
                        val error = runCatching { gson.fromJson(body, AuthErrorResponse::class.java) }.getOrNull()
                        val msg = error?.errorDescription ?: error?.error ?: "登录失败"
                        return@withContext Result.failure(Exception(msg))
                    }

                    Result.success(
                        SupabaseSession(
                            accessToken = token.accessToken!!,
                            refreshToken = token.refreshToken,
                            userId = token.user!!.id!!,
                            email = token.user.email
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun signOut(accessToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            requireConfigured().getOrElse { return@withContext Result.failure(it) }

            val url = authUrl().newBuilder()
                .addPathSegment("logout")
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(ByteArray(0).toRequestBody(null))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) Result.success(Unit)
                    else Result.failure(Exception("登出失败: HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun uploadLatestBackup(session: SupabaseSession, bytes: ByteArray): Result<Unit> {
        return withContext(Dispatchers.IO) {
            requireConfigured().getOrElse { return@withContext Result.failure(it) }

            val url = storageUrl().newBuilder()
                .addPathSegments("object")
                .addPathSegment(STORAGE_BUCKET)
                .addPathSegment(session.userId)
                .addPathSegment("latest.zip")
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .addHeader("x-upsert", "true")
                .post(bytes.toRequestBody("application/octet-stream".toMediaType()))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) Result.success(Unit)
                    else {
                        val msg = response.body?.string()?.takeIf { it.isNotBlank() }
                            ?: "上传失败: HTTP ${response.code}"
                        Result.failure(Exception(msg))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun downloadLatestBackup(session: SupabaseSession): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            requireConfigured().getOrElse { return@withContext Result.failure(it) }

            val url = storageUrl().newBuilder()
                .addPathSegments("object/authenticated")
                .addPathSegment(STORAGE_BUCKET)
                .addPathSegment(session.userId)
                .addPathSegment("latest.zip")
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .get()
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val msg = response.body?.string()?.takeIf { it.isNotBlank() }
                            ?: "下载失败: HTTP ${response.code}"
                        return@withContext Result.failure(Exception(msg))
                    }

                    val bytes = response.body?.bytes()
                        ?: return@withContext Result.failure(Exception("下载失败: 空内容"))

                    Result.success(bytes)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private data class AuthTokenResponse(
        @SerializedName("access_token") val accessToken: String?,
        @SerializedName("refresh_token") val refreshToken: String?,
        val user: AuthUser?
    )

    private data class AuthUser(
        val id: String?,
        val email: String?
    )

    private data class AuthErrorResponse(
        val error: String?,
        @SerializedName("error_description") val errorDescription: String?
    )
}
