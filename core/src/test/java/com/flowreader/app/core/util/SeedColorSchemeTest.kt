package com.flowreader.app.core.util

import com.flowreader.app.core.designsystem.token.FlowColorPresets
import com.flowreader.app.domain.model.AppColorPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The AA guarantee in [SeedColorScheme]'s contract, asserted rather than assumed.
 *
 * These are the tests the generator exists for: hand-checking 12 presets x 2 modes x 12 text pairs
 * is 288 contrast ratios, and a custom seed is unbounded, so the property has to be machine-checked.
 */
class SeedColorSchemeTest {

    /** Every (foreground, background) pair the app can actually render text with. */
    private fun textPairs(scheme: SeedScheme): List<Triple<String, Long, Long>> = listOf(
        Triple("onPrimary/primary", scheme.onPrimary, scheme.primary),
        Triple("onPrimaryContainer/primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer),
        Triple("onSecondary/secondary", scheme.onSecondary, scheme.secondary),
        Triple("onSecondaryContainer/secondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer),
        Triple("onTertiary/tertiary", scheme.onTertiary, scheme.tertiary),
        Triple("onTertiaryContainer/tertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer),
        Triple("onError/error", scheme.onError, scheme.error),
        Triple("onErrorContainer/errorContainer", scheme.onErrorContainer, scheme.errorContainer),
        Triple("onBackground/background", scheme.onBackground, scheme.background),
        Triple("onSurface/surface", scheme.onSurface, scheme.surface),
        Triple("onSurfaceVariant/surfaceVariant", scheme.onSurfaceVariant, scheme.surfaceVariant),
        // The app puts onSurfaceVariant on plain surface for every subtitle, and onSurface on
        // surfaceVariant inside cards, so both cross pairs are load-bearing too.
        Triple("onSurfaceVariant/surface", scheme.onSurfaceVariant, scheme.surface),
        Triple("onSurface/surfaceVariant", scheme.onSurface, scheme.surfaceVariant)
    )

    private fun assertAllPairsMeetAa(label: String, scheme: SeedScheme) {
        textPairs(scheme).forEach { (name, foreground, background) ->
            val ratio = ColorContrast.ratio(foreground, background)
            assertTrue(
                "$label $name = ${"%.2f".format(ratio)}:1, below AA " +
                    "(${ColorSpaces.toHexString(foreground)} on ${ColorSpaces.toHexString(background)})",
                ratio >= ColorContrast.AA_BODY_TEXT
            )
        }
    }

    @Test
    fun everyPresetSeedMeetsAaInBothModes() {
        AppColorPreset.entries.forEach { preset ->
            val seed = FlowColorPresets.seedOf(preset)
            assertAllPairsMeetAa("${preset.name} light", SeedColorScheme.generate(seed, dark = false))
            assertAllPairsMeetAa("${preset.name} dark", SeedColorScheme.generate(seed, dark = true))
        }
    }

    @Test
    fun aaHoldsForEveryHueAtFullSaturation() {
        // Sweeps the whole hue circle: yellow-green seeds are the ones where a naive tone ladder
        // puts onPrimary and primary at nearly the same luminance.
        (0 until 360 step 5).forEach { hue ->
            val seed = ColorSpaces.fromHsv(hue.toFloat(), 1f, 1f)
            assertAllPairsMeetAa("hue $hue light", SeedColorScheme.generate(seed, dark = false))
            assertAllPairsMeetAa("hue $hue dark", SeedColorScheme.generate(seed, dark = true))
        }
    }

    @Test
    fun aaHoldsForTheDegenerateSeeds() {
        // Pure black, pure white and mid grey have no hue and no saturation to work with; the
        // generator must still produce a readable neutral scheme rather than a flat one.
        listOf(0xFF000000L, 0xFFFFFFFFL, 0xFF808080L, 0xFF010101L).forEach { seed ->
            assertAllPairsMeetAa("${ColorSpaces.toHexString(seed)} light", SeedColorScheme.generate(seed, dark = false))
            assertAllPairsMeetAa("${ColorSpaces.toHexString(seed)} dark", SeedColorScheme.generate(seed, dark = true))
        }
    }

    @Test
    fun lightAndDarkDifferInSurfaceLuminance() {
        val seed = FlowColorPresets.seedOf(AppColorPreset.AZURE)
        val light = SeedColorScheme.generate(seed, dark = false)
        val dark = SeedColorScheme.generate(seed, dark = true)
        assertTrue(
            "light surface should be brighter than dark surface",
            ColorContrast.relativeLuminance(light.surface) > ColorContrast.relativeLuminance(dark.surface)
        )
        assertTrue("light surface should read as light", ColorContrast.relativeLuminance(light.surface) > 0.5)
        assertTrue("dark surface should read as dark", ColorContrast.relativeLuminance(dark.surface) < 0.1)
    }

    @Test
    fun generationIsDeterministic() {
        val seed = 0xFF7A3C93L
        assertEquals(SeedColorScheme.generate(seed, dark = false), SeedColorScheme.generate(seed, dark = false))
    }

    @Test
    fun alphaIsIgnoredSoOnlyTheRgbSeedMatters() {
        assertEquals(
            SeedColorScheme.generate(0xFF0B6BC4L, dark = false),
            SeedColorScheme.generate(0x000B6BC4L, dark = false)
        )
    }

    @Test
    fun distinctSeedsProduceDistinctPrimaries() {
        // Guards against a generator that quietly collapses everything onto one neutral: 12 presets
        // that all render the same primary would make the whole grid decorative.
        val primaries = AppColorPreset.entries.map {
            SeedColorScheme.generate(FlowColorPresets.seedOf(it), dark = false).primary
        }
        assertEquals(primaries.size, primaries.distinct().size)
    }

    @Test
    fun errorRolesAreSeedIndependent() {
        // A red error is a convention, not a brand choice — a green "error" would be worse than
        // an off-brand one.
        val azure = SeedColorScheme.generate(FlowColorPresets.seedOf(AppColorPreset.AZURE), dark = false)
        val moss = SeedColorScheme.generate(FlowColorPresets.seedOf(AppColorPreset.MOSS), dark = false)
        assertEquals(azure.error, moss.error)
        assertEquals(azure.onError, moss.onError)
        assertNotEquals(azure.primary, moss.primary)
    }

    @Test
    fun graphiteStaysNeutral() {
        // No saturation floor is applied, so the neutral preset must actually be neutral: the
        // primary's channels should be within a couple of steps of each other.
        val scheme = SeedColorScheme.generate(FlowColorPresets.seedOf(AppColorPreset.GRAPHITE), dark = false)
        val r = ColorSpaces.red(scheme.primary)
        val g = ColorSpaces.green(scheme.primary)
        val b = ColorSpaces.blue(scheme.primary)
        val spread = maxOf(r, g, b) - minOf(r, g, b)
        assertTrue("graphite primary spread $spread is too chromatic", spread <= 24)
    }
}
