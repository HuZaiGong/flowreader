package com.flowreader.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingProgressTest {

    @Test
    fun progressMovesWhileReadingInsideAChapter() {
        // The v51 bar reported chapterIndex / chapterCount and stayed frozen at 0% for a whole
        // chapter of a five-chapter book.
        val start = ReadingProgress.fraction(chapterIndex = 0, chapterFraction = 0f, totalChapters = 5)
        val middle = ReadingProgress.fraction(chapterIndex = 0, chapterFraction = 0.5f, totalChapters = 5)
        assertEquals(0f, start, 0.0001f)
        assertEquals(0.1f, middle, 0.0001f)
        assertTrue(middle > start)
    }

    @Test
    fun finishingTheLastChapterReachesOneHundredPercent() {
        assertEquals(1f, ReadingProgress.fraction(4, 1f, 5), 0.0001f)
    }

    @Test
    fun progressClampsOutOfRangeInput() {
        assertEquals(0f, ReadingProgress.fraction(-3, -1f, 5), 0.0001f)
        assertEquals(1f, ReadingProgress.fraction(99, 5f, 5), 0.0001f)
        assertEquals(0f, ReadingProgress.fraction(0, Float.NaN, 5), 0.0001f)
        assertEquals(0f, ReadingProgress.fraction(0, 0.5f, 0), 0.0001f)
    }

    @Test
    fun chapterAtIsTheInverseOfChapterStarts() {
        assertEquals(0, ReadingProgress.chapterAt(0f, 5))
        assertEquals(2, ReadingProgress.chapterAt(0.5f, 5))
        assertEquals(4, ReadingProgress.chapterAt(1f, 5))
        assertEquals(0, ReadingProgress.chapterAt(0.5f, 0))
    }

    @Test
    fun scrollFractionGuardsTheUnmeasuredCase() {
        assertEquals(0f, ReadingProgress.scrollFraction(0, 0), 0.0001f)
        assertEquals(0.5f, ReadingProgress.scrollFraction(500, 1000), 0.0001f)
        assertEquals(1f, ReadingProgress.scrollFraction(2000, 1000), 0.0001f)
    }
}
