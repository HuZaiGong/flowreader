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

            SettingsSection(title = "阅读设置") {
                SettingsItem(
                    icon = Icons.Default.TextFields,
                    title = "字体大小",
                    subtitle = "${uiState.readingSettings.fontSize}sp",
                    onClick = { showFontSizeDialog = true }
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

            SettingsSection(title = "账户") {
                SettingsItem(
                    icon = Icons.Default.AccountCircle,
                    title = "账户与同步",
                    subtitle = "登录以同步阅读数据",
                    onClick = { }
                )
            }

            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "版本",
                    subtitle = "12.0.0",
                    onClick = { showAboutDialog = true }
                )

                SettingsSwitch(
                    icon = Icons.Default.Backup,
                    title = "备份数据",
                    subtitle = "导出书籍和阅读进度",
                    checked = false,
                    onCheckedChange = { if (it) { viewModel.exportData() } }
                )

                SettingsSwitch(
                    icon = Icons.Default.Restore,
                    title = "恢复数据",
                    subtitle = "从备份文件导入",
                    checked = false,
                    onCheckedChange = { if (it) { viewModel.importData() } }
                )
            }
        }

        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false }
            )
        }
    }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showReaderThemeDialog by remember { mutableStateOf(false) }
    var showPageModeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
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
                PageMode.values().forEach { mode ->
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
}

private fun getReaderThemeName(theme: ReaderTheme): String = when (theme) {
    ReaderTheme.SYSTEM -> "浅色"
    ReaderTheme.LIGHT -> "浅色"
    ReaderTheme.DARK -> "深色"
    ReaderTheme.SEPIA -> "护眼"
    ReaderTheme.PAPER -> "羊皮纸"
    ReaderTheme.AMOLED -> "夜间"
}

private fun getPageModeName(mode: PageMode): String = when (mode) {
    PageMode.SLIDE -> "滑动"
    PageMode.SIMULATION -> "仿真翻页"
    PageMode.NONE -> "无动画"
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
                Text("版本: 11.0.0")
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
