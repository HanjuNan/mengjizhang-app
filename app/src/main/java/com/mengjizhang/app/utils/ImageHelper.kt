package com.mengjizhang.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 图片处理工具类
 */
object ImageHelper {

    /**
     * 保存图片到应用私有目录
     * @return 保存后的文件路径
     */
    fun saveImage(context: Context, bitmap: Bitmap): String {
        val imagesDir = File(context.filesDir, "record_images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        val fileName = "img_${UUID.randomUUID()}.jpg"
        val file = File(imagesDir, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        return file.absolutePath
    }

    /**
     * 从 Uri 保存图片
     */
    fun saveImageFromUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // 压缩图片
                val resizedBitmap = resizeBitmap(bitmap, 1200)
                val path = saveImage(context, resizedBitmap)
                if (resizedBitmap != bitmap) {
                    resizedBitmap.recycle()
                }
                path
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 调整图片大小
     */
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 删除图片
     */
    fun deleteImage(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 加载图片
     */
    fun loadBitmap(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }
}
