package com.mengjizhang.app.ui.screens.add

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.mengjizhang.app.data.model.Category
import com.mengjizhang.app.data.model.expenseCategories
import com.mengjizhang.app.data.model.incomeCategories
import com.mengjizhang.app.ui.theme.ExpenseRed
import com.mengjizhang.app.ui.theme.IncomeGreen
import com.mengjizhang.app.ui.theme.MengJiZhangTheme
import com.mengjizhang.app.ui.theme.PinkLight
import com.mengjizhang.app.ui.theme.PinkPrimary
import com.mengjizhang.app.utils.BaiduVoiceRecognitionHelper
import com.mengjizhang.app.utils.ImageHelper
import com.mengjizhang.app.utils.VoiceParser
import java.io.File

@Composable
fun AddScreen(
    onBack: () -> Unit = {},
    onSave: (amount: Double, categoryId: Int, isExpense: Boolean, note: String, imagePath: String?, inputMethod: String) -> Unit = { _, _, _, _, _, _ -> },
    onUpdate: (recordId: Long, amount: Double, categoryId: Int, isExpense: Boolean, note: String, imagePath: String?) -> Unit = { _, _, _, _, _, _ -> },
    onNavigateToCamera: (mode: String) -> Unit = {},
    initialAmount: Double? = null,
    initialNote: String? = null,
    initialImagePath: String? = null,
    initialCategory: String? = null,  // 分类名称，如"餐饮"、"交通"等
    editRecordId: Long? = null,  // 编辑模式：记录ID
    editCategoryId: Int? = null,  // 编辑模式：分类ID
    editIsExpense: Boolean? = null  // 编辑模式：是否支出
) {
    val isEditMode = editRecordId != null
    val context = LocalContext.current

    // 编辑模式：根据 editIsExpense 设置初始标签
    var selectedTabIndex by remember { mutableIntStateOf(
        if (editIsExpense == false) 1 else 0
    ) }
    var amount by remember { mutableStateOf(initialAmount?.let {
        String.format("%.2f", it).trimEnd('0').trimEnd('.').ifEmpty { "0" }
    } ?: "0") }
    // 编辑模式：根据 editCategoryId 匹配分类；OCR模式：根据分类名称匹配
    var selectedCategory by remember { mutableStateOf<Category?>(
        when {
            editCategoryId != null -> {
                expenseCategories.find { it.id == editCategoryId }
                    ?: incomeCategories.find { it.id == editCategoryId }
            }
            initialCategory != null -> {
                expenseCategories.find { it.name == initialCategory }
                    ?: incomeCategories.find { it.name == initialCategory }
            }
            else -> null
        }
    ) }
    var inputMethod by remember { mutableIntStateOf(if (initialAmount != null) 2 else 0) }
    var note by remember { mutableStateOf(initialNote ?: "") }
    var imagePath by remember { mutableStateOf(initialImagePath) }

    // 弹窗状态
    var showNumberPadDialog by remember { mutableStateOf(false) }
    var showVoiceOverlay by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    val currentCategories = if (selectedTabIndex == 0) expenseCategories else incomeCategories

    // 当 OCR 数据更新时，自动更新状态
    LaunchedEffect(initialAmount, initialNote, initialImagePath, initialCategory) {
        initialAmount?.let {
            amount = String.format("%.2f", it).trimEnd('0').trimEnd('.').ifEmpty { "0" }
            inputMethod = 2
        }
        initialNote?.let { note = it }
        initialImagePath?.let { imagePath = it }
        initialCategory?.let { categoryName ->
            // 根据分类名称匹配对应的分类
            val matchedCategory = expenseCategories.find { it.name == categoryName }
                ?: incomeCategories.find { it.name == categoryName }
            matchedCategory?.let {
                selectedCategory = it
                // 如果是收入分类，切换到收入标签
                if (!it.isExpense) {
                    selectedTabIndex = 1
                }
            }
        }
    }

    // 当切换类型时，重置选中的分类
    if (selectedCategory != null && selectedCategory !in currentCategories) {
        selectedCategory = null
    }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = ImageHelper.saveImageFromUri(context, it)
            if (savedPath != null) {
                imagePath = savedPath
                inputMethod = 2
                Toast.makeText(context, "图片已添加", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "图片保存失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 百度语音识别助手
    val voiceHelper = remember {
        BaiduVoiceRecognitionHelper(
            context = context,
            onResult = { result ->
                val parseResult = VoiceParser.parse(result)

                parseResult.amount?.let { parsedAmount ->
                    amount = String.format("%.2f", parsedAmount).trimEnd('0').trimEnd('.')
                    if (amount.isEmpty()) amount = "0"
                }

                parseResult.categoryKeyword?.let { keyword ->
                    val matchedCategory = currentCategories.find { it.name == keyword }
                    if (matchedCategory != null) {
                        selectedCategory = matchedCategory
                    }
                }

                note = result
                inputMethod = 1

                Toast.makeText(context, "识别成功", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            },
            onListening = { listening ->
                isListening = listening
                if (!listening) {
                    showVoiceOverlay = false
                }
            }
        )
    }

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceOverlay = true
            voiceHelper.startListening()
        } else {
            Toast.makeText(context, "需要录音权限才能使用语音记账", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceHelper.destroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    text = if (isEditMode) "编辑账单" else "记一笔",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        if (amountValue > 0 && selectedCategory != null) {
                            if (isEditMode) {
                                // 编辑模式：调用更新
                                onUpdate(
                                    editRecordId!!,
                                    amountValue,
                                    selectedCategory!!.id,
                                    selectedTabIndex == 0,
                                    note,
                                    imagePath
                                )
                            } else {
                                // 新增模式：调用保存
                                val method = when (inputMethod) {
                                    1 -> "voice"
                                    2 -> "camera"
                                    else -> "manual"
                                }
                                onSave(
                                    amountValue,
                                    selectedCategory!!.id,
                                    selectedTabIndex == 0,
                                    note,
                                    imagePath,
                                    method
                                )
                            }
                            onBack()
                        }
                    },
                    enabled = amount != "0" && amount.isNotEmpty() && selectedCategory != null
                ) {
                    Text(
                        text = if (isEditMode) "更新" else "保存",
                        color = if (amount != "0" && selectedCategory != null) PinkPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Type Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.padding(horizontal = 16.dp),
                containerColor = MaterialTheme.colorScheme.background,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = if (selectedTabIndex == 0) PinkPrimary else IncomeGreen
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            "支出",
                            color = if (selectedTabIndex == 0) PinkPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            "收入",
                            color = if (selectedTabIndex == 1) IncomeGreen
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // 可滚动内容区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 金额显示卡片 - 可点击打开数字键盘
                AmountDisplayCard(
                    amount = amount,
                    isExpense = selectedTabIndex == 0,
                    onClick = { showNumberPadDialog = true }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 分类选择 - 横向滚动芯片
                Text(
                    text = "选择分类",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                CategoryChipsRow(
                    categories = currentCategories,
                    selectedCategory = selectedCategory,
                    isExpense = selectedTabIndex == 0,
                    onCategorySelect = { selectedCategory = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 图片预览区域（有图片时显示）
                if (imagePath != null) {
                    ImagePreviewCard(
                        imagePath = imagePath!!,
                        onDelete = {
                            imagePath?.let { ImageHelper.deleteImage(it) }
                            imagePath = null
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 备注输入框
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("添加备注（可选）") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PinkPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(100.dp)) // 为底部操作栏留空间
            }

            // 底部固定操作栏
            BottomActionBar(
                onCameraClick = { onNavigateToCamera("camera") },
                onVoiceClick = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onManualClick = { showNumberPadDialog = true }
            )
        }

        // 语音识别遮罩
        AnimatedVisibility(
            visible = showVoiceOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            VoiceListeningOverlay(
                isListening = isListening,
                onCancel = {
                    voiceHelper.destroy()
                    showVoiceOverlay = false
                    isListening = false
                },
                onComplete = {
                    voiceHelper.stopListening()
                }
            )
        }

        // 手动输入弹窗
        if (showNumberPadDialog) {
            NumberPadDialog(
                currentAmount = amount,
                isExpense = selectedTabIndex == 0,
                onAmountChange = { amount = it },
                onDismiss = { showNumberPadDialog = false }
            )
        }
    }
}

/**
 * 金额显示卡片
 */
@Composable
private fun AmountDisplayCard(
    amount: String,
    isExpense: Boolean,
    onClick: () -> Unit
) {
    val amountColor = if (isExpense) ExpenseRed else IncomeGreen

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = amountColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¥",
                    style = MaterialTheme.typography.headlineMedium,
                    color = amountColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (amount == "0") "0.00" else amount,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = amountColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击输入金额",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 分类横向滚动芯片
 */
@Composable
private fun CategoryChipsRow(
    categories: List<Category>,
    selectedCategory: Category?,
    isExpense: Boolean,
    onCategorySelect: (Category) -> Unit
) {
    val accentColor = if (isExpense) PinkPrimary else IncomeGreen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = selectedCategory == category

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(category) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(category.emoji)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(category.name)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor,
                    selectedLabelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = accentColor.copy(alpha = 0.5f),
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

/**
 * 图片预览卡片
 */
@Composable
private fun ImagePreviewCard(
    imagePath: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "凭证图片",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            // 删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "删除图片",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 底部操作栏
 */
@Composable
private fun BottomActionBar(
    onCameraClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onManualClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                icon = Icons.Default.CameraAlt,
                label = "拍照识别",
                color = PinkPrimary,
                onClick = onCameraClick
            )
            ActionButton(
                icon = Icons.Default.Mic,
                label = "语音输入",
                color = PinkPrimary,
                onClick = onVoiceClick
            )
            ActionButton(
                icon = Icons.Default.Edit,
                label = "手动输入",
                color = PinkPrimary,
                onClick = onManualClick
            )
        }
    }
}

/**
 * 操作按钮
 */
@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 语音识别全屏遮罩
 */
@Composable
private fun VoiceListeningOverlay(
    isListening: Boolean,
    onCancel: () -> Unit,
    onComplete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { /* 阻止点击穿透 */ },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 脉冲动画圆圈
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // 外层动画圆
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(PinkPrimary.copy(alpha = 0.3f))
                )
                // 中间圆
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(PinkPrimary.copy(alpha = 0.6f))
                        .clickable { onComplete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isListening) "正在录音..." else "准备中...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "说完后点击麦克风完成录音",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TextButton(onClick = onCancel) {
                    Text("取消", color = Color.White)
                }
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text("完成录音")
                }
            }
        }
    }
}

/**
 * 数字键盘弹窗
 */
@Composable
private fun NumberPadDialog(
    currentAmount: String,
    isExpense: Boolean,
    onAmountChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempAmount by remember { mutableStateOf(currentAmount) }
    val amountColor = if (isExpense) ExpenseRed else IncomeGreen

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 金额显示
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¥",
                        style = MaterialTheme.typography.headlineMedium,
                        color = amountColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (tempAmount.isEmpty() || tempAmount == "0") "0" else tempAmount,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 数字键盘
                val buttons = listOf(
                    "1", "2", "3",
                    "4", "5", "6",
                    "7", "8", "9",
                    ".", "0", "DEL"
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(240.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    items(buttons) { button ->
                        Button(
                            onClick = {
                                tempAmount = when {
                                    button == "DEL" -> {
                                        if (tempAmount.length > 1) tempAmount.dropLast(1) else "0"
                                    }
                                    tempAmount == "0" && button != "." -> button
                                    button == "." && tempAmount.contains(".") -> tempAmount
                                    tempAmount.contains(".") && tempAmount.substringAfter(".").length >= 2 -> tempAmount
                                    else -> tempAmount + button
                                }
                            },
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (button == "DEL") PinkLight
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            if (button == "DEL") {
                                Icon(
                                    Icons.Default.Backspace,
                                    contentDescription = "删除",
                                    tint = PinkPrimary
                                )
                            } else {
                                Text(
                                    text = button,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 确认按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            onAmountChange(tempAmount)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddScreenPreview() {
    MengJiZhangTheme {
        AddScreen()
    }
}
