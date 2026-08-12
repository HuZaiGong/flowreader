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

    /**
     * `rawRange` hands back an inclusive last offset while `endPosition` is consumed as exclusive,
     * so `selectionSpan` must widen it by one. Round-tripping through `buildParagraphContent` pins
     * both halves together: a selection of N characters must highlight exactly N characters.
     */
    @Test
    fun selectionSpanCoversTheWholeSelectionWhenRoundTripped() {
        val paragraph = "abcdefghij"
        val content = buildParagraphContent(paragraph, paragraphStart = 0, annotations = emptyList(), indent = false)

        val span = content.selectionSpan(2, 5, paragraph, paragraphStart = 0)!!
        assertEquals(2, span.startPosition)
        assertEquals(5, span.endPosition)
        assertEquals("cde", span.text)

        val rendered = buildParagraphContent(
            paragraph = paragraph,
            paragraphStart = 0,
            annotations = listOf(annotation(span.startPosition, span.endPosition)),
            indent = false
        )
        val spanStyle = rendered.annotatedString.spanStyles.single()
        assertEquals(2, spanStyle.start)
        assertEquals(5, spanStyle.end)
        assertEquals(paragraph, rendered.annotatedString.text)
    }

    @Test
    fun selectionSpanIsExclusiveOfItsEndPosition() {
        val paragraph = "abcdefghij"
        val content = buildParagraphContent(paragraph, paragraphStart = 0, annotations = emptyList(), indent = false)

        val single = content.selectionSpan(0, 1, paragraph, paragraphStart = 0)!!
        assertEquals(0, single.startPosition)
        assertEquals(1, single.endPosition)
        assertEquals("a", single.text)

        val whole = content.selectionSpan(0, paragraph.length, paragraph, paragraphStart = 0)!!
        assertEquals(paragraph.length, whole.endPosition)
        assertEquals(paragraph, whole.text)
    }

    @Test
    fun selectionSpanKeepsAbsoluteOffsetsForALaterParagraph() {
        val chapterText = "前言。这是被高亮选中的文字。后文。"
        val paragraphStart = 3
        val paragraph = chapterText.substring(paragraphStart)
        val content = buildParagraphContent(paragraph, paragraphStart, emptyList(), indent = false)

        val span = content.selectionSpan(3, 3 + 8, paragraph, paragraphStart)!!
        assertEquals(paragraphStart + 3, span.startPosition)
        assertEquals(paragraphStart + 3 + 8, span.endPosition)
        assertEquals(chapterText.substring(span.startPosition, span.endPosition), span.text)
        assertEquals(8, span.text.length)
    }

    @Test
    fun selectionSpanSkipsMarkdownMarkersButStillCoversTheLastCharacter() {
        val paragraph = "这是**重要**内容"
        val content = buildParagraphContent(paragraph, paragraphStart = 0, annotations = emptyList(), indent = false)

        // Display "这是重要内容"; selecting display 2..4 is the bold word "重要" at raw 4..5.
        val span = content.selectionSpan(2, 4, paragraph, paragraphStart = 0)!!
        assertEquals(4, span.startPosition)
        assertEquals(6, span.endPosition)
        assertEquals("重要", span.text)
    }

    @Test
    fun selectionSpanRejectsEmptyAndInventedSelections() {
        val paragraph = "段落"
        val indented = buildParagraphContent(paragraph, paragraphStart = 0, annotations = emptyList(), indent = true)
        assertNull(indented.selectionSpan(0, 2, paragraph, paragraphStart = 0))

        val plain = buildParagraphContent(paragraph, paragraphStart = 0, annotations = emptyList(), indent = false)
        assertNull(plain.selectionSpan(1, 1, paragraph, paragraphStart = 0))
        assertNull(plain.selectionSpan(-1, 2, paragraph, paragraphStart = 0))
        assertNull(plain.selectionSpan(0, 99, paragraph, paragraphStart = 0))
    }

    @Test
    fun overlappingHighlightsDoNotDuplicateText() {
        val paragraph = "abcdefghij"
        val content = buildParagraphContent(
            paragraph = paragraph,
            paragraphStart = 0,
            annotations = listOf(annotation(start = 0, end = 6), annotation(start = 3, end = 9)),
            indent = false
        )

        assertEquals(paragraph, content.annotatedString.text)
        assertEquals(paragraph.length, content.rawOffsets.size)
        for (i in paragraph.indices) {
            assertEquals(i, content.rawOffsets[i])
        }
    }

    @Test
    fun nestedHighlightIsSkippedInsteadOfRewindingTheCursor() {
        val paragraph = "abcdefghij"
        val content = buildParagraphContent(
            paragraph = paragraph,
            paragraphStart = 0,
            annotations = listOf(annotation(start = 0, end = 8), annotation(start = 2, end = 5)),
            indent = false
        )

        assertEquals(paragraph, content.annotatedString.text)
        assertEquals(paragraph.length, content.rawOffsets.size)
        assertEquals(1, content.annotatedString.spanStyles.size)
        assertEquals(0, content.annotatedString.spanStyles.single().start)
        assertEquals(8, content.annotatedString.spanStyles.single().end)
    }

    @Test
    fun identicalStartsKeepTheParagraphIntactInEitherOrder() {
        val paragraph = "abcdefghij"
        val forward = buildParagraphContent(
            paragraph = paragraph,
            paragraphStart = 0,
            annotations = listOf(annotation(start = 0, end = 4), annotation(start = 0, end = 7)),
            indent = false
        )
        val reversed = buildParagraphContent(
            paragraph = paragraph,
            paragraphStart = 0,
            annotations = listOf(annotation(start = 0, end = 7), annotation(start = 0, end = 4)),
            indent = false
        )

        assertEquals(paragraph, forward.annotatedString.text)
        assertEquals(paragraph, reversed.annotatedString.text)
        assertEquals(paragraph.length, forward.rawOffsets.size)
        assertEquals(paragraph.length, reversed.rawOffsets.size)
    }

    @Test
    fun nonOverlappingHighlightsBothRender() {
        val paragraph = "abcdefghij"
        val content = buildParagraphContent(
            paragraph = paragraph,
            paragraphStart = 0,
            annotations = listOf(annotation(start = 1, end = 3), annotation(start = 6, end = 9)),
            indent = false
        )

        assertEquals(paragraph, content.annotatedString.text)
        assertEquals(2, content.annotatedString.spanStyles.size)
        assertEquals(1, content.annotatedString.spanStyles[0].start)
        assertEquals(3, content.annotatedString.spanStyles[0].end)
        assertEquals(6, content.annotatedString.spanStyles[1].start)
        assertEquals(9, content.annotatedString.spanStyles[1].end)
    }

    @Test
    fun paragraphStartOffsetKeepsAbsolutePositions() {
        val paragraph = "结尾"
        val content = buildParagraphContent(paragraph, paragraphStart = 42, annotations = emptyList(), indent = false)

        assertEquals(42, content.rawOffsets[0])
        assertEquals(43, content.rawOffsets[1])
    }
}
