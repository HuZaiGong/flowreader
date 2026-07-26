package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderFontFamilyTest {

    @Test
    fun onlyFacesThePlatformCanResolveAreOffered() {
        // v51 offered 宋体/黑体/楷体/仿宋 as separate chips that all resolved to the same
        // system face; the enum now lists only what actually renders differently.
        assertEquals(
            listOf(
                ReaderFontFamily.DEFAULT,
                ReaderFontFamily.SERIF,
                ReaderFontFamily.SANS_SERIF,
                ReaderFontFamily.MONOSPACE
            ),
            ReaderFontFamily.entries.toList()
        )
    }

    @Test
    fun retiredCjkNamesMapOntoTheirRealFace() {
        assertEquals(ReaderFontFamily.SERIF, ReaderFontFamily.fromStoredName("SONG"))
        assertEquals(ReaderFontFamily.SERIF, ReaderFontFamily.fromStoredName("FANGSONG"))
        assertEquals(ReaderFontFamily.SERIF, ReaderFontFamily.fromStoredName("KAI"))
        assertEquals(ReaderFontFamily.SANS_SERIF, ReaderFontFamily.fromStoredName("HEI"))
    }

    @Test
    fun unknownAndMissingValuesFallBackToDefault() {
        assertEquals(ReaderFontFamily.DEFAULT, ReaderFontFamily.fromStoredName(null))
        assertEquals(ReaderFontFamily.DEFAULT, ReaderFontFamily.fromStoredName("COMIC_SANS"))
    }

    @Test
    fun storedNamesRoundTrip() {
        ReaderFontFamily.entries.forEach { family ->
            assertEquals(family, ReaderFontFamily.fromStoredName(family.name))
        }
    }

    @Test
    fun everyFaceHasADisplayName() {
        assertTrue(ReaderFontFamily.entries.all { it.displayName.isNotBlank() })
    }
}
