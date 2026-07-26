package com.flowreader.app.core.designsystem.reader

import androidx.compose.ui.graphics.Color
import com.flowreader.app.domain.model.ReaderPaletteId

/**
 * A reader color palette.
 *
 * Values are plain ARGB longs rather than Compose `Color`s so the whole table stays a pure-Kotlin
 * value that JVM unit tests (including the WCAG contrast assertions) can read without Compose.
 * Use the [background]/[text]/[secondaryText]/[selection] extensions below for rendering.
 */
data class ReaderPalette(
    val id: ReaderPaletteId,
    val backgroundArgb: Long,
    val textArgb: Long,
    val secondaryTextArgb: Long,
    val selectionArgb: Long
) {
    val isDark: Boolean get() = id.isDark
    val displayName: String get() = id.displayName
}

/** The 12 built-in reader palettes. Every entry is contrast-checked in `ReaderPaletteContrastTest`. */
object ReaderPalettes {
    private const val LIGHT_SELECTION = 0x556750A4L
    private const val DARK_SELECTION = 0x66D0BCFFL

    val Paper = ReaderPalette(ReaderPaletteId.PAPER, 0xFFFFFFFF, 0xFF1A1A1A, 0xFF5F5F5F, LIGHT_SELECTION)
    val Sepia = ReaderPalette(ReaderPaletteId.SEPIA, 0xFFF5EFE0, 0xFF3A3226, 0xFF6B6154, LIGHT_SELECTION)
    val Green = ReaderPalette(ReaderPaletteId.GREEN, 0xFFCCE8CF, 0xFF1E2B1F, 0xFF46584A, LIGHT_SELECTION)
    val Linen = ReaderPalette(ReaderPaletteId.LINEN, 0xFFEDE6D6, 0xFF33302A, 0xFF635F55, LIGHT_SELECTION)
    val Mist = ReaderPalette(ReaderPaletteId.MIST, 0xFFE9EEF2, 0xFF22282C, 0xFF515A61, LIGHT_SELECTION)
    val CoolGray = ReaderPalette(ReaderPaletteId.COOL_GRAY, 0xFFDDE1E4, 0xFF23272A, 0xFF52585C, LIGHT_SELECTION)
    val EInk = ReaderPalette(ReaderPaletteId.EINK, 0xFFF2F2F0, 0xFF000000, 0xFF4A4A48, LIGHT_SELECTION)
    val Night = ReaderPalette(ReaderPaletteId.NIGHT, 0xFF121212, 0xFFD7D7D7, 0xFF9A9A9A, DARK_SELECTION)
    val InkBlue = ReaderPalette(ReaderPaletteId.INK_BLUE, 0xFF101822, 0xFFC6D3E0, 0xFF8B9AAA, DARK_SELECTION)
    val DeepBrown = ReaderPalette(ReaderPaletteId.DEEP_BROWN, 0xFF1E1712, 0xFFDCCFC0, 0xFFA2907E, DARK_SELECTION)
    val Obsidian = ReaderPalette(ReaderPaletteId.OBSIDIAN, 0xFF191B20, 0xFFCFD3DA, 0xFF939AA5, DARK_SELECTION)
    val Oled = ReaderPalette(ReaderPaletteId.OLED, 0xFF000000, 0xFFC8C8C8, 0xFF8A8A8A, DARK_SELECTION)

    val all: List<ReaderPalette> = listOf(
        Paper,
        Sepia,
        Green,
        Linen,
        Mist,
        CoolGray,
        EInk,
        Night,
        InkBlue,
        DeepBrown,
        Obsidian,
        Oled
    )

    private val byId: Map<ReaderPaletteId, ReaderPalette> = all.associateBy { it.id }

    fun of(id: ReaderPaletteId): ReaderPalette = byId.getValue(id)
}

val ReaderPalette.background: Color get() = Color(backgroundArgb)
val ReaderPalette.text: Color get() = Color(textArgb)
val ReaderPalette.secondaryText: Color get() = Color(secondaryTextArgb)
val ReaderPalette.selection: Color get() = Color(selectionArgb)
