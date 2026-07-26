package com.flowreader.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.BuildConfig
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.domain.model.AppThemeMode
import com.flowreader.app.domain.model.ColorSource
import com.flowreader.app.domain.model.GestureAction
import com.flowreader.app.domain.model.GestureSettings
import java.io.File

/**
 * App-level settings only.
 *
 * v52 removed the duplicated 字号 / 行间距 / 翻页模式 entries that fought with the reader's own
 * panel (changing one silently overwrote the other). Everything that shapes the page now lives in
 * the reader sheet; what remains here is app-wide or device-level.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showReadingGoalDialog by remember { mutableStateOf(false) }
    var showGestureDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.onExportReady(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.onImportReady(uri) }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.onCustomFontSelected(uri) }

    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportResult()
        }
    }

    LaunchedEffect(uiState.importResult) {
        uiState.importResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { TopAppBar(title = { Text("设置") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "外观") {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "主题模式",
                    subtitle = uiState.themeMode.displayName
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
                    modifier = Modifier.padding(horizontal = FlowSpacing.lg, vertical = FlowSpacing.sm)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.themeMode == mode,
                            onClick = { viewModel.updateThemeMode(mode) },
                            label = { Text(mode.displayName) }
                        )
                    }
                }

                SettingsItem(
                    icon = Icons.Default.ColorLens,
                    title = "配色来源",
                    subtitle = "品牌配色保持视觉身份；跟随壁纸需要 Android 12 及以上"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
                    modifier = Modifier.padding(horizontal = FlowSpacing.lg, vertical = FlowSpacing.sm)
                ) {
                    ColorSource.entries.forEach { source ->
                        FilterChip(
                            selected = uiState.colorSource == source,
                            onClick = { viewModel.updateColorSource(source) },
                            label = { Text(source.displayName) }
                        )
                    }
                }
            }

            SettingsSection(title = "阅读偏好") {
                SettingsItem(
                    icon = Icons.Default.TextFields,
                    title = "排版与阅读主题",
                    subtitle = "在阅读界面顶栏「更多 → 阅读设置」中调整，改动实时预览"
                )

                SettingsItem(
                    icon = Icons.Default.Gesture,
                    title = "手势自定义",
                    subtitle = "点击区域、双击、长按、左右滑动",
                    onClick = { showGestureDialog = true }
                )

                SettingsSwitch(
                    icon = Icons.Default.WbSunny,
                    title = "保持屏幕常亮",
                    checked = uiState.readingSettings.keepScreenOn,
                    onCheckedChange = { viewModel.updateKeepScreenOn(it) }
                )
            }

            SettingsSection(title = "字体管理") {
                val fontName = remember(uiState.customFontPath) {
                    uiState.customFontPath?.let { path ->
                        runCatching { File(path).takeIf { it.isFile }?.name }.getOrNull()
                    }
                }

                SettingsItem(
                    icon = Icons.Default.TextFields,
                    title = "自定义字体",
                    subtitle = fontName ?: "点击选择 .ttf / .otf 字体文件",
                    onClick = { fontPickerLauncher.launch("*/*") }
                )

                if (uiState.customFontPath != null) {
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "清除自定义字体",
                        subtitle = "恢复所选内置字体",
                        onClick = { viewModel.clearCustomFont() }
                    )
                }
            }

            SettingsSection(title = "提醒") {
                SettingsSwitch(
                    icon = Icons.Default.Notifications,
                    title = "每日阅读提醒",
                    subtitle = "提醒时间: %d:%02d".format(uiState.readingReminderHour, uiState.readingReminderMinute),
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

            SettingsSection(title = "数据管理") {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "备份数据",
                    subtitle = "导出书籍、进度、书签与标注",
                    onClick = {
                        viewModel.exportData()
                        exportLauncher.launch("flowreader-backup.json")
                    }
                )

                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = "恢复数据",
                    subtitle = "从备份文件导入",
                    onClick = {
                        viewModel.importData()
                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                    }
                )
            }

            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "关于心流阅读",
                    subtitle = "版本 ${BuildConfig.VERSION_NAME}",
                    onClick = { showAboutDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(FlowSpacing.xxl))
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showReadingGoalDialog) {
        ReadingGoalDialog(
            currentGoal = uiState.dailyReadingGoal,
            onGoalChange = { viewModel.updateDailyReadingGoal(it) },
            onDismiss = { showReadingGoalDialog = false }
        )
    }

    if (showGestureDialog) {
        GestureSettingsDialog(
            gestureSettings = uiState.readingSettings.gestureSettings,
            onGestureChange = { viewModel.updateGestureSettings(it) },
            onDismiss = { showGestureDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = FlowSpacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = FlowSpacing.lg, vertical = FlowSpacing.sm)
        )
        content()
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String = "", onClick: (() -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle.isNotEmpty()) {
            { Text(subtitle) }
        } else {
            null
        },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}

