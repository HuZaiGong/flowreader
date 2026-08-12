package com.flowreader.feature.reader

import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.model.PageMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPositionUnitTest {

    @Test
    fun scrollingTextModesCountCharacters() {
        assertEquals(
            ReaderPositionUnit.CHARACTERS,
            ReaderPositionUnit.of(PageMode.SLIDE, BookFormat.EPUB)
        )
        assertEquals(
            ReaderPositionUnit.CHARACTERS,
            ReaderPositionUnit.of(PageMode.NONE, BookFormat.TXT)
        )
    }

    @Test
    fun pagedTextModeCountsPageIndices() {
        assertEquals(
            ReaderPositionUnit.PAGE_INDEX,
            ReaderPositionUnit.of(PageMode.PAGED, BookFormat.EPUB)
        )
    }

    @Test
    fun comicsCountPageIndicesInEveryPageMode() {
        PageMode.entries.forEach { mode ->
            assertEquals(
                "comic in $mode",
                ReaderPositionUnit.PAGE_INDEX,
                ReaderPositionUnit.of(mode, BookFormat.COMIC)
            )
        }
    }

    @Test
    fun unknownFormatFallsBackToThePageMode() {
        assertEquals(ReaderPositionUnit.CHARACTERS, ReaderPositionUnit.of(PageMode.SLIDE, null))
        assertEquals(ReaderPositionUnit.PAGE_INDEX, ReaderPositionUnit.of(PageMode.PAGED, null))
    }

    @Test
    fun isPageIndexMatchesTheEnumCase() {
        assertTrue(ReaderPositionUnit.PAGE_INDEX.isPageIndex)
        assertFalse(ReaderPositionUnit.CHARACTERS.isPageIndex)
    }

    /** Every text page mode must be classified; a new mode should not silently default wrong. */
    @Test
    fun everyPageModeIsClassifiedForTextBooks() {
        val units = PageMode.entries.map { ReaderPositionUnit.of(it, BookFormat.EPUB) }
        assertEquals(PageMode.entries.size, units.size)
        assertEquals(1, units.count { it.isPageIndex })
    }
}
