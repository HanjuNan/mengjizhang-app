package com.mengjizhang.app.ui.screens.export

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mengjizhang.app.ui.theme.ExpenseRed
import com.mengjizhang.app.ui.theme.IncomeGreen
import com.mengjizhang.app.ui.theme.LavenderPurple
import com.mengjizhang.app.ui.theme.MintGreen
import com.mengjizhang.app.ui.theme.PinkLight
import com.mengjizhang.app.ui.theme.PinkPrimary
import com.mengjizhang.app.ui.theme.SkyBlue
import com.mengjizhang.app.ui.viewmodel.RecordViewModel
import com.mengjizhang.app.utils.ExportHelper
import com.mengjizhang.app.utils.ExportSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportScreen(
    viewModel: RecordViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val recentRecords by viewModel.recentRecords.collectAsState()
    val monthlyRecords by viewModel.monthlyRecords.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()

    var isExporting by remember { mutableStateOf(false) }
    var exportType by remember { mutableStateOf("") }
    var backupFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var selectedBackupFile by remember { mutableStateOf<File?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 文件选择器（用于恢复备份）
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    ExportHelper.restoreDatabase(context, it)
                }
                result.onSuccess {
                    Toast.makeText(context, "数据恢复成功，请重启应用", Toast.LENGTH_LONG).show()
                }.onFailure { e ->
                    Toast.makeText(context, "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 加载备份文件列表
    LaunchedEffect(Unit) {
        backupFiles = ExportHelper.getBackupFiles(context)
    }

    // 合并所有记录（去重）
    val allRecords = remember(recentRecords, monthlyRecords) {
        (recentRecords + monthlyRecords).distinctBy { it.id }.sortedByDescending { it.date }
    }

    // 恢复确认弹窗
    if (showRestoreDialog && selectedBackupFile != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("确认恢复", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("确定要从以下备份恢复数据吗？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedBackupFile!!.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ 当前数据将被覆盖，请确保已备份！",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRed
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                ExportHelper.restoreDatabase(
                                    context,
                                    androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        selectedBackupFile!!
                                    )
                                )
                            }
                            result.onSuccess {
                                Toast.makeText(context, "数据恢复成功，请重启应用", Toast.LENGTH_LONG).show()
                            }.onFailure { e ->
                                Toast.makeText(context, "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showRestoreDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text("确认恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 删除确认弹窗
    if (showDeleteDialog && selectedBackupFile != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除备份", fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除这个备份文件吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        ExportHelper.deleteBackup(selectedBackupFile!!)
                        backupFiles = ExportHelper.getBackupFiles(context)
                        showDeleteDialog = false
                        Toast.makeText(context, "备份已删除", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Spacer(modifier = Modifier.height(40.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "数据备份",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Box(modifier = Modifier.size(48.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 数据概览
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PinkLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "📊 数据概览",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatItem(label = "总记录", value = "${allRecords.size}笔")
                            StatItem(label = "本月收入", value = "¥${String.format("%.0f", monthlyIncome)}")
                            StatItem(label = "本月支出", value = "¥${String.format("%.0f", monthlyExpense)}")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 导出选项
            item {
                Text(
                    text = "导出数据",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExportOptionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Description,
                        title = "CSV",
                        subtitle = "通用表格格式",
                        color = MintGreen,
                        isLoading = isExporting && exportType == "csv",
                        onClick = {
                            if (!isExporting) {
                                isExporting = true
                                exportType = "csv"
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        ExportHelper.exportToCsv(context, allRecords)
                                    }
                                    isExporting = false
                                    result.onSuccess { file ->
                                        Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                                        ExportHelper.shareFile(context, file, ExportHelper.getMimeType(file))
                                    }.onFailure { e ->
                                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                    ExportOptionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TableChart,
                        title = "Excel",
                        subtitle = "电子表格格式",
                        color = SkyBlue,
                        isLoading = isExporting && exportType == "excel",
                        onClick = {
                            if (!isExporting) {
                                isExporting = true
                                exportType = "excel"
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        ExportHelper.exportToExcel(context, allRecords)
                                    }
                                    isExporting = false
                                    result.onSuccess { file ->
                                        Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                                        ExportHelper.shareFile(context, file, ExportHelper.getMimeType(file))
                                    }.onFailure { e ->
                                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                ExportOptionCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.PictureAsPdf,
                    title = "PDF 报表",
                    subtitle = "生成精美账单报表，包含汇总和明细",
                    color = LavenderPurple,
                    isLoading = isExporting && exportType == "pdf",
                    onClick = {
                        if (!isExporting) {
                            isExporting = true
                            exportType = "pdf"
                            scope.launch {
                                val summary = ExportSummary(
                                    totalCount = allRecords.size,
                                    totalIncome = allRecords.filter { !it.isExpense }.sumOf { it.amount },
                                    totalExpense = allRecords.filter { it.isExpense }.sumOf { it.amount }
                                )
                                val result = withContext(Dispatchers.IO) {
                                    ExportHelper.exportToPdf(context, allRecords, summary)
                                }
                                isExporting = false
                                result.onSuccess { file ->
                                    Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                                    ExportHelper.shareFile(context, file, ExportHelper.getMimeType(file))
                                }.onFailure { e ->
                                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 备份选项
            item {
                Text(
                    text = "本地备份",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 创建备份
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        ExportHelper.backupDatabase(context)
                                    }
                                    result.onSuccess { file ->
                                        backupFiles = ExportHelper.getBackupFiles(context)
                                        Toast.makeText(context, "备份成功", Toast.LENGTH_SHORT).show()
                                    }.onFailure { e ->
                                        Toast.makeText(context, "备份失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backup,
                                    contentDescription = null,
                                    tint = IncomeGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "创建备份",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 恢复备份
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                filePickerLauncher.launch("*/*")
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(SkyBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = SkyBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "导入备份",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 备份列表
            if (backupFiles.isNotEmpty()) {
                item {
                    Text(
                        text = "历史备份 (${backupFiles.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(backupFiles) { file ->
                    BackupFileItem(
                        file = file,
                        onRestore = {
                            selectedBackupFile = file
                            showRestoreDialog = true
                        },
                        onDelete = {
                            selectedBackupFile = file
                            showDeleteDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PinkPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExportOptionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(enabled = !isLoading) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = color,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupFileItem(
    file: File,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    val fileDate = dateFormat.format(Date(file.lastModified()))
    val fileSize = "${file.length() / 1024} KB"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MintGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = IncomeGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileDate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = fileSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "恢复",
                    tint = PinkPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = ExpenseRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
