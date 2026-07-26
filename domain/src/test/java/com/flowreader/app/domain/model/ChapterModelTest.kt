package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterModelTest {
    @Test
    fun chapterDefaultsToEmptyContentAndPositions() {
        val chapter = Chapter(bookId = 7L, index = 2, title = "Chapter 2")

        assertEquals(7L, chapter.bookId)
        assertEquals(2, chapter.index)
        assertEquals("", chapter.content)
        assertEquals(0, chapter.startPosition)
        assertEquals(0, chapter.endPosition)
    }
}
