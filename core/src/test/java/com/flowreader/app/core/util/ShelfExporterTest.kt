package com.flowreader.app.core.util

import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.model.Category
import com.flowreader.app.domain.model.ReadingList
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfExporterTest {
    private val book = Book(
        id = 7,
        title = "测试, 书名",
        author = "作者 \"A\"",
        description = "",
        filePath = "/books/1.epub",
        fileSize = 100,
        format = BookFormat.EPUB,
        totalChapters = 3,
        readingProgress = 0.5f,
        lastReadTime = Date(1_700_000_000_000),
        tags = listOf("技术", "在读"),
        categoryId = 1
    )
    private val category = Category(id = 1, name = "小说", bookCount = 1)
    private val list = ReadingList(id = 2, name = "2026 必读", description = "", createdTime = Date(), updatedTime = Date())

    @Test
    fun csvQuotesFieldsWithCommasAndQuotes() {
        val csv = ShelfExporter.toCsv(listOf(book), listOf(category), emptyList(), listsByBook = mapOf(7L to "2026 必读"))
        assertTrue(csv.startsWith("标题,作者,格式,章节数,进度,分类,标签,阅读列表,文件路径\n"))
        assertTrue(csv.contains("\"测试, 书名\""))
        assertTrue(csv.contains("\"作者 \"\"A\"\"\""))
        assertTrue(csv.contains("小说"))
        assertTrue(csv.contains("技术; 在读"))
    }

    @Test
    fun csvOmitsEmptyFields() {
        val csv = ShelfExporter.toCsv(
            listOf(book.copy(tags = emptyList(), categoryId = 0, author = "")),
            emptyList(),
            emptyList()
        )
        val line = csv.lineSequence().last { it.isNotEmpty() }
        assertTrue(line.startsWith("\"测试, 书名\",,"))
        assertTrue(line.contains(",,"))
    }

    @Test
    fun jsonContainsAllBookFields() {
        val json = ShelfExporter.toJson(listOf(book), listOf(category), listOf(list), listsByBook = mapOf(7L to "2026 必读"))
        assertTrue(json.contains("\"title\": \"测试, 书名\""))
        assertTrue(json.contains("\"author\": \"作者 \\\"A\\\"\""))
        assertTrue(json.contains("\"format\": \"EPUB\""))
        assertTrue(json.contains("\"progress\": \"0.50\""))
        assertTrue(json.contains("\"category\": \"小说\""))
        assertTrue(json.contains("\"tags\": \"技术; 在读\""))
        assertTrue(json.contains("\"readingList\": \"2026 必读\""))
    }

    @Test
    fun jsonEscapesQuotesAndNewlines() {
        val weird = book.copy(title = "行一\n行二\"引号")
        val json = ShelfExporter.toJson(listOf(weird), emptyList(), emptyList())
        assertTrue(json.contains("\"title\": \"行一\\n行二\\\"引号\""))
    }

    @Test
    fun emptyShelfProducesValidShells() {
        val csv = ShelfExporter.toCsv(emptyList(), emptyList(), emptyList())
        assertEquals("标题,作者,格式,章节数,进度,分类,标签,阅读列表,文件路径\n", csv)
        val json = ShelfExporter.toJson(emptyList(), emptyList(), emptyList())
        assertTrue(json.contains("\"books\": [\n  ]"))
    }
}
