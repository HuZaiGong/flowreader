package com.flowreader.app.core.util

import kotlin.math.pow

/**
 * WCAG 2.1 relative-luminance and contrast math over plain ARGB longs.
 *
 * Kept free of Compose so the reader palettes and brand scheme can be asserted against WCAG AA
 * in a JVM unit test instead of being eyeballed.
 */
object ColorContrast {

    const val AA_BODY_TEXT = 4.5
    const val AA_LARGE_TEXT = 3.0

    private fun channel(value: Int): Double {
        val c = value / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    fun relativeLuminance(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF).toInt()
        val g = ((argb shr 8) and 0xFF).toInt()
        val b = (argb and 0xFF).toInt()
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    fun ratio(foregroundArgb: Long, backgroundArgb: Long): Double {
        val a = relativeLuminance(foregroundArgb)
        val b = relativeLuminance(backgroundArgb)
        val lighter = maxOf(a, b)
        val darker = minOf(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun meetsAaBodyText(foregroundArgb: Long, backgroundArgb: Long): Boolean = ratio(foregroundArgb, backgroundArgb) >= AA_BODY_TEXT

    fun meetsAaLargeText(foregroundArgb: Long, backgroundArgb: Long): Boolean = ratio(foregroundArgb, backgroundArgb) >= AA_LARGE_TEXT
}
