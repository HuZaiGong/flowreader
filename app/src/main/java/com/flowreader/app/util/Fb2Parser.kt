package com.flowreader.app.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.util.Base64

/**
 * Read-only FB2 reader.
 *
 * FB2 is plain XML, so this is a Jsoup XML parse plus a walk of `<body>` → `<section>`. Kept pure
 * (string in, model out) so the chapter splitting and cover writing stay in [BookParser] where the
 * rest of the file I/O lives, and so this is JVM-testable.
 */
object Fb2Parser {

    data class Fb2Section(val title: String, val content: String)

    data class Fb2Book(
        val title: String,
        val author: String,
        val description: String,
        val sections: List<Fb2Section>,
        val coverImage: ByteArray?
    ) {
        // Data classes with a ByteArray need these by hand; the generated versions compare
        // identity, which silently breaks any equality check on a parsed book.
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Fb2Book) return false
            return title == other.title &&
                author == other.author &&
                description == other.description &&
                sections == other.sections &&
                coverImage.contentEquals(other.coverImage)
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + author.hashCode()
            result = 31 * result + description.hashCode()
            result = 31 * result + sections.hashCode()
            result = 31 * result + (coverImage?.contentHashCode() ?: 0)
            return result
        }
    }

    fun parse(xml: String): Result<Fb2Book> {
        return try {
            val document = Jsoup.parse(xml, "", Parser.xmlParser())
            val titleInfo = document.selectFirst("title-info")

            val title = titleInfo?.selectFirst("book-title")?.text()?.trim().orEmpty()
            val author = titleInfo?.selectFirst("author")?.let { authorName(it) }.orEmpty()
            val description = titleInfo?.selectFirst("annotation")?.text()?.trim().orEmpty()

            // `<body name="notes">` holds footnotes, not the book, so it never becomes a chapter.
            val bodies = document.select("body").filterNot { it.attr("name").equals("notes", ignoreCase = true) }
            val sections = bodies.flatMap { body -> readSections(body) }.filter { it.content.isNotBlank() }

            if (sections.isEmpty()) {
                return Result.failure(IllegalArgumentException("FB2 文件中没有可读正文"))
            }

            Result.success(
                Fb2Book(
                    title = title,
                    author = author,
                    description = description,
                    sections = sections,
                    coverImage = readCover(document, titleInfo)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun authorName(element: Element): String {
        val parts = listOfNotNull(
            element.selectFirst("first-name")?.text(),
            element.selectFirst("middle-name")?.text(),
            element.selectFirst("last-name")?.text()
        ).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isNotEmpty()) return parts.joinToString(" ")
        return element.selectFirst("nickname")?.text()?.trim().orEmpty()
    }

    /**
     * Only top-level `<section>`s become chapters. Nested sections are folded into their parent —
     * a deeply nested FB2 would otherwise explode into hundreds of two-line "chapters".
     */
    private fun readSections(body: Element): List<Fb2Section> {
        val topLevel = body.children().filter { it.tagName().equals("section", ignoreCase = true) }
        if (topLevel.isEmpty()) {
            val text = paragraphText(body)
            return if (text.isBlank()) emptyList() else listOf(Fb2Section(body.attr("name").ifBlank { "正文" }, text))
        }
        return topLevel.mapIndexed { index, section ->
            val heading = section.selectFirst("title")?.text()?.trim().orEmpty()
            Fb2Section(
                title = heading.ifBlank { "第 ${index + 1} 节" },
                content = paragraphText(section)
            )
        }
    }

    private fun paragraphText(element: Element): String {
        val builder = StringBuilder()
        element.select("p, subtitle, v, text-author").forEach { node ->
            val line = node.text().trim()
            if (line.isNotEmpty()) {
                builder.appendLine(line)
                builder.appendLine()
            }
        }
        if (builder.isBlank()) return element.text().trim()
        return builder.toString().trim()
    }

    private fun readCover(document: org.jsoup.nodes.Document, titleInfo: Element?): ByteArray? {
        val href = titleInfo?.selectFirst("coverpage image")?.let { image ->
            image.attr("l:href").ifBlank { image.attr("xlink:href") }.ifBlank { image.attr("href") }
        } ?: return null
        val id = href.removePrefix("#")
        if (id.isBlank()) return null
        val binary = document.select("binary").firstOrNull { it.attr("id") == id } ?: return null
        return runCatching { Base64.getMimeDecoder().decode(binary.text().trim()) }.getOrNull()
    }
}
