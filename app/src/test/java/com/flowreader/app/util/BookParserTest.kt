package com.flowreader.app.util

import com.flowreader.app.domain.model.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BookParserTest {

    @Test
    fun testDetectFormat() {
        assertEquals(BookFormat.EPUB, detectFormatTest("book.epub"))
        assertEquals(BookFormat.TXT, detectFormatTest("book.txt"))
        assertEquals(BookFormat.PDF, detectFormatTest("book.pdf"))
        assertEquals(BookFormat.MARKDOWN, detectFormatTest("book.md"))
        assertEquals(BookFormat.MARKDOWN, detectFormatTest("book.markdown"))
        assertEquals(BookFormat.UNKNOWN, detectFormatTest("book.exe"))
    }

    @Test
    fun safeFileNameRemovesPathSegments() {
        val fileName = SafeFileNames.forInternalBook("../../evil/Book Name.epub")

        assertEquals("Book Name.epub", fileName)
        assertFalse(fileName.contains('/'))
        assertFalse(fileName.contains('\\'))
    }

    @Test
    fun safeFileNameFallsBackForUnsafeNames() {
        assertEquals("book.txt", SafeFileNames.forInternalBook("..", "txt"))
        assertEquals("cover.jpg", SafeFileNames.forCover("../cover.png"))
    }

    private fun detectFormatTest(fileName: String): BookFormat {
        return when {
            fileName.endsWith(".epub", ignoreCase = true) -> BookFormat.EPUB
            fileName.endsWith(".txt", ignoreCase = true) -> BookFormat.TXT
            fileName.endsWith(".pdf", ignoreCase = true) -> BookFormat.PDF
            fileName.endsWith(".md", ignoreCase = true) || fileName.endsWith(".markdown", ignoreCase = true) -> BookFormat.MARKDOWN
            else -> BookFormat.UNKNOWN
        }
    }
}