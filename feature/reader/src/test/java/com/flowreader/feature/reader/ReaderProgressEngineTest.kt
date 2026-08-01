package com.flowreader.feature.reader

import com.flowreader.app.domain.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderProgressEngineTest {

    private val engine = ReaderProgressEngine()

    private fun chapter(content: String) = Chapter(
        id = 0,
        bookId = 1,
        index = 0,
        title = "章",
        content = content,
        startPosition = 0,
        endPosition = content.length
    )

    @Test
    fun charsPerPageScalesWithFontSizeAndLineSpacing() {
        val base = engine.estimateCharsPerPage(18, 1.5f)
        val largerFont = engine.estimateCharsPerPage(24, 1.5f)
        val tighterSpacing = engine.estimateCharsPerPage(18, 1.2f)

        assertEquals(900, base)
        assertEquals(675, largerFont)
        assertEquals(1125, tighterSpacing)
    }

    @Test
    fun charsPerPageIsClamped() {
        assertEquals(1600, engine.estimateCharsPerPage(10, 1f))
        assertEquals(350, engine.estimateCharsPerPage(48, 3f))
    }

    @Test
    fun fractionFoldsChapterPositionIntoOverallProgress() {
        assertEquals(0f, engine.fraction(0, 0f, 4))
        assertEquals(0.25f, engine.fraction(1, 0f, 4))
        assertEquals(0.375f, engine.fraction(1, 0.5f, 4))
        assertEquals(1f, engine.fraction(3, 1f, 4))
        assertEquals(0f, engine.fraction(0, Float.NaN, 4))
        assertEquals(0f, engine.fraction(0, 0f, 0))
    }

    @Test
    fun chapterAtMapsSliderFractionBack() {
        assertEquals(0, engine.chapterAt(0f, 4))
        assertEquals(1, engine.chapterAt(0.3f, 4))
        assertEquals(3, engine.chapterAt(0.99f, 4))
        assertEquals(0, engine.chapterAt(0f, 0))
    }

    @Test
    fun remainingMinutesSumsUnreadCharsAcrossChapters() {
        val chapters = listOf(
            chapter("01234567890123456789"),
            chapter("01234567890123456789"),
            chapter("01234567890123456789")
        )
        // chapter 0 finished: 20 + 20 = 40 unread chars at 100/min -> 0.4 -> 0
        assertEquals(0, engine.remainingMinutes(chapters, 0, 20, 100f))
        // halfway through chapter 0: 10 + 20 + 20 = 50 chars -> 0.5 -> rounds up to 1
        assertEquals(1, engine.remainingMinutes(chapters, 0, 10, 100f))
    }

    @Test
    fun remainingMinutesGuardsEmptySpeed() {
        val chapters = listOf(chapter("0123456789"))
        assertEquals(0, engine.remainingMinutes(chapters, 0, 0, 0f))
    }

    @Test
    fun breakSuggestionsEscalateWithSessionLength() {
        assertEquals(0L, engine.suggestedBreakMinutes(20))
        assertEquals(10L, engine.suggestedBreakMinutes(30))
        assertEquals(10L, engine.suggestedBreakMinutes(44))
        assertEquals(15L, engine.suggestedBreakMinutes(45))
    }

    @Test
    fun percentClampsToHundred() {
        assertEquals(100, engine.percent(1.5f))
        assertEquals(0, engine.percent(-0.5f))
        assertEquals(37, engine.percent(0.37f))
    }
}
