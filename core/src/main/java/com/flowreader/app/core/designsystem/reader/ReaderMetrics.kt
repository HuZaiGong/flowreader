package com.flowreader.app.core.designsystem.reader

import com.flowreader.app.domain.model.ReadingSettings

/**
 * Pure typography math for the reader body. Everything here is deliberately free of Compose and
 * Android types so it can be unit-tested on the JVM — these are the functions that turn
 * [ReadingSettings] into real pixels, which is what the pre-v52 renderer never did.
 */
object ReaderMetrics {

    /** Ideal CJK measure. Beyond roughly this many glyphs a line becomes hard to track back. */
    const val IDEAL_LINE_CHARS = 34

    /** Minimum body width we will ever squeeze to, in dp, before we stop clamping. */
    const val MIN_CONTENT_WIDTH_DP = 280f

    fun lineHeightSp(fontSize: Int, lineSpacing: Float): Float = fontSize.coerceAtLeast(1) * lineSpacing.coerceIn(1.0f, 2.5f)

    /**
     * Paragraph gap in dp. Before v52 the raw multiplier was passed straight to `padding(x.dp)`,
     * so the default of `1.0f` produced a 1dp gap — visually no paragraph separation at all.
     */
    fun paragraphSpacingDp(fontSize: Int, paragraphSpacing: Float): Float {
        val normalized = ReadingSettings.normalizeParagraphSpacing(paragraphSpacing)
        return fontSize.coerceAtLeast(1) * normalized * 0.5f
    }

    /** First-line indent, expressed in sp: two full-width characters when enabled. */
    fun firstLineIndentSp(fontSize: Int, enabled: Boolean): Float = if (enabled) fontSize.coerceAtLeast(1) * 2f else 0f

    /**
     * Body column width. On a tablet an unconstrained column reaches 100+ characters per line;
     * we cap the measure at [idealChars] glyphs and let the remainder become side margin.
     */
    fun contentWidthDp(availableWidthDp: Float, fontSize: Int, idealChars: Int = IDEAL_LINE_CHARS): Float {
        if (availableWidthDp <= 0f) return 0f
        val ideal = fontSize.coerceAtLeast(1) * idealChars.coerceAtLeast(1).toFloat()
        val capped = minOf(availableWidthDp, ideal)
        return maxOf(capped, minOf(availableWidthDp, MIN_CONTENT_WIDTH_DP))
    }

    fun chapterTitleSizeSp(fontSize: Int): Float = fontSize.coerceAtLeast(1) + 6f

    fun headingSizeSp(fontSize: Int): Float = fontSize.coerceAtLeast(1) + 3f
}
