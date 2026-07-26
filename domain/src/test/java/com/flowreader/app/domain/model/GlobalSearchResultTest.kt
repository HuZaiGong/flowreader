package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GlobalSearchResultTest {
    @Test
    fun globalSearchResultCarriesBookTitleForLibrarySearch() {
        val result = GlobalSearchResult(
            bookId = 9L,
            bookTitle = "Book",
            chapterIndex = 4,
            chapterTitle = "Chapter",
            matchedText = "keyword context"
        )

        assertEquals(9L, result.bookId)
        assertEquals("Book", result.bookTitle)
        assertEquals(4, result.chapterIndex)
        assertEquals("keyword context", result.matchedText)
    }
}
