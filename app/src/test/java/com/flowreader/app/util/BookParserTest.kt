package com.flowreader.app.util

import com.flowreader.app.domain.model.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class BookParserTest {

    @Test
    fun testDetectFormat() {
        assertEquals(BookFormat.EPUB, BookParser.detectFormatStatic("book.epub"))
        assertEquals(BookFormat.TXT, BookParser.detectFormatStatic("book.txt"))
        assertEquals(BookFormat.PDF, BookParser.detectFormatStatic("book.pdf"))
        assertEquals(BookFormat.MARKDOWN, BookParser.detectFormatStatic("book.md"))
        assertEquals(BookFormat.MARKDOWN, BookParser.detectFormatStatic("book.markdown"))
        assertEquals(BookFormat.UNKNOWN, BookParser.detectFormatStatic("book.exe"))
    }
}
