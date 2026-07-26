package com.flowreader.app.core.util

/**
 * Real reading progress.
 *
 * The pre-v52 reader bar reported `chapterIndex / chapterCount`, so a five-chapter book jumped to
 * 20% the moment you opened chapter one and never moved again while you read it. Progress now
 * folds in how far through the current chapter you actually are.
 */
object ReadingProgress {

    /**
     * @param chapterIndex zero-based index of the open chapter.
     * @param chapterFraction how far through that chapter the reader is, 0..1.
     * @param totalChapters chapter count of the book.
     */
    fun fraction(chapterIndex: Int, chapterFraction: Float, totalChapters: Int): Float {
        if (totalChapters <= 0) return 0f
        val index = chapterIndex.coerceIn(0, totalChapters - 1)
        val inside = if (chapterFraction.isNaN()) 0f else chapterFraction.coerceIn(0f, 1f)
        return ((index + inside) / totalChapters).coerceIn(0f, 1f)
    }

    /** Maps a slider fraction back onto the chapter index it points at. */
    fun chapterAt(fraction: Float, totalChapters: Int): Int {
        if (totalChapters <= 0) return 0
        val clamped = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
        return (clamped * totalChapters).toInt().coerceIn(0, totalChapters - 1)
    }

    /** Scroll offset to chapter fraction, guarding the not-yet-measured case. */
    fun scrollFraction(scrollValue: Int, scrollMax: Int): Float {
        if (scrollMax <= 0) return 0f
        return (scrollValue.toFloat() / scrollMax.toFloat()).coerceIn(0f, 1f)
    }
}
