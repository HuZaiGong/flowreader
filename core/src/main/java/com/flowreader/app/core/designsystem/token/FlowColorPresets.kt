package com.flowreader.app.core.designsystem.token

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.flowreader.app.core.util.ColorSpaces
import com.flowreader.app.core.util.SeedColorScheme
import com.flowreader.app.core.util.SeedScheme
import com.flowreader.app.domain.model.AppColorPreset

/**
 * Seed colors for the 12 built-in [AppColorPreset]s, and the mapping from a seed to a Compose
 * [ColorScheme] (v56.6).
 *
 * [AppColorPreset.VIOLET] returns [FlowLightColorScheme] / [FlowDarkColorScheme] **verbatim** rather
 * than anything generated. Those 50 values are hand-tuned and are what every existing install and
 * every Roborazzi golden already shows; routing the default through a generator would shift the
 * app's colors for every current user as a side effect of adding presets. The seed below is only
 * used when VIOLET is the fallback for a CUSTOM source with no stored seed.
 */
object FlowColorPresets {

    private val seeds: Map<AppColorPreset, Long> = mapOf(
        AppColorPreset.VIOLET to 0xFF6750A4,
        AppColorPreset.INDIGO to 0xFF4355B9,
        AppColorPreset.AZURE to 0xFF0B6BC4,
        AppColorPreset.TEAL to 0xFF00696E,
        AppColorPreset.EMERALD to 0xFF14733C,
        AppColorPreset.MOSS to 0xFF5A6B22,
        AppColorPreset.AMBER to 0xFF8A5A00,
        AppColorPreset.TANGERINE to 0xFFA24312,
        AppColorPreset.CRIMSON to 0xFFB3261E,
        AppColorPreset.ROSE to 0xFFA3355C,
        AppColorPreset.PLUM to 0xFF7A3C93,
        AppColorPreset.GRAPHITE to 0xFF5A5F66
    )

    fun seedOf(preset: AppColorPreset): Long = seeds.getValue(preset)

    /** The swatch drawn in the settings grid: the preset's seed as a Compose color. */
    fun swatchOf(preset: AppColorPreset): Color = Color(seedOf(preset))

    fun schemeOf(preset: AppColorPreset, dark: Boolean): ColorScheme = when (preset) {
        AppColorPreset.VIOLET -> if (dark) FlowDarkColorScheme else FlowLightColorScheme
        else -> schemeFromSeed(seedOf(preset), dark)
    }

    /**
     * Generates a scheme from an arbitrary user seed. Alpha is forced opaque — a translucent seed
     * would make every generated role translucent, and the contrast math assumes opaque colors.
     */
    fun schemeFromSeed(seedArgb: Long, dark: Boolean): ColorScheme =
        SeedColorScheme.generate(ColorSpaces.OPAQUE_ALPHA or (seedArgb and 0xFFFFFF), dark).toColorScheme(dark)
}

private fun SeedScheme.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant)
    )
}
