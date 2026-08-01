package com.flowreader.app.core.util

import com.flowreader.app.core.designsystem.reader.ReaderPalettes
import com.flowreader.app.domain.model.ReaderPaletteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderCustomThemeTest {

    @Test
    fun nullCustomColorsReturnPaletteAsIs() {
        val palette = ReaderPalettes.of(ReaderPaletteId.PAPER)
        val resolved = ReaderCustomTheme.resolve(palette, null, null)
        assertEquals(palette, resolved)
    }

    @Test
    fun customPairOverridesPalette() {
        val palette = ReaderPalettes.of(ReaderPaletteId.PAPER)
        val resolved = ReaderCustomTheme.resolve(palette, 0xFF000000L, 0xFFFFFFFFL)
        assertEquals(0xFF000000L, resolved.textArgb)
        assertEquals(0xFFFFFFFFL, resolved.backgroundArgb)
    }

    @Test
    fun unreadableCustomPairFallsBackToPalette() {
        val palette = ReaderPalettes.of(ReaderPaletteId.PAPER)
        // White text on white background fails AA body text.
        val resolved = ReaderCustomTheme.resolve(palette, 0xFFFFFFFFL, 0xFFFFFFFFL)
        assertEquals(palette.textArgb, resolved.textArgb)
        assertEquals(palette.backgroundArgb, resolved.backgroundArgb)
    }

    @Test
    fun customTextRevertsToPaletteTextWhenBackgroundIsCustom() {
        val palette = ReaderPalettes.of(ReaderPaletteId.PAPER)
        // Paper background + near-white custom text: fails AA -> text falls back to palette ink.
        val resolved = ReaderCustomTheme.resolve(palette, 0xFFF0F0F0L, 0xFFFFFFFFL)
        assertEquals(palette.textArgb, resolved.textArgb)
        assertEquals(0xFFFFFFFFL, resolved.backgroundArgb)
    }

    @Test
    fun customBackgroundRevertsToPaletteBackgroundWhenTextIsCustom() {
        val palette = ReaderPalettes.of(ReaderPaletteId.PAPER)
        // Palette ink + custom mid-gray background: ratio ~4.3 < 4.5 -> background falls back.
        val resolved = ReaderCustomTheme.resolve(palette, 0xFF1A1A1AL, 0xFF7F7F7FL)
        assertEquals(palette.backgroundArgb, resolved.backgroundArgb)
    }

    @Test
    fun paletteIdentityAndSelectionArePreserved() {
        val palette = ReaderPalettes.of(ReaderPaletteId.NIGHT)
        val resolved = ReaderCustomTheme.resolve(palette, 0xFFFFFFFFL, 0xFF000000L)
        assertEquals(ReaderPaletteId.NIGHT, resolved.id)
        assertEquals(palette.secondaryTextArgb, resolved.secondaryTextArgb)
        assertEquals(palette.selectionArgb, resolved.selectionArgb)
    }

    @Test
    fun allPresetPairsPassAaBodyText() {
        val palette = ReaderPalettes.of(ReaderPaletteId.PAPER)
        val backgrounds = listOf(
            0xFFFFFFFFL,
            0xFFF5EFE0L,
            0xFFCCE8CFL,
            0xFFE9EEF2L,
            0xFFDDE1E4L,
            0xFF121212L,
            0xFF101822L,
            0xFF000000L
        )
        val texts = listOf(
            0xFF1A1A1AL,
            0xFF3A3226L,
            0xFF22282CL,
            0xFF33302AL,
            0xFFD7D7D7L,
            0xFF9A9A9AL,
            0xFFDCCFC0L,
            0xFFC6D3E0L
        )
        backgrounds.forEach { background ->
            texts.forEach { text ->
                val resolved = ReaderCustomTheme.resolve(palette, text, background)
                assertNull(
                    "pair text=#%08X bg=#%08X resolved unreadable",
                    resolved.takeIf { !ColorContrast.meetsAaBodyText(it.textArgb, it.backgroundArgb) }
                )
            }
        }
    }
}
