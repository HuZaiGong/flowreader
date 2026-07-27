package com.flowreader.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverArtTest {

    @Test
    fun hashIsStableForTheSameSeed() {
        assertEquals(CoverArt.hash("心流|米哈里"), CoverArt.hash("心流|米哈里"))
    }

    @Test
    fun differentSeedsUsuallyLandOnDifferentSlots() {
        val slots = listOf("心流", "深度工作", "断舍离", "人类简史", "白夜行", "三体")
            .map { CoverArt.paletteIndex(it, 8) }
        assertTrue("expected some spread, got $slots", slots.toSet().size > 1)
    }

    @Test
    fun paletteIndexStaysInRangeIncludingNegativeHashes() {
        repeat(500) { index ->
            val result = CoverArt.paletteIndex("book-$index", 8)
            assertTrue("index $result out of range for seed book-$index", result in 0..7)
        }
    }

    @Test
    fun paletteIndexSurvivesAnEmptyPalette() {
        assertEquals(0, CoverArt.paletteIndex("anything", 0))
    }

    @Test
    fun initialsTakeOneGlyphForCjk() {
        assertEquals("心", CoverArt.initials("心流"))
        assertEquals("三", CoverArt.initials("  三体 "))
    }

    @Test
    fun initialsTakeTwoLettersForLatin() {
        assertEquals("DW", CoverArt.initials("Deep Work"))
        assertEquals("TP", CoverArt.initials("the-pragmatic-programmer"))
        assertEquals("H", CoverArt.initials("hamlet"))
    }

    @Test
    fun initialsNeverReturnBlank() {
        assertEquals("?", CoverArt.initials(""))
        assertEquals("?", CoverArt.initials("   "))
    }
}
