package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingSettingsTest {
    @Test
    fun defaultReadingSettingsMatchReaderDefaults() {
        val settings = ReadingSettings()

        assertEquals(18, settings.fontSize)
        assertEquals(1.5f, settings.lineSpacing)
        assertEquals(1.0f, settings.paragraphSpacing)
        assertEquals(ReaderPaletteId.PAPER, settings.palette)
        assertEquals(ReaderPaletteId.NIGHT, settings.nightPalette)
        assertEquals(PageMode.SLIDE, settings.pageMode)
        assertTrue(settings.firstLineIndent)
        assertTrue(settings.keepScreenOn)
        assertEquals(20, settings.eyeProtectionIntervalMinutes)
    }

    @Test
    fun paragraphSpacingKeepsValuesInsideTheMultiplierDomain() {
        assertEquals(1.0f, ReadingSettings.normalizeParagraphSpacing(1.0f))
        assertEquals(2.5f, ReadingSettings.normalizeParagraphSpacing(2.5f))
        assertEquals(ReadingSettings.PARAGRAPH_SPACING_MIN, ReadingSettings.normalizeParagraphSpacing(0f))
    }

    @Test
    fun legacyDpParagraphSpacingResetsToTheDefault() {
        // Values stored before v52 were raw dp, so anything above the multiplier ceiling is junk.
        assertEquals(1.0f, ReadingSettings.normalizeParagraphSpacing(8f))
        assertEquals(1.0f, ReadingSettings.normalizeParagraphSpacing(24f))
        assertEquals(1.0f, ReadingSettings.normalizeParagraphSpacing(Float.NaN))
    }

    @Test
    fun appSettingsDefaultToBrandColorAndSystemTheme() {
        val app = AppSettings()
        assertEquals(AppThemeMode.FOLLOW_SYSTEM, app.themeMode)
        assertEquals(ColorSource.BRAND, app.colorSource)
    }
}
