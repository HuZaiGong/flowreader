package com.flowreader.app.core.util

/**
 * A full Material color role set as plain opaque ARGB longs.
 *
 * Mirrors exactly the 24 roles `FlowLightColorScheme` / `FlowDarkColorScheme` set, so the generated
 * scheme is a drop-in replacement for the hand-tuned brand one. Longs rather than Compose `Color`s
 * for the same reason as `ReaderPalette`: the whole table stays a pure value that JVM tests —
 * including the WCAG assertions in `SeedColorSchemeTest` — can read without Compose.
 */
data class SeedScheme(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val tertiaryContainer: Long,
    val onTertiaryContainer: Long,
    val error: Long,
    val onError: Long,
    val errorContainer: Long,
    val onErrorContainer: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
    val outlineVariant: Long
)

/**
 * Generates a Material-style scheme from a single seed color (v56.6).
 *
 * This is a deliberate approximation of M3's HCT tonal palettes, not a port of them.
 * `material-color-utilities` is not a dependency and `dynamicLightColorScheme` only reads the
 * wallpaper, so the choice was between adding a library to an offline-first app or owning ~150 lines
 * of HSL math. Owning it keeps the generator JVM-testable, which is what makes the AA guarantee
 * below verifiable instead of aspirational.
 *
 * The guarantee: **every text-on-surface pair in the returned scheme clears WCAG AA body text
 * (4.5:1)**, for any seed, in both light and dark. Tone ladders alone cannot promise that — a
 * mid-luminance seed pushes `onPrimary` toward the same luminance as `primary` — so each `onX` role
 * is swept away from its background until it passes, falling back to pure black or white. That
 * fallback always succeeds: the worst possible background luminance still clears 4.58:1 against
 * whichever of black/white is further from it.
 */
object SeedColorScheme {

    /** Error roles stay fixed across presets: a red error is a convention, not a brand choice. */
    private const val LIGHT_ERROR = 0xFFB3261EL
    private const val LIGHT_ON_ERROR = 0xFFFFFFFFL
    private const val LIGHT_ERROR_CONTAINER = 0xFFF9DEDCL
    private const val LIGHT_ON_ERROR_CONTAINER = 0xFF410E0BL
    private const val DARK_ERROR = 0xFFF2B8B5L
    private const val DARK_ON_ERROR = 0xFF601410L
    private const val DARK_ERROR_CONTAINER = 0xFF8C1D18L
    private const val DARK_ON_ERROR_CONTAINER = 0xFFF9DEDCL

    /** Hue offset for the tertiary family, in degrees. M3 uses a comparable analogous shift. */
    private const val TERTIARY_HUE_SHIFT = 60f

    /** Contrast sweep granularity, in lightness units. 64 steps covers the full 0..1 range. */
    private const val SWEEP_STEP = 0.02f

    fun generate(seedArgb: Long, dark: Boolean): SeedScheme = if (dark) generateDark(seedArgb) else generateLight(seedArgb)

    private fun generateLight(seedArgb: Long): SeedScheme {
        val (hue, seedSaturation, _) = ColorSpaces.hslOf(seedArgb)
        // A near-grey seed (the GRAPHITE preset) must stay near-grey: no saturation floor is applied
        // to the chromatic roles. Only the neutrals are damped, which is a no-op when sat is already 0.
        val sat = seedSaturation.coerceIn(0f, 1f)
        val tertiaryHue = hue + TERTIARY_HUE_SHIFT

        val primary = ColorSpaces.fromHsl(hue, sat, 0.42f)
        val primaryContainer = ColorSpaces.fromHsl(hue, sat * 0.60f, 0.90f)
        val secondary = ColorSpaces.fromHsl(hue, sat * 0.40f, 0.40f)
        val secondaryContainer = ColorSpaces.fromHsl(hue, sat * 0.30f, 0.90f)
        val tertiary = ColorSpaces.fromHsl(tertiaryHue, sat * 0.55f, 0.42f)
        val tertiaryContainer = ColorSpaces.fromHsl(tertiaryHue, sat * 0.40f, 0.90f)
        val background = ColorSpaces.fromHsl(hue, sat * 0.10f, 0.985f)
        val surface = background
        val surfaceVariant = ColorSpaces.fromHsl(hue, sat * 0.14f, 0.91f)

        return SeedScheme(
            primary = primary,
            onPrimary = onColor(hue, sat * 0.10f, 0.99f, listOf(primary)),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onColor(hue, sat, 0.16f, listOf(primaryContainer)),
            secondary = secondary,
            onSecondary = onColor(hue, sat * 0.10f, 0.99f, listOf(secondary)),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onColor(hue, sat * 0.70f, 0.15f, listOf(secondaryContainer)),
            tertiary = tertiary,
            onTertiary = onColor(tertiaryHue, sat * 0.10f, 0.99f, listOf(tertiary)),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onColor(tertiaryHue, sat * 0.70f, 0.15f, listOf(tertiaryContainer)),
            error = LIGHT_ERROR,
            onError = LIGHT_ON_ERROR,
            errorContainer = LIGHT_ERROR_CONTAINER,
            onErrorContainer = LIGHT_ON_ERROR_CONTAINER,
            background = background,
            onBackground = onColor(hue, sat * 0.18f, 0.11f, listOf(background)),
            surface = surface,
            // onSurface lands on surfaceVariant too (cards, list rows), so it is held against both.
            onSurface = onColor(hue, sat * 0.18f, 0.11f, listOf(surface, surfaceVariant)),
            surfaceVariant = surfaceVariant,
            // Same for the muted body colour used for every subtitle in the app.
            onSurfaceVariant = onColor(hue, sat * 0.20f, 0.30f, listOf(surfaceVariant, surface)),
            outline = ColorSpaces.fromHsl(hue, sat * 0.14f, 0.48f),
            outlineVariant = ColorSpaces.fromHsl(hue, sat * 0.14f, 0.79f)
        )
    }

