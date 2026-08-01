package com.flowreader.feature.reader

import com.flowreader.app.core.util.ReadingProgress
import com.flowreader.app.domain.model.Chapter
import kotlin.math.roundToInt

/**
 * Pure reading-math for the reader: page estimation, progress fractions, reading-time
 * prediction and break suggestions. No Android types, unit-testable on the JVM.
 *
 * Extracted from `ReaderViewModel` in v54 so the ViewModel only orchestrates.
 */
class ReaderProgressEngine {

    /**
     * Estimated non-whitespace characters that fit on one screen. Scales inversely with the
     * font size and line spacing so bigger type means fewer characters per page.
     */
    fun estimateCharsPerPage(fontSize: Int, lineSpacing: Float): Int {
        val fontFactor = 18f / fontSize.coerceAtLeast(12)
        val lineFactor = 1.5f / lineSpacing.coerceAtLeast(1f)
        return (900 * fontFactor * lineFactor).roundToInt().coerceIn(350, 1600)
    }

    fun fraction(chapterIndex: Int, chapterFraction: Float, totalChapters: Int): Float =
        ReadingProgress.fraction(chapterIndex, chapterFraction, totalChapters)

    fun chapterAt(fraction: Float, totalChapters: Int): Int = ReadingProgress.chapterAt(fraction, totalChapters)

    fun scrollFraction(scrollValue: Int, scrollMax: Int): Float = ReadingProgress.scrollFraction(scrollValue, scrollMax)

    fun percent(progress: Float): Int = (progress * 100).roundToInt().coerceIn(0, 100)

    /**
     * Minutes left to finish the book at the current reading speed, counting the unread part of
     * the current chapter plus every following chapter.
     */
    fun remainingMinutes(chapters: List<Chapter>, currentIndex: Int, currentPosition: Int, speed: Float): Int {
        var remainingChars = 0
        for (i in currentIndex until chapters.size) {
            val chapter = chapters.getOrNull(i) ?: continue
            remainingChars += if (i == currentIndex) {
                (chapter.content.length - currentPosition).coerceAtLeast(0)
            } else {
                chapter.content.length
            }
        }
        return (remainingChars / speed.coerceAtLeast(100f)).roundToInt()
    }

    /** Break suggestion after a long session: 15 minutes after 45, 10 after 30, none before. */
    fun suggestedBreakMinutes(sessionMinutes: Long): Long = when {
        sessionMinutes >= 45 -> 15
        sessionMinutes >= 30 -> 10
        else -> 0
    }
}
