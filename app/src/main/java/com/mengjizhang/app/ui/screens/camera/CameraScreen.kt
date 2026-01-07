package com.mengjizhang.app.ui.screens.camera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.mengjizhang.app.ui.theme.PinkLight
import com.mengjizhang.app.ui.theme.PinkPrimary
import com.mengjizhang.app.utils.ImageHelper
import com.mengjizhang.app.utils.OcrHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 拍照扫描页面
 * @param mode "camera" 拍照模式, "gallery" 相册选择模式
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    mode: String = "camera",
    onBack: () -> Unit,
    onResult: (amount: Double?, category: String, note: String, imagePath: String?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var ocrResult by remember { mutableStateOf<OcrHelper.ReceiptInfo?>(null) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    // 处理图片的通用函数 - 使用智能视觉模型识别
    fun processImage(bitmap: Bitmap, savedImagePath: String?) {
        scope.launch {
            try {
                // 使用视觉语言模型智能识别账单
                val result = withContext(Dispatchers.IO) {
                    OcrHelper.recognizeReceipt(bitmap)
                }

                result.onSuccess { receiptInfo ->
                    ocrResult = receiptInfo

                    // 构建备注：商家 + 商品信息
                    val noteBuilder = StringBuilder()
                    receiptInfo.merchant?.let { noteBuilder.append(it) }
                    receiptInfo.note?.let {
                        if (noteBuilder.isNotEmpty()) noteBuilder.append(" - ")
                        noteBuilder.append(it)
                    }
                    val finalNote = noteBuilder.toString().ifEmpty { "扫描录入" }

                    // 返回识别结果，包含图片路径
                    // onResult 回调中已包含导航返回逻辑，不需要再调用 onBack
                    onResult(
                        receiptInfo.totalAmount,
                        receiptInfo.category ?: "其他",
                        finalNote,
                        savedImagePath
                    )

                    Toast.makeText(
                        context,
                        "识别成功！金额: ¥${receiptInfo.totalAmount ?: "未识别"}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // 注意：不要调用 onBack()，因为 onResult 回调中已经 popBackStack 了
                }.onFailure { e ->
                    Log.e("CameraScreen", "OCR failed", e)
                    Toast.makeText(
                        context,
                        "识别失败: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("CameraScreen", "OCR failed", e)
                Toast.makeText(
                    context,
                    "识别失败，请重试",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isProcessing = false
            }
        }
    }

    // 相册选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isProcessing = true
            scope.launch {
                try {
                    // 保存图片到应用目录
                    val savedPath = withContext(Dispatchers.IO) {
                        ImageHelper.saveImageFromUri(context, it)
                    }

                    // 加载图片进行OCR
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        processImage(bitmap, savedPath)
                    } else {
                        Toast.makeText(context, "无法读取图片", Toast.LENGTH_SHORT).show()
                        isProcessing = false
                    }
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Gallery image processing failed", e)
                    Toast.makeText(context, "图片处理失败", Toast.LENGTH_SHORT).show()
                    isProcessing = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // 如果是相册模式，直接打开相册选择器
    LaunchedEffect(mode) {
        if (mode == "gallery") {
            galleryLauncher.launch("image/*")
        }
    }

    LaunchedEffect(Unit) {
        if (mode == "camera" && !cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setFlashMode(
                                if (flashEnabled) ImageCapture.FLASH_MODE_ON
                                else ImageCapture.FLASH_MODE_OFF
                            )
                            .build()

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "Use case binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            // Overlay UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "闪光灯",
                            tint = if (flashEnabled) PinkPrimary else Color.White
                        )
                    }
                }

                // Guide text
                Spacer(modifier = Modifier.height(40.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "将小票/收据放入框内\n自动识别金额信息",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Capture Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(72.dp),
                            color = PinkPrimary,
                            strokeWidth = 4.dp
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 相册按钮
                            IconButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "相册",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // 拍照按钮
                            IconButton(
                                onClick = {
                                    imageCapture?.let { capture ->
                                        isProcessing = true
                                        takePhoto(
                                            context = context,
                                            imageCapture = capture,
                                            executor = cameraExecutor,
                                            onPhotoTaken = { bitmap, savedPath ->
                                                processImage(bitmap, savedPath)
                                            },
                                            onError = { error ->
                                                isProcessing = false
                                                Toast.makeText(
                                                    context,
                                                    "拍照失败: $error",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(PinkPrimary)
                            ) {
                                Icon(
                                    Icons.Default.Camera,
                                    contentDescription = "拍照",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Permission not granted
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📷",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (cameraPermissionState.status.shouldShowRationale) {
                        "需要相机权限来扫描小票"
                    } else {
                        "请在设置中允许相机权限"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PinkLight)
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text("返回", color = PinkPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onPhotoTaken: (Bitmap, String?) -> Unit,
    onError: (String) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        "photo_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                try {
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    if (bitmap != null) {
                        // 保存图片到应用目录
                        val savedPath = ImageHelper.saveImage(context, bitmap)
                        onPhotoTaken(bitmap, savedPath)
                    } else {
                        onError("无法读取图片")
                    }
                    // 删除临时文件
                    photoFile.delete()
                } catch (e: Exception) {
                    onError(e.message ?: "处理图片失败")
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "拍照失败")
            }
        }
    )
}
