package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookModelTest {
    @Test
    fun defaultBookKeepsUnreadProgressAndTags() {
        val book = Book(title = "Title", author = "Author", filePath = "/books/title.epub")

        assertEquals(BookFormat.EPUB, book.format)
        assertEquals(0, book.currentChapter)
        assertEquals(0, book.currentPosition)
        assertEquals(0f, book.readingProgress)
        assertTrue(book.tags.isEmpty())
    }
}