@Composable
private fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String = ""
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle.isNotEmpty()) {
            { Text(subtitle) }
        } else {
            null
        },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

@Composable
private fun ReadingGoalDialog(currentGoal: Int, onGoalChange: (Int) -> Unit, onDismiss: () -> Unit) {
    var goal by remember { mutableIntStateOf(currentGoal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("每日阅读目标") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$goal 分钟", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
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
                    Text("5 分钟", style = MaterialTheme.typography.bodySmall)
                    Text("2 小时", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onGoalChange(goal)
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于心流阅读") },
        text = {
            Column {
                Text("版本: ${BuildConfig.VERSION_NAME}")
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                Text("一款离线优先的电子书阅读应用")
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                Text("感谢以下开源项目:")
                Text("- Jetpack Compose", style = MaterialTheme.typography.bodySmall)
                Text("- Room Database", style = MaterialTheme.typography.bodySmall)
                Text("- Hilt", style = MaterialTheme.typography.bodySmall)
                Text("- Coil", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                Text("作者: HuZaiGong", style = MaterialTheme.typography.bodySmall)
                Text("GitHub: github.com/HuZaiGong/flowreader", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
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
                verticalArrangement = Arrangement.spacedBy(FlowSpacing.md)
            ) {
                GestureRow("左侧点击", localSettings.leftTapAction) {
                    localSettings = localSettings.copy(leftTapAction = it)
                }
                GestureRow("中间点击", localSettings.middleTapAction) {
                    localSettings = localSettings.copy(middleTapAction = it)
                }
                GestureRow("右侧点击", localSettings.rightTapAction) {
                    localSettings = localSettings.copy(rightTapAction = it)
                }
                GestureRow("左滑", localSettings.swipeLeftAction) {
                    localSettings = localSettings.copy(swipeLeftAction = it)
                }
                GestureRow("右滑", localSettings.swipeRightAction) {
                    localSettings = localSettings.copy(swipeRightAction = it)
                }
                GestureRow("双击", localSettings.doubleTapAction) {
                    localSettings = localSettings.copy(doubleTapAction = it)
                }
                GestureRow("长按", localSettings.longPressAction) {
                    localSettings = localSettings.copy(longPressAction = it)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("边缘热区")
                    Switch(
                        checked = localSettings.edgeGestureEnabled,
                        onCheckedChange = { localSettings = localSettings.copy(edgeGestureEnabled = it) }
                    )
                }

                if (localSettings.edgeGestureEnabled) {
                    Text("左侧热区: ${localSettings.leftEdgeWidth}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = localSettings.leftEdgeWidth.toFloat(),
                        onValueChange = { localSettings = localSettings.copy(leftEdgeWidth = it.toInt()) },
                        valueRange = 5f..45f
                    )
                    Text("右侧热区: ${localSettings.rightEdgeWidth}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = localSettings.rightEdgeWidth.toFloat(),
                        onValueChange = { localSettings = localSettings.copy(rightEdgeWidth = it.toInt()) },
                        valueRange = 5f..45f
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onGestureChange(localSettings)
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun GestureRow(label: String, value: GestureAction, onValueChange: (GestureAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        var expanded by remember { mutableStateOf(false) }
        Row {
            TextButton(onClick = { expanded = true }) { Text(value.displayName) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                GestureAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.displayName) },
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
