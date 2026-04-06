package com.flowreader.app.util

import com.flowreader.app.domain.model.BookFormat
import org.junit.Assert.*
import org.junit.Test

class BookParserTest {

    @Test
    fun testDetectFormat() {
        assertEquals(BookFormat.EPUB, detectFormatTest("book.epub"))
        assertEquals(BookFormat.TXT, detectFormatTest("book.txt"))
        assertEquals(BookFormat.PDF, detectFormatTest("book.pdf"))
        assertEquals(BookFormat.MARKDOWN, detectFormatTest("book.md"))
        assertEquals(BookFormat.UNKNOWN, detectFormatTest("book.exe"))
    }

    private fun detectFormatTest(fileName: String): BookFormat {
        return when {
            fileName.endsWith(".epub", ignoreCase = true) -> BookFormat.EPUB
            fileName.endsWith(".txt", ignoreCase = true) -> BookFormat.TXT
            fileName.endsWith(".pdf", ignoreCase = true) -> BookFormat.PDF
            fileName.endsWith(".md", ignoreCase = true) -> BookFormat.MARKDOWN
            else -> BookFormat.UNKNOWN
        }
    }
}
