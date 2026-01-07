package com.mengjizhang.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.mengjizhang.app.api.BaiduVoiceApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * 百度语音识别助手
 *
 * 功能：
 * 1. 录制音频（PCM 格式，16kHz，16bit，单声道）
 * 2. 调用百度语音识别 API
 * 3. 返回识别结果
 */
class BaiduVoiceRecognitionHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListening: (Boolean) -> Unit
) {
    // 音频参数（百度语音要求）
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // 音频数据缓存
    private var audioOutputStream: ByteArrayOutputStream? = null

    /**
     * 检查是否已配置 API
     */
    fun isConfigured(): Boolean {
        return BaiduVoiceApi.Config.isConfigured()
    }

    /**
     * 检查录音权限
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 开始录音
     */
    fun startListening() {
        if (isRecording) {
            stopListening()
            return
        }

        if (!hasPermission()) {
            onError("请授权录音权限")
            return
        }

        if (!isConfigured()) {
            onError("请先配置百度语音 API\n\n" +
                "1. 打开 https://ai.baidu.com/\n" +
                "2. 注册并创建语音识别应用\n" +
                "3. 在代码中配置 API Key")
            return
        }

        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                channelConfig,
                audioFormat
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("无法初始化录音器")
                return
            }

            audioOutputStream = ByteArrayOutputStream()
            audioRecord?.startRecording()
            isRecording = true
            onListening(true)

            // 开始录音线程
            recordingJob = coroutineScope.launch(Dispatchers.IO) {
                val buffer = ByteArray(bufferSize)
                var totalSize = 0
                val maxSize = sampleRate * 2 * 15 // 最多录制 15 秒

                while (isRecording && totalSize < maxSize) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        audioOutputStream?.write(buffer, 0, readSize)
                        totalSize += readSize
                    }
                }

                // 如果达到最大时长，自动停止
                if (totalSize >= maxSize) {
                    withContext(Dispatchers.Main) {
                        stopAndRecognize()
                    }
                }
            }
        } catch (e: SecurityException) {
            onError("录音权限被拒绝")
        } catch (e: Exception) {
            onError("录音启动失败: ${e.message}")
        }
    }

    /**
     * 停止录音并识别
     */
    fun stopListening() {
        if (!isRecording) return
        stopAndRecognize()
    }

    private fun stopAndRecognize() {
        isRecording = false
        recordingJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null

        val audioData = audioOutputStream?.toByteArray()
        audioOutputStream?.close()
        audioOutputStream = null

        onListening(false)

        // 检查音频数据
        if (audioData == null || audioData.size < 3200) { // 至少 0.1 秒的数据
            onError("录音时间太短，请重试")
            return
        }

        // 调用百度语音识别
        coroutineScope.launch {
            try {
                val result = BaiduVoiceApi.recognize(audioData)

                result.fold(
                    onSuccess = { text ->
                        if (text.isNotBlank()) {
                            onResult(text)
                        } else {
                            onError("未识别到语音内容")
                        }
                    },
                    onFailure = { error ->
                        onError(error.message ?: "识别失败")
                    }
                )
            } catch (e: Exception) {
                onError("识别异常: ${e.message}")
            }
        }
    }

    /**
     * 释放资源
     */
    fun destroy() {
        isRecording = false
        recordingJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null

        audioOutputStream?.close()
        audioOutputStream = null
    }
}
