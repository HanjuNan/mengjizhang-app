package com.mengjizhang.app.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mengjizhang.app.api.SupabaseClient
import com.mengjizhang.app.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class CloudSyncViewModel(application: Application) : AndroidViewModel(application) {

    sealed class SyncState {
        data object Idle : SyncState()
        data object Loading : SyncState()
        data class Success(val message: String) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    private val prefs = application.getSharedPreferences("cloud_sync", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow<SupabaseClient.SupabaseSession?>(null)
    val session: StateFlow<SupabaseClient.SupabaseSession?> = _session.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val isConfigured: Boolean get() = SupabaseClient.isConfigured

    init {
        loadSession()
    }

    private fun loadSession() {
        val accessToken = prefs.getString("access_token", null)
        val refreshToken = prefs.getString("refresh_token", null)
        val userId = prefs.getString("user_id", null)
        val email = prefs.getString("email", null)

        if (!accessToken.isNullOrBlank() && !userId.isNullOrBlank()) {
            _session.value = SupabaseClient.SupabaseSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = userId,
                email = email
            )
        }
    }

    private fun saveSession(session: SupabaseClient.SupabaseSession?) {
        prefs.edit().apply {
            if (session != null) {
                putString("access_token", session.accessToken)
                putString("refresh_token", session.refreshToken)
                putString("user_id", session.userId)
                putString("email", session.email)
            } else {
                clear()
            }
            apply()
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            SupabaseClient.signInWithPassword(email, password)
                .onSuccess { sess ->
                    _session.value = sess
                    saveSession(sess)
                    _syncState.value = SyncState.Success("登录成功")
                }
                .onFailure { e ->
                    _syncState.value = SyncState.Error(e.message ?: "登录失败")
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            val sess = _session.value ?: return@launch
            _syncState.value = SyncState.Loading
            SupabaseClient.signOut(sess.accessToken)
            _session.value = null
            saveSession(null)
            _syncState.value = SyncState.Success("已退出登录")
        }
    }

    fun uploadBackup() {
        viewModelScope.launch {
            val sess = _session.value
            if (sess == null) {
                _syncState.value = SyncState.Error("请先登录")
                return@launch
            }

            _syncState.value = SyncState.Loading

            val context = getApplication<Application>()
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            if (!dbFile.exists()) {
                _syncState.value = SyncState.Error("本地数据库不存在")
                return@launch
            }

            try {
                val zipBytes = withContext(Dispatchers.IO) {
                    // 关闭数据库并执行 checkpoint，确保所有数据都写入主数据库文件
                    AppDatabase.closeDatabaseForBackup()

                    // 打包数据库和图片文件夹为 ZIP
                    val imagesDir = File(context.filesDir, "record_images")
                    createBackupZip(dbFile, imagesDir)
                }

                SupabaseClient.uploadLatestBackup(sess, zipBytes)
                    .onSuccess {
                        _syncState.value = SyncState.Success("备份上传成功")
                    }
                    .onFailure { e ->
                        _syncState.value = SyncState.Error(e.message ?: "上传失败")
                    }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error("创建备份失败: ${e.message}")
            }
        }
    }

    /**
     * 创建备份 ZIP 文件
     */
    private fun createBackupZip(dbFile: File, imagesDir: File): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        ZipOutputStream(BufferedOutputStream(byteArrayOutputStream)).use { zipOut ->
            // 添加数据库文件
            addFileToZip(zipOut, dbFile, "database.db")

            // 添加图片文件夹
            if (imagesDir.exists() && imagesDir.isDirectory) {
                imagesDir.listFiles()?.forEach { imageFile ->
                    if (imageFile.isFile) {
                        addFileToZip(zipOut, imageFile, "record_images/${imageFile.name}")
                    }
                }
            }
        }
        return byteArrayOutputStream.toByteArray()
    }

    /**
     * 将文件添加到 ZIP
     */
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        FileInputStream(file).use { fis ->
            BufferedInputStream(fis).use { bis ->
                val entry = ZipEntry(entryName)
                zipOut.putNextEntry(entry)
                bis.copyTo(zipOut)
                zipOut.closeEntry()
            }
        }
    }

    fun downloadAndRestore() {
        viewModelScope.launch {
            val sess = _session.value
            if (sess == null) {
                _syncState.value = SyncState.Error("请先登录")
                return@launch
            }

            _syncState.value = SyncState.Loading

            SupabaseClient.downloadLatestBackup(sess)
                .onSuccess { zipBytes ->
                    val context = getApplication<Application>()
                    val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
                    val walFile = File(dbFile.path + "-wal")
                    val shmFile = File(dbFile.path + "-shm")
                    val imagesDir = File(context.filesDir, "record_images")

                    try {
                        withContext(Dispatchers.IO) {
                            // 关闭数据库连接
                            AppDatabase.closeDatabase()

                            // 删除 WAL 和 SHM 文件
                            walFile.delete()
                            shmFile.delete()

                            // 解压 ZIP 文件
                            extractBackupZip(zipBytes, dbFile, imagesDir)
                        }
                        _syncState.value = SyncState.Success("恢复成功，请重启应用")
                    } catch (e: Exception) {
                        _syncState.value = SyncState.Error("恢复失败: ${e.message}")
                    }
                }
                .onFailure { e ->
                    _syncState.value = SyncState.Error(e.message ?: "下载失败")
                }
        }
    }

    /**
     * 解压备份 ZIP 文件
     */
    private fun extractBackupZip(zipBytes: ByteArray, dbFile: File, imagesDir: File) {
        // 确保图片目录存在
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        ByteArrayInputStream(zipBytes).use { bais ->
            ZipInputStream(BufferedInputStream(bais)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "database.db" -> {
                            // 恢复数据库文件
                            FileOutputStream(dbFile).use { fos ->
                                zipIn.copyTo(fos)
                            }
                        }
                        entry.name.startsWith("record_images/") && !entry.isDirectory -> {
                            // 恢复图片文件
                            val fileName = entry.name.substringAfter("record_images/")
                            val imageFile = File(imagesDir, fileName)
                            FileOutputStream(imageFile).use { fos ->
                                zipIn.copyTo(fos)
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }
    }

    fun clearState() {
        _syncState.value = SyncState.Idle
    }
}
