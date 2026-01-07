package com.mengjizhang.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.mengjizhang.app.data.model.Record
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据导出工具类
 * 支持 CSV、Excel、PDF 导出以及数据备份恢复
 */
object ExportHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)

    /**
     * 获取导出目录
     */
    private fun getExportDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 获取备份目录
     */
    private fun getBackupDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 导出为 CSV 格式
     */
    fun exportToCsv(context: Context, records: List<Record>): Result<File> {
        return try {
            val fileName = "萌记账_${fileNameDateFormat.format(Date())}.csv"
            val file = File(getExportDir(context), fileName)

            FileWriter(file).use { writer ->
                // 写入 BOM 以支持 Excel 中文显示
                writer.write("\uFEFF")
                // 写入表头
                writer.write("日期,类型,分类,金额,备注,录入方式\n")

                // 写入数据
                records.forEach { record ->
                    val date = dateFormat.format(Date(record.date))
                    val type = if (record.isExpense) "支出" else "收入"
                    val amount = String.format("%.2f", record.amount)
                    val note = record.note.replace(",", "，").replace("\n", " ")
                    val inputMethod = when (record.inputMethod) {
                        "voice" -> "语音"
                        "camera" -> "拍照"
                        else -> "手动"
                    }

                    writer.write("$date,$type,${record.categoryName},$amount,$note,$inputMethod\n")
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 导出为 Excel 格式 (使用 HTML 表格，Excel 可完美打开)
     */
    fun exportToExcel(context: Context, records: List<Record>): Result<File> {
        return try {
            val fileName = "萌记账_${fileNameDateFormat.format(Date())}.xls"
            val file = File(getExportDir(context), fileName)

            FileWriter(file).use { writer ->
                // HTML 表格格式，Excel 可以直接打开
                writer.write("""
                    <html xmlns:o="urn:schemas-microsoft-com:office:office"
                          xmlns:x="urn:schemas-microsoft-com:office:excel"
                          xmlns="http://www.w3.org/TR/REC-html40">
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            table { border-collapse: collapse; width: 100%; }
                            th {
                                background-color: #FF6B9D;
                                color: white;
                                font-weight: bold;
                                padding: 10px;
                                border: 1px solid #ddd;
                            }
                            td {
                                padding: 8px;
                                border: 1px solid #ddd;
                                text-align: center;
                            }
                            .expense { color: #F44336; }
                            .income { color: #4CAF50; }
                            tr:nth-child(even) { background-color: #f9f9f9; }
                        </style>
                    </head>
                    <body>
                        <h2 style="color: #FF6B9D;">萌记账 - 账单导出</h2>
                        <p>导出时间: ${dateFormat.format(Date())}</p>
                        <table>
                            <tr>
                                <th>日期</th>
                                <th>类型</th>
                                <th>分类</th>
                                <th>金额</th>
                                <th>备注</th>
                                <th>录入方式</th>
                            </tr>
                """.trimIndent())

                // 写入数据
                records.forEach { record ->
                    val date = dateFormat.format(Date(record.date))
                    val type = if (record.isExpense) "支出" else "收入"
                    val category = "${record.categoryEmoji} ${record.categoryName}"
                    val amountClass = if (record.isExpense) "expense" else "income"
                    val prefix = if (record.isExpense) "-" else "+"
                    val amount = "$prefix¥${String.format("%.2f", record.amount)}"
                    val note = record.note.replace("<", "&lt;").replace(">", "&gt;")
                    val inputMethod = when (record.inputMethod) {
                        "voice" -> "语音"
                        "camera" -> "拍照"
                        else -> "手动"
                    }

                    writer.write("""
                        <tr>
                            <td>$date</td>
                            <td>$type</td>
                            <td>$category</td>
                            <td class="$amountClass">$amount</td>
                            <td>$note</td>
                            <td>$inputMethod</td>
                        </tr>
                    """.trimIndent())
                }

                // 汇总行
                val totalIncome = records.filter { !it.isExpense }.sumOf { it.amount }
                val totalExpense = records.filter { it.isExpense }.sumOf { it.amount }
                val balance = totalIncome - totalExpense

                writer.write("""
                        </table>
                        <br/>
                        <table>
                            <tr>
                                <td><b>总收入</b></td>
                                <td class="income">+¥${String.format("%.2f", totalIncome)}</td>
                            </tr>
                            <tr>
                                <td><b>总支出</b></td>
                                <td class="expense">-¥${String.format("%.2f", totalExpense)}</td>
                            </tr>
                            <tr>
                                <td><b>结余</b></td>
                                <td class="${if (balance >= 0) "income" else "expense"}">¥${String.format("%.2f", balance)}</td>
                            </tr>
                        </table>
                    </body>
                    </html>
                """.trimIndent())
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 导出为 PDF 格式
     */
    fun exportToPdf(context: Context, records: List<Record>, summary: ExportSummary): Result<File> {
        return try {
            val fileName = "萌记账_${fileNameDateFormat.format(Date())}.pdf"
            val file = File(getExportDir(context), fileName)

            val document = PdfDocument()
            val pageWidth = 595 // A4 宽度
            val pageHeight = 842 // A4 高度
            var pageNumber = 1
            var yPosition = 80f

            val titlePaint = Paint().apply {
                textSize = 24f
                color = Color.parseColor("#FF6B9D")
                isFakeBoldText = true
            }

            val headerPaint = Paint().apply {
                textSize = 14f
                color = Color.parseColor("#FF6B9D")
                isFakeBoldText = true
            }

            val textPaint = Paint().apply {
                textSize = 12f
                color = Color.BLACK
            }

            val summaryPaint = Paint().apply {
                textSize = 14f
                color = Color.DKGRAY
            }

            val expensePaint = Paint().apply {
                textSize = 12f
                color = Color.parseColor("#F44336")
            }

            val incomePaint = Paint().apply {
                textSize = 12f
                color = Color.parseColor("#4CAF50")
            }

            var currentPage: PdfDocument.Page? = null

            fun createNewPage(): Canvas {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                currentPage = document.startPage(pageInfo)
                pageNumber++
                yPosition = 80f
                return currentPage!!.canvas
            }

            var canvas = createNewPage()

            // 标题
            canvas.drawText("萌记账 - 账单报表", 40f, yPosition, titlePaint)
            yPosition += 30f

            // 导出日期
            canvas.drawText("导出时间: ${dateFormat.format(Date())}", 40f, yPosition, textPaint)
            yPosition += 40f

            // 汇总信息
            canvas.drawText("📊 账单汇总", 40f, yPosition, headerPaint)
            yPosition += 25f
            canvas.drawText("记录数量: ${summary.totalCount} 笔", 60f, yPosition, summaryPaint)
            yPosition += 20f
            canvas.drawText("总收入: ¥${String.format("%.2f", summary.totalIncome)}", 60f, yPosition, incomePaint)
            yPosition += 20f
            canvas.drawText("总支出: ¥${String.format("%.2f", summary.totalExpense)}", 60f, yPosition, expensePaint)
            yPosition += 20f
            val balance = summary.totalIncome - summary.totalExpense
            val balancePaint = if (balance >= 0) incomePaint else expensePaint
            canvas.drawText("结余: ¥${String.format("%.2f", balance)}", 60f, yPosition, balancePaint)
            yPosition += 40f

            // 明细标题
            canvas.drawText("📝 账单明细", 40f, yPosition, headerPaint)
            yPosition += 30f

            // 表头
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }
            canvas.drawLine(40f, yPosition, pageWidth - 40f, yPosition, linePaint)
            yPosition += 5f

            canvas.drawText("日期", 45f, yPosition + 15f, textPaint)
            canvas.drawText("分类", 150f, yPosition + 15f, textPaint)
            canvas.drawText("金额", 280f, yPosition + 15f, textPaint)
            canvas.drawText("备注", 380f, yPosition + 15f, textPaint)
            yPosition += 25f
            canvas.drawLine(40f, yPosition, pageWidth - 40f, yPosition, linePaint)
            yPosition += 15f

            // 数据行
            val shortDateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
            for (record in records) {
                if (yPosition > pageHeight - 60) {
                    currentPage?.let { document.finishPage(it) }
                    canvas = createNewPage()
                    yPosition = 60f
                }

                val date = shortDateFormat.format(Date(record.date))
                val category = "${record.categoryEmoji}${record.categoryName}"
                val amount = if (record.isExpense) "-¥${String.format("%.2f", record.amount)}"
                            else "+¥${String.format("%.2f", record.amount)}"
                val note = if (record.note.length > 12) record.note.take(12) + "..." else record.note

                canvas.drawText(date, 45f, yPosition, textPaint)
                canvas.drawText(category, 150f, yPosition, textPaint)
                canvas.drawText(amount, 280f, yPosition, if (record.isExpense) expensePaint else incomePaint)
                canvas.drawText(note, 380f, yPosition, textPaint)

                yPosition += 22f
            }

            currentPage?.let { document.finishPage(it) }

            // 写入文件
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 备份数据库
     */
    fun backupDatabase(context: Context): Result<File> {
        return try {
            val dbFile = context.getDatabasePath("mengjizhang_database")
            if (!dbFile.exists()) {
                return Result.failure(Exception("数据库文件不存在"))
            }

            val fileName = "backup_${fileNameDateFormat.format(Date())}.db"
            val backupFile = File(getBackupDir(context), fileName)

            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 恢复数据库
     */
    fun restoreDatabase(context: Context, backupUri: Uri): Result<Unit> {
        return try {
            val dbFile = context.getDatabasePath("mengjizhang_database")

            context.contentResolver.openInputStream(backupUri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("无法读取备份文件"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取所有备份文件
     */
    fun getBackupFiles(context: Context): List<File> {
        val backupDir = getBackupDir(context)
        return backupDir.listFiles()
            ?.filter { it.extension == "db" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * 删除备份文件
     */
    fun deleteBackup(file: File): Boolean {
        return file.delete()
    }

    /**
     * 分享文件
     */
    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "分享到"))
    }

    /**
     * 获取文件 MIME 类型
     */
    fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "csv" -> "text/csv"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "pdf" -> "application/pdf"
            "db" -> "application/octet-stream"
            else -> "*/*"
        }
    }
}

/**
 * 导出汇总信息
 */
data class ExportSummary(
    val totalCount: Int,
    val totalIncome: Double,
    val totalExpense: Double
)
