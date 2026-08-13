package com.flowreader.app.core.util

/**
 * Rules for using a user-supplied image as the reader background (v56.5.0).
 *
 * The reader's whole readability guarantee rests on the background being a single flat colour that
 * [ColorContrast] can measure — `ReaderPaletteContrastTest` asserts AA body contrast for every
 * palette, and [ReaderCustomTheme] silently falls back when a custom pair fails. An arbitrary photo
 * has no single background colour, so that guarantee would simply evaporate: body text could land
 * on a bright patch and become unreadable, with nothing in the test suite noticing.
 *
 * The fix is structural rather than a preference. The image is always covered by a scrim in the
 * active palette's background colour, at [MIN_SCRIM_ALPHA] opacity or more. Text is then composited
 * against a *known* colour — the scrim-over-image result at worst approaches the palette background
 * as alpha approaches 1, and at [MIN_SCRIM_ALPHA] still dominates the image beneath it. The scrim
 * cannot be switched off; that is the point of it.
 *
 * Deliberately Compose-free so the alpha clamping and the worst-case contrast reasoning stay
 * JVM-testable, matching the rest of `core/util/`.
 */
object ReaderBackgroundImage {

    /**
     * Lower bound on scrim opacity, offered as the most transparent setting in the UI.
     *
     * Not every palette is readable this low — the per-palette floor ranges from 0.5 (EINK, pure
     * black on near-white) to 0.8 (SOLARIZED_DARK and NORD, whose backgrounds are the lightest of
     * the dark set, so a white image bleeding through raises the effective luminance fastest). Use
     * [minimumReadableAlpha] for the active palette rather than assuming this value is safe.
     */
    const val MIN_SCRIM_ALPHA = 0.6f

    /** Upper bound. At 1f the image is fully hidden, which is a legitimate "temporarily off" state. */
    const val MAX_SCRIM_ALPHA = 1.0f

    /**
     * Default for a freshly imported image. Measured, not guessed: the worst palette of the 18
     * needs 0.8, so 0.85 clears every one of them with room to spare. Lowering this without
     * re-running `ReaderBackgroundImageTest` will make some palettes fail AA.
     */
    const val DEFAULT_SCRIM_ALPHA = 0.85f

    /**
     * Largest image accepted on import, matching `BookParser.maxSingleImageBytes` so every image
     * entering the app obeys one limit.
     */
    const val MAX_IMAGE_BYTES = 24L * 1024 * 1024

    /** Longest edge kept after downsampling. A background never needs more than screen resolution. */
    const val MAX_EDGE_PX = 2048

    /** Subdirectory under `filesDir` holding imported backgrounds, mirroring `fonts/`. */
    const val BACKGROUND_DIR = "backgrounds"

    /** Clamps a stored or user-chosen alpha into the readable range. */
    fun clampScrimAlpha(raw: Float): Float = when {
        raw.isNaN() -> DEFAULT_SCRIM_ALPHA
        raw < MIN_SCRIM_ALPHA -> MIN_SCRIM_ALPHA
        raw > MAX_SCRIM_ALPHA -> MAX_SCRIM_ALPHA
        else -> raw
    }

    /**
     * Whether [path] should be treated as an active background. A blank path means "none", which is
     * distinct from a path that no longer resolves — that case is handled at render time, since a
     * file can disappear between sessions.
     */
    fun isActive(path: String?): Boolean = !path.isNullOrBlank()

    /**
     * Power-of-two subsample factor that brings an image of [srcWidth] x [srcHeight] under
     * [MAX_EDGE_PX], in the form `BitmapFactory.Options.inSampleSize` expects.
     *
     * Returns 1 for images already small enough, and for non-positive dimensions — a bogus bounds
     * read should not produce a zero or negative sample size, which `BitmapFactory` treats as 1
     * anyway but which would hide the fact that the decode failed.
     */
    fun sampleSizeFor(srcWidth: Int, srcHeight: Int): Int {
        if (srcWidth <= 0 || srcHeight <= 0) return 1
        var sample = 1
        while (maxOf(srcWidth, srcHeight) / sample > MAX_EDGE_PX) {
            sample *= 2
        }
        return sample
    }

    /**
     * The colour body text is effectively composited against: the palette background at the given
     * alpha over an unknown image. Alpha-composites [scrimArgb] over [worstCaseUnderlyingArgb],
     * which callers set to the value that hurts contrast most — white for a light palette, black
     * for a dark one — so the result is a *lower bound* on readability rather than an average.
     *
     * Both inputs and the result are opaque ARGB; the alpha channel of [scrimArgb] is ignored in
     * favour of [scrimAlpha].
     */
    fun effectiveBackgroundArgb(scrimArgb: Long, worstCaseUnderlyingArgb: Long, scrimAlpha: Float): Long {
        val a = clampScrimAlpha(scrimAlpha).toDouble()
        fun mix(shift: Int): Long {
            val top = (scrimArgb shr shift) and 0xFF
            val bottom = (worstCaseUnderlyingArgb shr shift) and 0xFF
            val value = top * a + bottom * (1.0 - a)
            return value.toLong().coerceIn(0L, 255L)
        }
        return (0xFFL shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    /**
     * Whether body text in [textArgb] still clears AA over a [scrimArgb] scrim at [scrimAlpha],
     * assuming the worst possible image underneath. Used to decide whether a chosen alpha is safe
     * for the active palette rather than trusting [MIN_SCRIM_ALPHA] to cover every combination.
     */
    fun bodyTextStaysReadable(textArgb: Long, scrimArgb: Long, scrimAlpha: Float, paletteIsDark: Boolean): Boolean {
        val worstCase = if (paletteIsDark) 0xFFFFFFFFL else 0xFF000000L
        val effective = effectiveBackgroundArgb(scrimArgb, worstCase, scrimAlpha)
        return ColorContrast.meetsAaBodyText(textArgb, effective)
    }

    /**
     * Smallest alpha at or above [MIN_SCRIM_ALPHA] that keeps body text readable for this palette,
     * or null if even a fully opaque scrim fails — which would mean the palette itself is broken,
     * since a fully opaque scrim *is* the palette background.
     *
     * Steps in 0.05 so the answer lands on values the settings UI can actually offer.
     */
    fun minimumReadableAlpha(textArgb: Long, scrimArgb: Long, paletteIsDark: Boolean): Float? {
        var alpha = MIN_SCRIM_ALPHA
        while (alpha <= MAX_SCRIM_ALPHA + 1e-6f) {
            if (bodyTextStaysReadable(textArgb, scrimArgb, alpha, paletteIsDark)) {
                return minOf(alpha, MAX_SCRIM_ALPHA)
            }
            alpha += 0.05f
        }
        return null
    }
}
