package com.flowreader.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showReaderThemeDialog by remember { mutableStateOf(false) }
    var showPageModeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showReadingGoalDialog by remember { mutableStateOf(false) }
    var showGestureDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "显示设置") {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "应用主题",
                    subtitle = getThemeName(uiState.appTheme),
                    onClick = { showThemeDialog = true }
                )

                SettingsSwitch(
                    icon = Icons.Default.BrightnessAuto,
                    title = "自动夜间模式",
                    subtitle = "根据时间自动切换深色/浅色",
                    checked = uiState.autoTimeTheme,
                    onCheckedChange = { viewModel.updateAutoTimeTheme(it) }
                )

                SettingsItem(
                    icon = Icons.Default.Brightness6,
                    title = "阅读主题",
                    subtitle = getReaderThemeName(uiState.readingSettings.theme),
                    onClick = { showReaderThemeDialog = true }
                )
            }

            SettingsSection(title = "提醒") {
                SettingsSwitch(
                    icon = Icons.Default.Notifications,
                    title = "每日阅读提醒",
                    subtitle = "提醒时间: ${uiState.readingReminderHour}:${String.format("%02d", uiState.readingReminderMinute)}",
                    checked = uiState.readingReminderEnabled,
                    onCheckedChange = { viewModel.updateReadingReminder(it) }
                )
            }

            SettingsSection(title = "阅读目标") {
                SettingsItem(
                    icon = Icons.Default.Flag,
                    title = "每日阅读时长",
                    subtitle = "${uiState.dailyReadingGoal} 分钟",
                    onClick = { showReadingGoalDialog = true }
                )
            }

            SettingsSection(title = "阅读设置") {
                SettingsItem(
                    icon = Icons.Default.TextFields,
                    title = "字体大小",
                    subtitle = "${uiState.readingSettings.fontSize}sp",
                    onClick = { showFontSizeDialog = true }
                )

                SettingsItem(
                    icon = Icons.Default.Gesture,
                    title = "手势自定义",
                    subtitle = "配置阅读手势",
                    onClick = { showGestureDialog = true }
                )

                SettingsItem(
                    icon = Icons.Default.FormatLineSpacing,
                    title = "行间距",
                    subtitle = String.format("%.1f", uiState.readingSettings.lineSpacing),
                    onClick = {
                        val newSpacing = when {
                            uiState.readingSettings.lineSpacing < 2.0f -> uiState.readingSettings.lineSpacing + 0.25f
                            else -> 1.25f
                        }
                        viewModel.updateLineSpacing(newSpacing)
                    }
                )

                SettingsItem(
                    icon = Icons.Default.FlipToBack,
                    title = "翻页模式",
                    subtitle = getPageModeName(uiState.readingSettings.pageMode),
                    onClick = { showPageModeDialog = true }
                )

                SettingsSwitch(
                    icon = Icons.Default.WbSunny,
                    title = "保持屏幕常亮",
                    checked = uiState.readingSettings.keepScreenOn,
                    onCheckedChange = { viewModel.updateKeepScreenOn(it) }
                )
            }

            SettingsSection(title = "数据管理") {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "备份数据",
                    subtitle = "导出书籍和阅读进度",
                    onClick = { viewModel.exportData() }
                )

                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = "恢复数据",
                    subtitle = "从备份文件导入",
                    onClick = { viewModel.importData() }
                )
            }

            SettingsSection(title = "关于") {
            }
        }

        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false }
            )
        }
    }

    if (showReadingGoalDialog) {
        ReadingGoalDialog(
            currentGoal = uiState.dailyReadingGoal,
            onGoalChange = { viewModel.updateDailyReadingGoal(it) },
            onDismiss = { showReadingGoalDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.appTheme,
            onThemeSelect = {
                viewModel.updateAppTheme(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showReaderThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.readingSettings.theme,
            onThemeSelect = {
                viewModel.updateReaderTheme(it)
                showReaderThemeDialog = false
            },
            onDismiss = { showReaderThemeDialog = false },
            showSystemTheme = false
        )
    }

    if (showPageModeDialog) {
        PageModeDialog(
            currentMode = uiState.readingSettings.pageMode,
            onModeSelect = {
                viewModel.updatePageMode(it)
                showPageModeDialog = false
            },
            onDismiss = { showPageModeDialog = false }
        )
    }

    if (showFontSizeDialog) {
        FontSizeDialog(
            currentSize = uiState.readingSettings.fontSize,
            onSizeChange = {
                viewModel.updateFontSize(it)
            },
            onDismiss = { showFontSizeDialog = false }
        )
    }

    if (showGestureDialog) {
        GestureSettingsDialog(
            gestureSettings = uiState.readingSettings.gestureSettings,
            onGestureChange = { gestureSettings ->
                viewModel.updateGestureSettings(gestureSettings)
            },
            onDismiss = { showGestureDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SettingsSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle.isNotEmpty()) {{ Text(subtitle) }} else null,
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: ReaderTheme,
    onThemeSelect: (ReaderTheme) -> Unit,
    onDismiss: () -> Unit,
    showSystemTheme: Boolean = true
) {
    val themes = if (showSystemTheme) {
        ReaderTheme.values().toList()
    } else {
        ReaderTheme.values().filter { it != ReaderTheme.SYSTEM }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
        text = {
            Column {
                themes.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelect(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = { onThemeSelect(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(getThemeName(theme))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun PageModeDialog(
    currentMode: PageMode,
    onModeSelect: (PageMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("翻页模式") },
        text = {
            Column {
                PageMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelect(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == currentMode,
                            onClick = { onModeSelect(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(getPageModeName(mode))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun FontSizeDialog(
    currentSize: Int,
    onSizeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var size by remember { mutableIntStateOf(currentSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("字体大小") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${size}sp",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = size.toFloat(),
                    onValueChange = {
                        size = it.toInt()
                        onSizeChange(size)
                    },
                    valueRange = 12f..32f,
                    steps = 19
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("小", style = MaterialTheme.typography.bodySmall)
                    Text("大", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

private fun getThemeName(theme: ReaderTheme): String = when (theme) {
    ReaderTheme.SYSTEM -> "跟随系统"
    ReaderTheme.LIGHT -> "浅色"
    ReaderTheme.DARK -> "深色"
    ReaderTheme.SEPIA -> "护眼"
    ReaderTheme.PAPER -> "羊皮纸"
    ReaderTheme.AMOLED -> "夜间"
    ReaderTheme.MORNING -> "晨读"
    ReaderTheme.NOON -> "午读"
    ReaderTheme.EVENING -> "暮读"
    ReaderTheme.NIGHT -> "夜读"
}

private fun getReaderThemeName(theme: ReaderTheme): String = when (theme) {
    ReaderTheme.SYSTEM -> "浅色"
    ReaderTheme.LIGHT -> "浅色"
    ReaderTheme.DARK -> "深色"
    ReaderTheme.SEPIA -> "护眼"
    ReaderTheme.PAPER -> "羊皮纸"
    ReaderTheme.AMOLED -> "夜间"
    ReaderTheme.MORNING -> "晨读"
    ReaderTheme.NOON -> "午读"
    ReaderTheme.EVENING -> "暮读"
    ReaderTheme.NIGHT -> "夜读"
}

private fun getPageModeName(mode: PageMode): String = when (mode) {
    PageMode.SLIDE -> "滑动"
    PageMode.SIMULATION -> "仿真翻页"
    PageMode.NONE -> "无动画"
    PageMode.CURL -> "卷曲"
    PageMode.SLIDE_OVER -> "滑动覆盖"
}

@Composable
private fun ReadingGoalDialog(
    currentGoal: Int,
    onGoalChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var goal by remember { mutableIntStateOf(currentGoal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("每日阅读目标") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${goal} 分钟",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = goal.toFloat(),
                    onValueChange = { goal = it.toInt() },
                    valueRange = 5f..120f,
                    steps = 22
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("5分钟", style = MaterialTheme.typography.bodySmall)
                    Text("2小时", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onGoalChange(goal)
                onDismiss()
            }) {
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

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("关于心流阅读")
        },
        text = {
            Column {
                Text("版本: 12.0.0")
                Spacer(modifier = Modifier.height(8.dp))
                Text("一款简洁优雅的电子书阅读应用")
                Spacer(modifier = Modifier.height(8.dp))
                Text("感谢以下开源项目:")
                Text("- Jetpack Compose", style = MaterialTheme.typography.bodySmall)
                Text("- Room Database", style = MaterialTheme.typography.bodySmall)
                Text("- Hilt", style = MaterialTheme.typography.bodySmall)
                Text("- Coil", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("作者: HuZaiGong", style = MaterialTheme.typography.bodySmall)
                Text("GitHub: github.com/HuZaiGong/flowreader", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun GestureSettingsDialog(
    gestureSettings: GestureSettings,
    onGestureChange: (GestureSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var localSettings by remember { mutableStateOf(gestureSettings) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手势自定义") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GestureRow(
                    label = "左侧点击",
                    value = localSettings.leftTapAction,
                    onValueChange = { localSettings = localSettings.copy(leftTapAction = it) }
                )
                GestureRow(
                    label = "中间点击",
                    value = localSettings.middleTapAction,
                    onValueChange = { localSettings = localSettings.copy(middleTapAction = it) }
                )
                GestureRow(
                    label = "右侧点击",
                    value = localSettings.rightTapAction,
                    onValueChange = { localSettings = localSettings.copy(rightTapAction = it) }
                )
                GestureRow(
                    label = "左滑",
                    value = localSettings.swipeLeftAction,
                    onValueChange = { localSettings = localSettings.copy(swipeLeftAction = it) }
                )
                GestureRow(
                    label = "右滑",
                    value = localSettings.swipeRightAction,
                    onValueChange = { localSettings = localSettings.copy(swipeRightAction = it) }
                )
                GestureRow(
                    label = "双击",
                    value = localSettings.doubleTapAction,
                    onValueChange = { localSettings = localSettings.copy(doubleTapAction = it) }
                )
                GestureRow(
                    label = "长按",
                    value = localSettings.longPressAction,
                    onValueChange = { localSettings = localSettings.copy(longPressAction = it) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("边缘手势")
                    Switch(
                        checked = localSettings.edgeGestureEnabled,
                        onCheckedChange = { localSettings = localSettings.copy(edgeGestureEnabled = it) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onGestureChange(localSettings)
                onDismiss()
            }) {
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

@Composable
private fun GestureRow(
    label: String,
    value: GestureAction,
    onValueChange: (GestureAction) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        var expanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(getGestureActionName(value))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                GestureAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(getGestureActionName(action)) },
                        onClick = {
                            onValueChange(action)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun getGestureActionName(action: GestureAction): String = when (action) {
    GestureAction.PREVIOUS_PAGE -> "上一页"
    GestureAction.NEXT_PAGE -> "下一页"
    GestureAction.TOGGLE_CONTROLS -> "显示/隐藏控制栏"
    GestureAction.SHOW_SETTINGS -> "显示设置"
    GestureAction.SHOW_BOOKMARKS -> "显示书签"
    GestureAction.SHOW_TOC -> "显示目录"
    GestureAction.ADD_BOOKMARK -> "添加书签"
    GestureAction.NONE -> "无"
}
