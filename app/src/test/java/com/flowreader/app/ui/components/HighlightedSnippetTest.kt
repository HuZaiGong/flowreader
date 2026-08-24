package com.flowreader.app.ui.components

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The snippet highlighter takes offsets straight from the search index, so every out-of-range case
 * has to degrade to plain text rather than throw. A `StringIndexOutOfBoundsException` here would
 * crash the search screen on a result the user cannot even see yet.
 */
class HighlightedSnippetTest {

    private val emphasis = SpanStyle(fontWeight = FontWeight.Bold)

    private fun snippet(text: String, start: Int, length: Int) =
        highlightedSnippet(text, start, length, emphasis)

    @Test
    fun theMatchedRunIsTheOnlyStyledPart() {
        val result = snippet("春天的心流阅读", 3, 2)

        assertEquals("春天的心流阅读", result.text)
        val spans = result.spanStyles
        assertEquals(1, spans.size)
        assertEquals(3, spans[0].start)
        assertEquals(5, spans[0].end)
        assertEquals("心流", result.text.substring(spans[0].start, spans[0].end))
    }

    @Test
    fun aMatchAtTheVeryStartKeepsItsSpan() {
        val result = snippet("心流阅读", 0, 2)

        assertEquals(1, result.spanStyles.size)
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(2, result.spanStyles[0].end)
    }

    @Test
    fun aMatchRunningToTheEndKeepsItsSpan() {
        val result = snippet("阅读心流", 2, 2)

        assertEquals(1, result.spanStyles.size)
        assertEquals(4, result.spanStyles[0].end)
    }

    /** -1 is what the index reports for a hit that exists only in the normalized stream. */
    @Test
    fun aMissingOffsetRendersPlainText() {
        val result = snippet("事，江南的春天", -1, 0)

        assertEquals("事，江南的春天", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun aZeroLengthMatchRendersPlainText() {
        assertTrue(snippet("春天", 1, 0).spanStyles.isEmpty())
        assertTrue(snippet("春天", 1, -3).spanStyles.isEmpty())
    }

    @Test
    fun anOffsetPastTheSnippetRendersPlainText() {
        val result = snippet("春天", 5, 2)

        assertEquals("春天", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    /**
     * The snippet is a ~50-character window, so a match near its edge can be reported longer than
     * what survived the cut. Clamping keeps that from throwing.
     */
    @Test
    fun aSpanOverrunningTheSnippetIsClampedInsteadOfThrowing() {
        val result = snippet("春天的心流", 3, 20)

        assertEquals("春天的心流", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(3, result.spanStyles[0].start)
        assertEquals(5, result.spanStyles[0].end)
    }

    @Test
    fun anEmptySnippetRendersPlainText() {
        val result = snippet("", 0, 2)

        assertEquals("", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }
}
