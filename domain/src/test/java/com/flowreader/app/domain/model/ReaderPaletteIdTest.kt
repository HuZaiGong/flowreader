package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPaletteIdTest {

    @Test
    fun twelveBuiltInPalettesSplitIntoLightAndDark() {
        assertEquals(12, ReaderPaletteId.entries.size)
        assertEquals(7, ReaderPaletteId.LIGHT_PALETTES.size)
        assertEquals(5, ReaderPaletteId.DARK_PALETTES.size)
        assertTrue(ReaderPaletteId.LIGHT_PALETTES.none { it.isDark })
        assertTrue(ReaderPaletteId.DARK_PALETTES.all { it.isDark })
    }

    @Test
    fun everyPaletteHasADisplayName() {
        assertTrue(ReaderPaletteId.entries.all { it.displayName.isNotBlank() })
    }

    @Test
    fun legacyReaderThemeValuesMigrateOntoPalettes() {
        // The `reader_theme` DataStore key used to hold LIGHT / DARK; the slot is reused.
        assertEquals(ReaderPaletteId.PAPER, ReaderPaletteId.fromStoredName("LIGHT"))
        assertEquals(ReaderPaletteId.NIGHT, ReaderPaletteId.fromStoredName("DARK"))
    }

    @Test
    fun unknownAndMissingValuesFallBack() {
        assertEquals(ReaderPaletteId.PAPER, ReaderPaletteId.fromStoredName(null))
        assertEquals(ReaderPaletteId.PAPER, ReaderPaletteId.fromStoredName("NOT_A_PALETTE"))
        assertEquals(ReaderPaletteId.OLED, ReaderPaletteId.fromStoredName("nope", ReaderPaletteId.OLED))
    }

    @Test
    fun storedNamesRoundTrip() {
        ReaderPaletteId.entries.forEach { id ->
            assertEquals(id, ReaderPaletteId.fromStoredName(id.name))
        }
    }

    @Test
    fun eInkIsALightPalette() {
        assertFalse(ReaderPaletteId.EINK.isDark)
    }
}
