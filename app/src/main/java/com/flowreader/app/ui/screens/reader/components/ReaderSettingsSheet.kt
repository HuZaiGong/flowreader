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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowreader.app.R
import com.flowreader.app.core.designsystem.reader.ReaderPalettes
import com.flowreader.app.core.designsystem.reader.background
import com.flowreader.app.core.designsystem.reader.text
import com.flowreader.app.core.designsystem.token.FlowRadius
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.core.util.ReaderBackgroundImage
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
    onPickBackgroundImage: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

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
            SectionTitle(stringResource(R.string.reader_section_typography))

            LabelledValue(stringResource(R.string.reader_setting_font), settings.fontFamily.displayName)
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
                        label = { Text(stringResource(R.string.reader_custom_font)) }
                    )
                }
            }

            LabelledValue(stringResource(R.string.reader_setting_font_size), "${settings.fontSize}sp")
            Slider(
                value = settings.fontSize.toFloat(),
                onValueChange = { onSettingsChange(settings.copy(fontSize = it.toInt())) },
                valueRange = 12f..32f,
                steps = 19
            )

            LabelledValue(stringResource(R.string.reader_setting_line_spacing), formatOneDecimal(settings.lineSpacing))
            Slider(
                value = settings.lineSpacing,
                onValueChange = { onSettingsChange(settings.copy(lineSpacing = it)) },
                valueRange = 1f..2.5f,
                steps = 14
            )

            LabelledValue(
                stringResource(R.string.reader_setting_para_spacing),
                stringResource(R.string.reader_setting_para_spacing_value, formatOneDecimal(settings.paragraphSpacing))
            )
            Slider(
                value = settings.paragraphSpacing,
                onValueChange = { onSettingsChange(settings.copy(paragraphSpacing = it)) },
                valueRange = ReadingSettings.PARAGRAPH_SPACING_MIN..ReadingSettings.PARAGRAPH_SPACING_MAX,
                steps = 10
            )

            SwitchRow(
                label = stringResource(R.string.reader_first_line_indent),
                checked = settings.firstLineIndent,
                onCheckedChange = { onSettingsChange(settings.copy(firstLineIndent = it)) }
            )

            SectionTitle(stringResource(R.string.reader_section_palette))
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
                label = stringResource(R.string.reader_auto_night),
                checked = settings.autoNightMode,
                onCheckedChange = { onSettingsChange(settings.copy(autoNightMode = it)) }
            )
            if (settings.autoNightMode) {
                LabelledValue(stringResource(R.string.reader_setting_night_palette), settings.nightPalette.displayName)
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

            BackgroundImageEditor(
                settings = settings,
                onPickImage = onPickBackgroundImage,
                onClearImage = onClearBackgroundImage,
                onSettingsChange = onSettingsChange
            )

            SectionTitle(stringResource(R.string.reader_section_page_mode))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
                PageMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.pageMode == mode,
                        onClick = { onSettingsChange(settings.copy(pageMode = mode)) },
                        label = { Text(mode.displayName) }
                    )
                }
            }

            SectionTitle(stringResource(R.string.reader_section_eye_protection))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
                listOf(15, 20, 30, 45, 60).forEach { minutes ->
                    FilterChip(
                        selected = settings.eyeProtectionIntervalMinutes == minutes,
                        onClick = { onSettingsChange(settings.copy(eyeProtectionIntervalMinutes = minutes)) },
                        label = { Text(stringResource(R.string.reader_timer_minutes, minutes)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteGrid(ids: List<ReaderPaletteId>, selected: ReaderPaletteId, onSelect: (ReaderPaletteId) -> Unit) {
    val context = LocalContext.current
    FlowRow(
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
                    .semantics { contentDescription = context.getString(R.string.reader_palette_label, id.displayName) },
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
    val context = LocalContext.current
    SectionTitle(stringResource(R.string.reader_section_custom_theme))
    Text(
        text = stringResource(R.string.reader_custom_color_notice),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val fallbackString = stringResource(R.string.reader_setting_color_none)
    LabelledValue(stringResource(R.string.reader_setting_bg_color), formatArgb(settings.customBackgroundColorArgb, fallbackString))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
        BACKGROUND_PRESETS.forEach { (name, argb) ->
            ColorSwatch(
                name = name,
                argb = argb,
                selected = settings.customBackgroundColorArgb == argb,
                onClick = {
                    onSettingsChange(settings.copy(customBackgroundColorArgb = if (settings.customBackgroundColorArgb == argb) null else argb))
                },
                context = context
            )
        }
    }

    LabelledValue(stringResource(R.string.reader_setting_text_color), formatArgb(settings.customTextColorArgb, fallbackString))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
        TEXT_PRESETS.forEach { (name, argb) ->
            ColorSwatch(
                name = name,
                argb = argb,
                selected = settings.customTextColorArgb == argb,
                onClick = {
                    onSettingsChange(settings.copy(customTextColorArgb = if (settings.customTextColorArgb == argb) null else argb))
                },
                context = context
            )
        }
    }

    if (settings.customTextColorArgb != null || settings.customBackgroundColorArgb != null) {
        androidx.compose.material3.TextButton(
            onClick = { onSettingsChange(settings.copy(customTextColorArgb = null, customBackgroundColorArgb = null)) }
        ) {
            Text(stringResource(R.string.reader_restore_palette))
        }
    }
}

/**
 * Reader background image: pick, clear, and set the scrim opacity.
 *
 * The scrim slider deliberately has no "off" position. Reader contrast is asserted against a flat
 * background colour, so text over a bare photo has no measurable readability at all — the image is
 * always behind the palette colour at [ReaderBackgroundImage.MIN_SCRIM_ALPHA] or more. The floor is
 * a range bound, not a guarantee: the minimum that actually clears AA depends on the palette (0.5
 * for e-ink, 0.8 for the lighter dark palettes), so the safe value is computed here and surfaced.
 */
@Composable
private fun BackgroundImageEditor(
    settings: ReadingSettings,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onSettingsChange: (ReadingSettings) -> Unit
) {
    SectionTitle(stringResource(R.string.reader_section_background))
    Text(
        text = stringResource(R.string.reader_background_notice),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val hasImage = ReaderBackgroundImage.isActive(settings.backgroundImagePath)
    Row(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
        androidx.compose.material3.TextButton(onClick = onPickImage) {
            Text(if (hasImage) stringResource(R.string.reader_background_change) else stringResource(R.string.reader_background_pick))
        }
        if (hasImage) {
            androidx.compose.material3.TextButton(onClick = onClearImage) {
                Text(stringResource(R.string.reader_background_remove))
            }
        }
    }

    if (hasImage) {
        val palette = ReaderPalettes.of(settings.palette)
        val safeAlpha = ReaderBackgroundImage.minimumReadableAlpha(
            textArgb = palette.textArgb,
            scrimArgb = palette.backgroundArgb,
            paletteIsDark = palette.isDark
        )
        val alpha = ReaderBackgroundImage.clampScrimAlpha(settings.backgroundScrimAlpha)
        LabelledValue(stringResource(R.string.reader_setting_scrim_alpha), "${(alpha * 100).toInt()}%")
        Slider(
            value = alpha,
            onValueChange = {
                onSettingsChange(settings.copy(backgroundScrimAlpha = ReaderBackgroundImage.clampScrimAlpha(it)))
            },
            valueRange = ReaderBackgroundImage.MIN_SCRIM_ALPHA..ReaderBackgroundImage.MAX_SCRIM_ALPHA
        )
        if (safeAlpha != null && alpha < safeAlpha) {
            Text(
                text = stringResource(R.string.reader_background_min_alpha, (safeAlpha * 100).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ColorSwatch(name: String, argb: Long, selected: Boolean, onClick: () -> Unit, context: android.content.Context) {
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
            .semantics { contentDescription = context.getString(R.string.reader_custom_color_label, name) }
    )
}

private fun formatArgb(argb: Long?, fallback: String): String = argb?.let { "#%08X".format(it) } ?: fallback

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
