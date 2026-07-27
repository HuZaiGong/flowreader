package com.flowreader.app.core.util

import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.repository.AnnotationExportFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotationExporterTest {

    private val annotation = Annotation(
        id = 1,
        bookId = 7,
        chapterIndex = 2,
        startPosition = 0,
        endPosition = 5,
        selectedText = "心流是一种状态",
        note = "对照第 4 章"
    )

    @Test
    fun markdownCarriesTextAndNote() {
        val markdown = AnnotationExporter.export(listOf(annotation), AnnotationExportFormat.MARKDOWN)

        assertTrue(markdown.contains("心流是一种状态"))
        assertTrue(markdown.contains("对照第 4 章"))
        assertTrue("chapter index must be 1-based in output", markdown.contains("第 3 章"))
    }

    @Test
    fun bookTitleIsOmittedWhenTheLookupReturnsNull() {
        val markdown = AnnotationExporter.export(listOf(annotation), AnnotationExportFormat.MARKDOWN)
        assertFalse(markdown.contains("《"))
    }

    @Test
    fun bookTitleIsIncludedForCrossBookExports() {
        val markdown = AnnotationExporter.export(
            annotations = listOf(annotation),
            format = AnnotationExportFormat.MARKDOWN,
            titleOf = { "心流" }
        )
        assertTrue(markdown.contains("《心流》"))
    }

    @Test
    fun htmlEscapesUserText() {
        val hostile = annotation.copy(selectedText = "<script>alert(1)</script>", note = "a & b")
        val html = AnnotationExporter.export(listOf(hostile), AnnotationExportFormat.HTML)

        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("a &amp; b"))
    }

    @Test
    fun noteLineIsDroppedWhenThereIsNoNote() {
        val plain = annotation.copy(note = "")
        val text = AnnotationExporter.export(listOf(plain), AnnotationExportFormat.TEXT)
        assertFalse(text.contains("笔记"))
    }

    @Test
    fun emptyInputProducesEmptyOrSkeletonOutput() {
        assertEquals("", AnnotationExporter.export(emptyList(), AnnotationExportFormat.MARKDOWN))
        assertEquals("", AnnotationExporter.export(emptyList(), AnnotationExportFormat.TEXT))
        assertTrue(AnnotationExporter.export(emptyList(), AnnotationExportFormat.HTML).contains("</html>"))
    }
}
