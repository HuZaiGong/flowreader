package com.flowreader.app.ui.screens.reader.components

import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.AnnotationColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderTextMappingTest {

    private fun annotation(start: Int, end: Int) = Annotation(
        bookId = 1L,
        chapterIndex = 0,
        startPosition = start,
        endPosition = end,
        selectedText = "",
        color = AnnotationColor.YELLOW
    )

    @Test
    fun plainParagraphMapsOneToOne() {
        val paragraph = "这是一个普通段落。"
        val content = buildParagraphContent(paragraph, paragraphStart = 0, annotations = emptyList(), indent = false)

        assertEquals(paragraph, content.annotatedString.text)
        assertEquals(paragraph.length, content.rawOffsets.size)
        for (i in paragraph.indices) {
            assertEquals(i, content.rawOffsets[i])
        }
    }

    @Test
    fun indentPrefixMapsToMinusOne() {
        val paragraph = "正文内容"
        val content = buildParagraphContent(paragraph, paragraphStart = 10, annotations = emptyList(), indent = true)

        assertEquals("　　正文内容", content.annotatedString.text)
        assertEquals(paragraph.length + 2, content.rawOffsets.size)
        assertEquals(-1, content.rawOffsets[0])
        assertEquals(-1, content.rawOffsets[1])
        assertEquals(10, content.rawOffsets[2])
        assertEquals(13, content.rawOffsets[5])
    }

    @Test
    fun markdownBoldIsStrippedButMapped() {
        val paragraph = "这是**重要**内容"
        val content = buildParagraphContent(paragraph, paragraphStart = 0, annotations = emptyList(), indent = false)

        assertEquals("这是重要内容", content.annotatedString.text)
        // 这是 = 0,1 ; 重 = 4, 要 = 5 (raw offsets past the ** markers) ; 内容 = 8,9
        assertEquals(4, content.rawOffsets[2])
        assertEquals(5, content.rawOffsets[3])
        assertEquals(8, content.rawOffsets[4])
        assertEquals(9, content.rawOffsets[5])
    }

    @Test
    fun rawRangeConvertsDisplaySelectionBackToChapterOffsets() {
        val chapterText = "前言。这是被高亮选中的文字。后文。"
        val paragraphStart = 3
        val paragraph = chapterText.substring(paragraphStart)
        val content = buildParagraphContent(paragraph, paragraphStart, emptyList(), indent = false)

        val selectionStart = 3
        val selectionEnd = 3 + 8
        val range = content.rawRange(selectionStart, selectionEnd)

        assertEquals(paragraphStart + selectionStart, range!!.first)
        assertEquals(paragraphStart + selectionEnd - 1, range.last)
    }

    @Test
    fun rawRangeRejectsIndentCharacters() {
        val paragraph = "段落"
        val content = buildParagraphContent(paragraph, paragraphStart = 0, annotations = emptyList(), indent = true)

        assertNull(content.rawRange(0, 2))
    }

    @Test
    fun annotationHighlightIsAppliedAtCorrectDisplayRange() {
        val chapterText = "abcdefghij"
        val paragraphStart = 2
        val paragraph = chapterText.substring(paragraphStart)
        val content = buildParagraphContent(
            paragraph = paragraph,
            paragraphStart = paragraphStart,
            annotations = listOf(annotation(start = 5, end = 8)),
            indent = false
        )

        // Raw chapter offsets 5..7 (display 3..5) carry the highlight span.
        val spanStyle = content.annotatedString.spanStyles.single()
        assertEquals(3, spanStyle.start)
        assertEquals(6, spanStyle.end)
    }

    @Test
    fun paragraphStartOffsetKeepsAbsolutePositions() {
        val paragraph = "结尾"
        val content = buildParagraphContent(paragraph, paragraphStart = 42, annotations = emptyList(), indent = false)

        assertEquals(42, content.rawOffsets[0])
        assertEquals(43, content.rawOffsets[1])
    }
}
