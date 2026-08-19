package com.flowreader.app.core.util

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * HSV / HSL conversions over plain ARGB longs.
 *
 * Deliberately Compose-free (like [ColorContrast]) so the color-scheme generator and the picker
 * geometry are both JVM-testable. `android.graphics.Color.colorToHSV` would drag in the Android
 * framework and `androidx.compose.ui.graphics.Color` would drag in Compose; neither is available in
 * a plain unit test, and this math is small enough to own.
 *
 * Conventions: `hue` is degrees in `[0, 360)`, `saturation` / `value` / `lightness` are `[0, 1]`.
 * Alpha is always forced opaque — every consumer here is a theme color, never a translucent one.
 */
object ColorSpaces {

    const val OPAQUE_ALPHA = 0xFF000000L

    data class Hsv(val hue: Float, val saturation: Float, val value: Float)

    fun red(argb: Long): Int = ((argb shr 16) and 0xFF).toInt()

    fun green(argb: Long): Int = ((argb shr 8) and 0xFF).toInt()

    fun blue(argb: Long): Int = (argb and 0xFF).toInt()

    /** Packs opaque ARGB from `[0, 255]` channels, clamping rather than wrapping on overflow. */
    fun argb(red: Int, green: Int, blue: Int): Long {
        val r = red.coerceIn(0, 255).toLong()
        val g = green.coerceIn(0, 255).toLong()
        val b = blue.coerceIn(0, 255).toLong()
        return OPAQUE_ALPHA or (r shl 16) or (g shl 8) or b
    }

    fun toHsv(argb: Long): Hsv {
        val r = red(argb) / 255f
        val g = green(argb) / 255f
        val b = blue(argb) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        // Grey has no meaningful hue; report 0 rather than NaN so callers can round-trip it.
        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        val saturation = if (max == 0f) 0f else delta / max
        return Hsv(normalizeHue(hue), saturation.coerceIn(0f, 1f), max.coerceIn(0f, 1f))
    }

    fun fromHsv(hue: Float, saturation: Float, value: Float): Long {
        val h = normalizeHue(hue)
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        val c = v * s
        val x = c * (1f - abs(((h / 60f) % 2f) - 1f))
        val m = v - c
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return argb(
            ((r + m) * 255f).roundToInt(),
            ((g + m) * 255f).roundToInt(),
            ((b + m) * 255f).roundToInt()
        )
    }

    fun fromHsv(hsv: Hsv): Long = fromHsv(hsv.hue, hsv.saturation, hsv.value)

    /**
     * HSL is the right space for tonal generation: lightness is symmetric around 0.5, so a tone
     * ladder built on it keeps hue and saturation while sweeping light-to-dark. HSV's `value` does
     * not — dropping `value` toward 0 makes everything black without ever passing through a tint.
     */
    fun fromHsl(hue: Float, saturation: Float, lightness: Float): Long {
        val h = normalizeHue(hue)
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs(((h / 60f) % 2f) - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return argb(
            ((r + m) * 255f).roundToInt(),
            ((g + m) * 255f).roundToInt(),
            ((b + m) * 255f).roundToInt()
        )
    }

    /** Hue, saturation and lightness of [argb], with lightness in `[0, 1]`. */
    fun hslOf(argb: Long): Triple<Float, Float, Float> {
        val r = red(argb) / 255f
        val g = green(argb) / 255f
        val b = blue(argb) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val lightness = (max + min) / 2f
        val hue = toHsv(argb).hue
        val saturation = when {
            delta == 0f -> 0f
            else -> delta / (1f - abs(2f * lightness - 1f)).coerceAtLeast(1e-6f)
        }
        return Triple(hue, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))
    }

    fun normalizeHue(hue: Float): Float {
        if (hue.isNaN()) return 0f
        val wrapped = hue % 360f
        return if (wrapped < 0f) wrapped + 360f else wrapped
    }

    /** `#RRGGBB`, upper case, alpha dropped. The form the picker's text field round-trips. */
    fun toHexString(argb: Long): String = "#%06X".format(argb and 0xFFFFFFL)

    /**
     * Parses `#RGB`, `#RRGGBB` or `#AARRGGBB` (the leading `#` optional) into an opaque ARGB long,
     * or null when the text is not a color. Any alpha supplied is discarded.
     */
    fun parseHex(raw: String): Long? {
        val cleaned = raw.trim().removePrefix("#")
        if (cleaned.any { it.digitToIntOrNull(16) == null }) return null
        val rgb = when (cleaned.length) {
            3 -> cleaned.map { "$it$it" }.joinToString("")
            6 -> cleaned
            8 -> cleaned.substring(2)
            else -> return null
        }
        return rgb.toLongOrNull(16)?.let { OPAQUE_ALPHA or it }
    }
}
