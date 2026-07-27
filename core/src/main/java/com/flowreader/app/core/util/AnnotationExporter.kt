package com.flowreader.app.core.util

import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.repository.AnnotationExportFormat

/**
 * The single annotation export formatter.
 *
 * v50 put this inside `AnnotationRepositoryImpl`, which meant the cross-book notes screen added in
 * v53 would have needed a second copy that drifts. It is a pure function of the annotations plus a
 * title lookup, so it lives here and both callers share it.
 */
object AnnotationExporter {

    /**
     * [titleOf] resolves a book id to its title. Return `null` (the default) to omit book headings,
     * which is what a single-book export wants.
     */
    fun export(annotations: List<Annotation>, format: AnnotationExportFormat, titleOf: (Long) -> String? = { null }): String =
        when (format) {
            AnnotationExportFormat.MARKDOWN -> annotations.joinToString("\n\n") { annotation ->
                val book = titleOf(annotation.bookId)?.let { "《$it》" } ?: ""
                "- ${book}第 ${annotation.chapterIndex + 1} 章：${annotation.selectedText}" +
                    if (annotation.note.isNotBlank()) "\n  - 笔记：${annotation.note}" else ""
            }

            AnnotationExportFormat.HTML -> buildString {
                append("<html><body><h1>FlowReader 标注</h1>")
                annotations.forEach { annotation ->
                    val book = titleOf(annotation.bookId)?.let { "《${it.escapeHtml()}》" } ?: ""
                    append("<section><h2>${book}第 ${annotation.chapterIndex + 1} 章</h2>")
                    append("<blockquote>${annotation.selectedText.escapeHtml()}</blockquote>")
                    if (annotation.note.isNotBlank()) append("<p>${annotation.note.escapeHtml()}</p>")
                    append("</section>")
                }
                append("</body></html>")
            }

            AnnotationExportFormat.TEXT -> annotations.joinToString("\n\n") { annotation ->
                val book = titleOf(annotation.bookId)?.let { "《$it》" } ?: ""
                "${book}第 ${annotation.chapterIndex + 1} 章\n${annotation.selectedText}" +
                    if (annotation.note.isNotBlank()) "\n笔记：${annotation.note}" else ""
            }
        }

    private fun String.escapeHtml(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
