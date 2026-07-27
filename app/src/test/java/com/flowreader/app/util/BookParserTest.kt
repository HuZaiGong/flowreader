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

    @Test
    fun v53FormatsAreRecognised() {
        assertEquals(BookFormat.FB2, BookParser.detectFormatStatic("novel.fb2"))
        assertEquals(BookFormat.MOBI, BookParser.detectFormatStatic("novel.mobi"))
        assertEquals(BookFormat.MOBI, BookParser.detectFormatStatic("novel.prc"))
        assertEquals(BookFormat.MOBI, BookParser.detectFormatStatic("novel.azw"))
    }

    @Test
    fun comicImageFormatsAreRecognised() {
        assertEquals(BookFormat.COMIC, BookParser.detectFormatStatic("page.jpg"))
        assertEquals(BookFormat.COMIC, BookParser.detectFormatStatic("page.jpeg"))
        assertEquals(BookFormat.COMIC, BookParser.detectFormatStatic("page.png"))
        assertEquals(BookFormat.COMIC, BookParser.detectFormatStatic("page.webp"))
        assertEquals(BookFormat.COMIC, BookParser.detectFormatStatic("PAGE.WEBP"))
    }

    @Test
    fun comicPagesHaveNaturalSortKeys() {
        val pages = listOf("page10.jpg", "page2.jpg", "page1.jpg")

        assertEquals(
            listOf("page1.jpg", "page2.jpg", "page10.jpg"),
            pages.sortedBy { BookParser.naturalComicSortKey(it).joinToString("/") }
        )
    }

    @Test
    fun detectionIsCaseInsensitive() {
        assertEquals(BookFormat.EPUB, BookParser.detectFormatStatic("BOOK.EPUB"))
        assertEquals(BookFormat.MOBI, BookParser.detectFormatStatic("Novel.MOBI"))
        assertEquals(BookFormat.FB2, BookParser.detectFormatStatic("Novel.FB2"))
    }

    @Test
    fun anFb2ArchiveIsABookButABareZipIsNot() {
        // The library import path keys off exactly this distinction: `.fb2.zip` parses as a book,
        // a plain `.zip` routes to the batch importer instead.
        assertEquals(BookFormat.FB2, BookParser.detectFormatStatic("novel.fb2.zip"))
        assertEquals(BookFormat.UNKNOWN, BookParser.detectFormatStatic("my-books.zip"))
        assertEquals(BookFormat.UNKNOWN, BookParser.detectFormatStatic("comic.cbz"))
    }
}