    private fun generateDark(seedArgb: Long): SeedScheme {
        val (hue, seedSaturation, _) = ColorSpaces.hslOf(seedArgb)
        val sat = seedSaturation.coerceIn(0f, 1f)
        val tertiaryHue = hue + TERTIARY_HUE_SHIFT

        val primary = ColorSpaces.fromHsl(hue, sat * 0.70f, 0.80f)
        val primaryContainer = ColorSpaces.fromHsl(hue, sat * 0.75f, 0.38f)
        val secondary = ColorSpaces.fromHsl(hue, sat * 0.30f, 0.80f)
        val secondaryContainer = ColorSpaces.fromHsl(hue, sat * 0.28f, 0.32f)
        val tertiary = ColorSpaces.fromHsl(tertiaryHue, sat * 0.45f, 0.82f)
        val tertiaryContainer = ColorSpaces.fromHsl(tertiaryHue, sat * 0.45f, 0.36f)
        val background = ColorSpaces.fromHsl(hue, sat * 0.12f, 0.11f)
        val surface = background
        val surfaceVariant = ColorSpaces.fromHsl(hue, sat * 0.12f, 0.30f)

        return SeedScheme(
            primary = primary,
            onPrimary = onColor(hue, sat * 0.90f, 0.20f, listOf(primary)),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onColor(hue, sat * 0.45f, 0.90f, listOf(primaryContainer)),
            secondary = secondary,
            onSecondary = onColor(hue, sat * 0.50f, 0.20f, listOf(secondary)),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onColor(hue, sat * 0.25f, 0.90f, listOf(secondaryContainer)),
            tertiary = tertiary,
            onTertiary = onColor(tertiaryHue, sat * 0.60f, 0.22f, listOf(tertiary)),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onColor(tertiaryHue, sat * 0.30f, 0.91f, listOf(tertiaryContainer)),
            error = DARK_ERROR,
            onError = DARK_ON_ERROR,
            errorContainer = DARK_ERROR_CONTAINER,
            onErrorContainer = DARK_ON_ERROR_CONTAINER,
            background = background,
            onBackground = onColor(hue, sat * 0.10f, 0.90f, listOf(background)),
            surface = surface,
            onSurface = onColor(hue, sat * 0.10f, 0.90f, listOf(surface, surfaceVariant)),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onColor(hue, sat * 0.10f, 0.79f, listOf(surfaceVariant, surface)),
            outline = ColorSpaces.fromHsl(hue, sat * 0.10f, 0.58f),
            outlineVariant = ColorSpaces.fromHsl(hue, sat * 0.12f, 0.30f)
        )
    }

    /**
     * Builds a foreground colour at [lightness] and then guarantees it clears AA against every
     * colour in [bases], sweeping its lightness away from them and finally falling back to pure
     * black or white.
     */
    private fun onColor(hue: Float, saturation: Float, lightness: Float, bases: List<Long>): Long {
        val candidate = ColorSpaces.fromHsl(hue, saturation, lightness)
        if (bases.all { ColorContrast.meetsAaBodyText(candidate, it) }) return candidate

        // Sweep toward whichever extreme is further from the *hardest* base, so one direction is
        // tried exhaustively before giving up rather than alternating around the failure.
        val towardWhite = bases.minOf { ColorContrast.ratio(0xFFFFFFFFL, it) } >=
            bases.minOf { ColorContrast.ratio(0xFF000000L, it) }
        var swept = lightness
        while (swept in 0f..1f) {
            swept += if (towardWhite) SWEEP_STEP else -SWEEP_STEP
            val next = ColorSpaces.fromHsl(hue, saturation, swept.coerceIn(0f, 1f))
            if (bases.all { ColorContrast.meetsAaBodyText(next, it) }) return next
        }
        return if (towardWhite) 0xFFFFFFFFL else 0xFF000000L
    }
}
