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
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.BuildConfig
import com.flowreader.app.R
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.ui.screens.settings.ShelfExportFormat
import com.flowreader.app.domain.model.AppLanguage
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
    var showShelfExportDialog by remember { mutableStateOf(false) }
    var showLanTransferDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.onExportReady(uri) }

    val shelfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri -> if (uri != null) viewModel.onShelfExportReady(uri) }

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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.settings_theme_mode),
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
                    title = stringResource(R.string.settings_color_source),
                    subtitle = stringResource(R.string.settings_color_source_desc)
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

                // v53: the app shipped four `values-*` folders that nothing could ever select.
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_desc)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
                    modifier = Modifier.padding(horizontal = FlowSpacing.lg, vertical = FlowSpacing.sm)
                ) {
                    AppLanguage.entries.forEach { language ->
                        FilterChip(
                            selected = uiState.language == language,
                            onClick = { viewModel.updateLanguage(language) },
                            label = {
                                Text(
                                    if (language.tag == null) {
                                        stringResource(R.string.settings_language_follow_system)
                                    } else {
                                        language.displayName
                                    }
                                )
                            }
                        )
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.settings_section_reading)) {
                SettingsItem(
                    icon = Icons.Default.TextFields,
                    title = stringResource(R.string.settings_typography),
                    subtitle = stringResource(R.string.settings_typography_desc)
                )

                SettingsItem(
                    icon = Icons.Default.Gesture,
                    title = stringResource(R.string.settings_gesture),
                    subtitle = stringResource(R.string.settings_gesture_desc),
                    onClick = { showGestureDialog = true }
                )

                SettingsSwitch(
                    icon = Icons.Default.WbSunny,
                    title = stringResource(R.string.settings_keep_screen_on),
                    checked = uiState.readingSettings.keepScreenOn,
                    onCheckedChange = { viewModel.updateKeepScreenOn(it) }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_font)) {
                val fontName = remember(uiState.customFontPath) {
                    uiState.customFontPath?.let { path ->
                        runCatching { File(path).takeIf { it.isFile }?.name }.getOrNull()
                    }
                }

                SettingsItem(
                    icon = Icons.Default.TextFields,
                    title = stringResource(R.string.settings_custom_font),
                    subtitle = fontName ?: stringResource(R.string.settings_custom_font_desc),
                    onClick = { fontPickerLauncher.launch("*/*") }
                )

                if (uiState.customFontPath != null) {
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.settings_clear_font),
                        subtitle = stringResource(R.string.settings_clear_font_desc),
                        onClick = { viewModel.clearCustomFont() }
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.settings_section_reminder)) {
                SettingsSwitch(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_daily_reminder),
                    subtitle = stringResource(
                        R.string.settings_daily_reminder_time,
                        uiState.readingReminderHour,
                        uiState.readingReminderMinute
                    ),
                    checked = uiState.readingReminderEnabled,
                    onCheckedChange = { viewModel.updateReadingReminder(it) }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_goal)) {
                SettingsItem(
                    icon = Icons.Default.Flag,
                    title = stringResource(R.string.settings_daily_goal),
                    subtitle = stringResource(R.string.settings_minutes, uiState.dailyReadingGoal),
                    onClick = { showReadingGoalDialog = true }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_data)) {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = stringResource(R.string.settings_backup),
                    subtitle = stringResource(R.string.settings_backup_desc),
                    onClick = {
                        viewModel.exportData()
                        exportLauncher.launch("flowreader-backup.json")
                    }
                )

                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = stringResource(R.string.settings_restore),
                    subtitle = stringResource(R.string.settings_restore_desc),
                    onClick = {
                        viewModel.importData()
                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                    }
                )

                SettingsItem(
                    icon = Icons.Default.TableChart,
                    title = stringResource(R.string.settings_export_shelf),
                    subtitle = stringResource(R.string.settings_export_shelf_desc),
                    onClick = { showShelfExportDialog = true }
                )

                SettingsItem(
                    icon = Icons.Default.Wifi,
                    title = stringResource(R.string.settings_lan_transfer),
                    subtitle = stringResource(R.string.settings_lan_transfer_desc),
                    onClick = { showLanTransferDialog = true }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_about),
                    subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
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
        title = { Text(stringResource(R.string.settings_goal_dialog_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.settings_minutes, goal),
                    style = MaterialTheme.typography.headlineMedium
                )
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
                    Text(stringResource(R.string.settings_goal_min), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.settings_goal_max), style = MaterialTheme.typography.bodySmall)
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
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_about)) },
        text = {
            Column {
                Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME))
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                Text(stringResource(R.string.settings_about_desc))
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                Text(stringResource(R.string.settings_about_thanks))
                Text("- Jetpack Compose", style = MaterialTheme.typography.bodySmall)
                Text("- Room Database", style = MaterialTheme.typography.bodySmall)
                Text("- Hilt", style = MaterialTheme.typography.bodySmall)
                Text("- Coil", style = MaterialTheme.typography.bodySmall)
                Text("- jsoup", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                Text(stringResource(R.string.settings_about_author), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.settings_about_github), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
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
        title = { Text(stringResource(R.string.settings_gesture_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(FlowSpacing.md)
            ) {
                GestureRow(stringResource(R.string.gesture_left_tap), localSettings.leftTapAction) {
                    localSettings = localSettings.copy(leftTapAction = it)
                }
                GestureRow(stringResource(R.string.gesture_middle_tap), localSettings.middleTapAction) {
                    localSettings = localSettings.copy(middleTapAction = it)
                }
                GestureRow(stringResource(R.string.gesture_right_tap), localSettings.rightTapAction) {
                    localSettings = localSettings.copy(rightTapAction = it)
                }
                GestureRow(stringResource(R.string.gesture_swipe_left), localSettings.swipeLeftAction) {
                    localSettings = localSettings.copy(swipeLeftAction = it)
                }
                GestureRow(stringResource(R.string.gesture_swipe_right), localSettings.swipeRightAction) {
                    localSettings = localSettings.copy(swipeRightAction = it)
                }
                GestureRow(stringResource(R.string.gesture_double_tap), localSettings.doubleTapAction) {
                    localSettings = localSettings.copy(doubleTapAction = it)
                }
                GestureRow(stringResource(R.string.gesture_long_press), localSettings.longPressAction) {
                    localSettings = localSettings.copy(longPressAction = it)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.gesture_edge_zone))
                    Switch(
                        checked = localSettings.edgeGestureEnabled,
                        onCheckedChange = { localSettings = localSettings.copy(edgeGestureEnabled = it) }
                    )
                }

                if (localSettings.edgeGestureEnabled) {
                    Text(
                        text = stringResource(R.string.gesture_left_edge, localSettings.leftEdgeWidth),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = localSettings.leftEdgeWidth.toFloat(),
                        onValueChange = { localSettings = localSettings.copy(leftEdgeWidth = it.toInt()) },
                        valueRange = 5f..45f
                    )
                    Text(
                        text = stringResource(R.string.gesture_right_edge, localSettings.rightEdgeWidth),
                        style = MaterialTheme.typography.bodySmall
                    )
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
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
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
