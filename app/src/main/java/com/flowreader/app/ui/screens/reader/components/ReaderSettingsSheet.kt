package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowreader.app.core.designsystem.reader.ReaderPalettes
import com.flowreader.app.core.designsystem.reader.background
import com.flowreader.app.core.designsystem.reader.text
import com.flowreader.app.core.designsystem.token.FlowRadius
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.domain.model.PageMode
import com.flowreader.app.domain.model.ReaderFontFamily
import com.flowreader.app.domain.model.ReaderPaletteId
import com.flowreader.app.domain.model.ReadingSettings

/**
 * The reader preference panel.
 *
 * Replaces the v51 `AlertDialog`: a bottom sheet keeps the body visible so every change previews
 * live, and each control here is now actually consumed by the renderer.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReadingSettings,
    onSettingsChange: (ReadingSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = FlowSpacing.lg)
                .padding(bottom = FlowSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FlowSpacing.lg)
        ) {
            SectionTitle("排版")

            LabelledValue("字体", settings.fontFamily.displayName)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
                ReaderFontFamily.entries.forEach { font ->
                    FilterChip(
                        selected = settings.fontFamily == font && settings.customFontPath == null,
                        onClick = { onSettingsChange(settings.copy(fontFamily = font, customFontPath = null)) },
                        label = { Text(font.displayName) }
                    )
                }
                if (settings.customFontPath != null) {
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("自定义字体") }
                    )
                }
            }

            LabelledValue("字号", "${settings.fontSize}sp")
            Slider(
                value = settings.fontSize.toFloat(),
                onValueChange = { onSettingsChange(settings.copy(fontSize = it.toInt())) },
                valueRange = 12f..32f,
                steps = 19
            )

            LabelledValue("行间距", formatOneDecimal(settings.lineSpacing))
            Slider(
                value = settings.lineSpacing,
                onValueChange = { onSettingsChange(settings.copy(lineSpacing = it)) },
                valueRange = 1f..2.5f,
                steps = 14
            )

            LabelledValue("段间距", formatOneDecimal(settings.paragraphSpacing) + " 倍字号")
            Slider(
                value = settings.paragraphSpacing,
                onValueChange = { onSettingsChange(settings.copy(paragraphSpacing = it)) },
                valueRange = ReadingSettings.PARAGRAPH_SPACING_MIN..ReadingSettings.PARAGRAPH_SPACING_MAX,
                steps = 10
            )

            SwitchRow(
                label = "首行缩进两字",
                checked = settings.firstLineIndent,
                onCheckedChange = { onSettingsChange(settings.copy(firstLineIndent = it)) }
            )

            SectionTitle("阅读主题")
            PaletteGrid(
                ids = ReaderPaletteId.LIGHT_PALETTES,
                selected = settings.palette,
                onSelect = { onSettingsChange(settings.copy(palette = it)) }
            )
            PaletteGrid(
                ids = ReaderPaletteId.DARK_PALETTES,
                selected = settings.palette,
                onSelect = { onSettingsChange(settings.copy(palette = it)) }
            )

            SwitchRow(
                label = "自动夜间模式 (19:00–07:00)",
                checked = settings.autoNightMode,
                onCheckedChange = { onSettingsChange(settings.copy(autoNightMode = it)) }
            )
            if (settings.autoNightMode) {
                LabelledValue("夜间色板", settings.nightPalette.displayName)
                PaletteGrid(
                    ids = ReaderPaletteId.DARK_PALETTES,
                    selected = settings.nightPalette,
                    onSelect = { onSettingsChange(settings.copy(nightPalette = it)) }
                )
            }

            CustomThemeEditor(
                settings = settings,
                onSettingsChange = onSettingsChange
            )

            SectionTitle("翻页")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
                PageMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.pageMode == mode,
                        onClick = { onSettingsChange(settings.copy(pageMode = mode)) },
                        label = { Text(mode.displayName) }
                    )
                }
            }

            SectionTitle("护眼提醒间隔")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
                listOf(15, 20, 30, 45, 60).forEach { minutes ->
                    FilterChip(
                        selected = settings.eyeProtectionIntervalMinutes == minutes,
                        onClick = { onSettingsChange(settings.copy(eyeProtectionIntervalMinutes = minutes)) },
                        label = { Text("$minutes 分钟") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteGrid(ids: List<ReaderPaletteId>, selected: ReaderPaletteId, onSelect: (ReaderPaletteId) -> Unit) {    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
        modifier = Modifier.fillMaxWidth()
    ) {
        ids.forEach { id ->
            val palette = ReaderPalettes.of(id)
            val isSelected = selected == id
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(FlowRadius.md))
                    .background(palette.background)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(FlowRadius.md)
                    )
                    .clickable { onSelect(id) }
                    .semantics { contentDescription = "阅读主题 ${id.displayName}" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = id.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.text
                )
            }
        }
    }
}

/**
 * Custom reader colors (v55): user-picked background/text colors override the active palette;
 * the contrast fallback happens in `:core` `ReaderCustomTheme`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomThemeEditor(settings: ReadingSettings, onSettingsChange: (ReadingSettings) -> Unit) {
    SectionTitle("自定义主题")
    Text(
        text = "自定义背景与文字色会覆盖所选色板；若对比度不达标会自动回退到可读配色。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    LabelledValue("背景色", formatArgb(settings.customBackgroundColorArgb))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
        BACKGROUND_PRESETS.forEach { (name, argb) ->
            ColorSwatch(
                name = name,
                argb = argb,
                selected = settings.customBackgroundColorArgb == argb,
                onClick = {
                    onSettingsChange(settings.copy(customBackgroundColorArgb = if (settings.customBackgroundColorArgb == argb) null else argb))
                }
            )
        }
    }

    LabelledValue("文字色", formatArgb(settings.customTextColorArgb))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
        TEXT_PRESETS.forEach { (name, argb) ->
            ColorSwatch(
                name = name,
                argb = argb,
                selected = settings.customTextColorArgb == argb,
                onClick = {
                    onSettingsChange(settings.copy(customTextColorArgb = if (settings.customTextColorArgb == argb) null else argb))
                }
            )
        }
    }

    if (settings.customTextColorArgb != null || settings.customBackgroundColorArgb != null) {
        androidx.compose.material3.TextButton(
            onClick = { onSettingsChange(settings.copy(customTextColorArgb = null, customBackgroundColorArgb = null)) }
        ) {
            Text("恢复色板默认")
        }
    }
}

@Composable
private fun ColorSwatch(name: String, argb: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(FlowRadius.md))
            .background(androidx.compose.ui.graphics.Color(argb))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(FlowRadius.md)
            )
            .clickable { onClick() }
            .semantics { contentDescription = "自定义主题色 $name" }
    )
}

private fun formatArgb(argb: Long?): String = argb?.let { "#%08X".format(it) } ?: "跟随色板"

private val BACKGROUND_PRESETS = listOf(
    "纯白" to 0xFFFFFFFFL,
    "米黄" to 0xFFF5EFE0L,
    "护眼绿" to 0xFFCCE8CFL,
    "晨雾" to 0xFFE9EEF2L,
    "冷灰" to 0xFFDDE1E4L,
    "夜黑" to 0xFF121212L,
    "墨蓝" to 0xFF101822L,
    "纯黑" to 0xFF000000L
)

private val TEXT_PRESETS = listOf(
    "近黑" to 0xFF1A1A1AL,
    "深灰" to 0xFF3A3226L,
    "墨蓝" to 0xFF22282CL,
    "深棕" to 0xFF33302AL,
    "白色" to 0xFFD7D7D7L,
    "浅灰" to 0xFF9A9A9AL,
    "米白" to 0xFFDCCFC0L,
    "青灰" to 0xFFC6D3E0L
)

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = FlowSpacing.sm)
    )
}

@Composable
private fun LabelledValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatOneDecimal(value: Float): String {
    val tenths = (value * 10).toInt()
    return "${tenths / 10}.${tenths % 10}"
}
