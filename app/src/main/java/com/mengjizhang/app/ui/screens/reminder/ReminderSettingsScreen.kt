package com.mengjizhang.app.ui.screens.reminder

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mengjizhang.app.data.model.ReminderType
import com.mengjizhang.app.data.model.weekDayNames
import com.mengjizhang.app.reminder.NotificationHelper
import com.mengjizhang.app.reminder.ReminderManager
import com.mengjizhang.app.reminder.ReminderScheduler
import com.mengjizhang.app.ui.theme.PinkPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by ReminderManager.settings.collectAsState()

    var showTimePicker by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    // 通知权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            ReminderManager.setEnabled(true)
            ReminderScheduler.scheduleReminder(context)
            Toast.makeText(context, "提醒已开启", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "需要通知权限才能使用提醒功能", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("提醒设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 开启提醒开关
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PinkPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = PinkPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "开启记账提醒",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "每天定时提醒你记账",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = settings.isEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        ReminderManager.setEnabled(true)
                                        ReminderScheduler.scheduleReminder(context)
                                    }
                                } else {
                                    ReminderManager.setEnabled(false)
                                    ReminderScheduler.cancelReminder(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PinkPrimary,
                                checkedTrackColor = PinkPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // 提醒时间设置
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = settings.isEnabled) { showTimePicker = true },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PinkPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = PinkPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "提醒时间",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format("%02d:%02d", settings.reminderHour, settings.reminderMinute),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (settings.isEnabled) PinkPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 重复类型选择
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "重复",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 重复类型选项
                        ReminderType.entries.forEach { type ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = settings.isEnabled) {
                                        ReminderManager.setReminderType(type)
                                        ReminderScheduler.scheduleReminder(context)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.reminderType == type,
                                    onClick = {
                                        ReminderManager.setReminderType(type)
                                        ReminderScheduler.scheduleReminder(context)
                                    },
                                    enabled = settings.isEnabled,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = PinkPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (type) {
                                        ReminderType.DAILY -> "每天"
                                        ReminderType.WEEKDAYS -> "工作日"
                                        ReminderType.WEEKENDS -> "周末"
                                        ReminderType.CUSTOM -> "自定义"
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // 自定义日期选择（仅在自定义模式下显示）
            if (settings.reminderType == ReminderType.CUSTOM) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "选择日期",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                (1..7).forEach { day ->
                                    val isSelected = day in settings.repeatDays
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val newDays = if (isSelected) {
                                                settings.repeatDays - day
                                            } else {
                                                settings.repeatDays + day
                                            }
                                            if (newDays.isNotEmpty()) {
                                                ReminderManager.setRepeatDays(newDays)
                                                ReminderScheduler.scheduleReminder(context)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = weekDayNames[day]?.take(1) ?: "",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        enabled = settings.isEnabled,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PinkPrimary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 时间选择器对话框
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                ReminderManager.setTime(hour, minute)
                ReminderScheduler.scheduleReminder(context)
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择提醒时间") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
