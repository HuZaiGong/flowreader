package com.flowreader.app.core.util

import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.Category
import com.flowreader.app.domain.model.ReadingList

/**
 * Shelf metadata export (v55): books, categories and reading lists as CSV or JSON.
 * Pure string builders, JVM-testable; the SAF write happens in the UI layer.
 */
object ShelfExporter {

    fun toCsv(
        books: List<Book>,
        categories: List<Category>,
        lists: List<ReadingList>,
        listsByBook: Map<Long, String> = emptyMap()
    ): String {
        val categoryNames = categories.associate { it.id to it.name }
        val builder = StringBuilder()
        builder.append("标题,作者,格式,章节数,进度,分类,标签,阅读列表,文件路径\n")
        books.forEach { book ->
            val percent = (book.readingProgress * 100).toString().take(5)
            builder
                .append(csvField(book.title))
                .append(csvField(book.author))
                .append(csvField(book.format.name))
                .append(book.totalChapters)
                .append(',')
                .append(percent)
                .append(',')
                .append(csvField(categoryNames[book.categoryId].orEmpty()))
                .append(csvField(book.tags.joinToString("; ")))
                .append(csvField(listsByBook[book.id] ?: ""))
                .append(csvField(book.filePath))
                .append('\n')
        }
        return builder.toString()
    }

    fun toJson(
        books: List<Book>,
        categories: List<Category>,
        lists: List<ReadingList>,
        listsByBook: Map<Long, String> = emptyMap()
    ): String {
        val categoryNames = categories.associate { it.id to it.name }
        val builder = StringBuilder()
        builder.append("{\n  \"exportedAt\": \"")
            .append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.ROOT).format(java.util.Date()))
            .append("\",\n")
        builder.append("  \"categories\": [")
        builder.append(categories.joinToString(", ") { jsonObject("id" to it.id.toString(), "name" to it.name) })
        builder.append("],\n  \"books\": [")
        if (books.isEmpty()) {
            builder.append("\n  ]\n}")
            return builder.toString()
        }
        builder.append("\n")
        builder.append(books.joinToString(",\n") { book -> buildBookJson(book, categoryNames, listsByBook) })
        builder.append("\n  ]\n}")
        return builder.toString()
    }

    private fun buildBookJson(book: Book, categoryNames: Map<Long, String>, listsByBook: Map<Long, String>): String {
        val percent = "%.2f".format(book.readingProgress)
        val obj = buildMap {
            put("title", book.title)
            put("author", book.author)
            put("format", book.format.name)
            put("totalChapters", book.totalChapters.toString())
            put("progress", percent)
            put("category", categoryNames[book.categoryId].orEmpty())
            put("tags", book.tags.joinToString("; "))
            put("readingList", listsByBook[book.id] ?: "")
            put("filePath", book.filePath)
            put("lastReadTime", book.lastReadTime?.time?.toString() ?: "")
        }
        return "    " + jsonObject(*obj.entries.map { it.key to it.value }.toTypedArray())
    }

    private fun csvField(value: String): String {
        if (value.isEmpty()) return ","
        if (value.any { it == ',' || it == '"' || it == '\n' }) {
            return "\"${value.replace("\"", "\"\"")}\","
        }
        return "$value,"
    }

    private fun jsonObject(vararg fields: Pair<String, String>): String {
        val body = fields.joinToString(", ") { (key, value) ->
            "\"$key\": \"${escapeJson(value)}\""
        }
        return "{ $body }"
    }

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
}
