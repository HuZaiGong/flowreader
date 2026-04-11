package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flowreader.app.domain.model.Bookmark
import com.flowreader.app.domain.model.PageMode
import com.flowreader.app.domain.model.ReaderTheme
import com.flowreader.app.domain.model.ReadingSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsDialog(
    settings: ReadingSettings,
    onFontSizeChange: (Int) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onPageModeChange: (PageMode) -> Unit,
    onTtsPlay: () -> Unit,
    onTtsStop: () -> Unit,
    isTtsPlaying: Boolean,
    onDismiss: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    backgroundColor: androidx.compose.ui.graphics.Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("阅读设置") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("语音朗读", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = if (isTtsPlaying) onTtsStop else onTtsPlay,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isTtsPlaying) "停止" else "朗读")
                    }
                }

                HorizontalDivider()

                Text("字体大小: ${settings.fontSize}sp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.fontSize.toFloat(),
                    onValueChange = { onFontSizeChange(it.toInt()) },
                    valueRange = 12f..32f,
                    steps = 19
                )

                Text("行间距: ${String.format("%.1f", settings.lineSpacing)}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.lineSpacing,
                    onValueChange = onLineSpacingChange,
                    valueRange = 1f..2.5f,
                    steps = 14
                )

                Text("阅读主题", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ReaderTheme.values().filter { it != ReaderTheme.SYSTEM }.forEach { theme ->
                        FilterChip(
                            selected = settings.theme == theme,
                            onClick = { onThemeChange(theme) },
                            label = {
                                Text(
                                    when (theme) {
                                        ReaderTheme.LIGHT -> "浅"
                                        ReaderTheme.DARK -> "深"
                                        ReaderTheme.SEPIA -> "护眼"
                                        ReaderTheme.PAPER -> "羊皮"
                                        ReaderTheme.AMOLED -> "夜间"
                                        else -> ""
                                    }
                                )
                            }
                        )
                    }
                }

                Text("翻页模式", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PageMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.pageMode == mode,
                            onClick = { onPageModeChange(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        PageMode.SLIDE -> "滑动"
                                        PageMode.SIMULATION -> "仿真"
                                        PageMode.NONE -> "无"
                                        PageMode.CURL -> "卷曲"
                                        PageMode.SLIDE_OVER -> "滑动覆盖"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
