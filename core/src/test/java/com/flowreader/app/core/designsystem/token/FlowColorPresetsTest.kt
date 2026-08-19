package com.flowreader.app.core.designsystem.token

import androidx.compose.ui.graphics.Color
import com.flowreader.app.core.util.ColorContrast
import com.flowreader.app.domain.model.AppColorPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowColorPresetsTest {

    private fun argb(color: Color): Long = (color.value shr 32).toLong()

    @Test
    fun violetReturnsTheHandTunedBrandSchemeUntouched() {
        // The whole point of special-casing VIOLET: it is the default, so routing it through the
        // generator would recolor every existing install as a side effect of adding presets.
        // Identity, not approximation — assertEquals on the ColorScheme would pass on a copy, so
        // the individual roles that the generator would have changed are checked.
        val light = FlowColorPresets.schemeOf(AppColorPreset.VIOLET, dark = false)
        assertEquals(FlowBrandColors.LightPrimary, light.primary)
        assertEquals(FlowBrandColors.LightOnPrimary, light.onPrimary)
        assertEquals(FlowBrandColors.LightSurface, light.surface)
        assertEquals(FlowBrandColors.LightOnSurfaceVariant, light.onSurfaceVariant)
        assertEquals(FlowBrandColors.LightOutlineVariant, light.outlineVariant)

        val dark = FlowColorPresets.schemeOf(AppColorPreset.VIOLET, dark = true)
        assertEquals(FlowBrandColors.DarkPrimary, dark.primary)
        assertEquals(FlowBrandColors.DarkOnPrimary, dark.onPrimary)
        assertEquals(FlowBrandColors.DarkSurface, dark.surface)
        assertEquals(FlowBrandColors.DarkOnSurfaceVariant, dark.onSurfaceVariant)
        assertEquals(FlowBrandColors.DarkOutlineVariant, dark.outlineVariant)
    }

    @Test
    fun violetSeedMatchesTheBrandPrimary() {
        // Only used when CUSTOM has no stored seed, but it should still be the brand colour rather
        // than an unrelated purple.
        assertEquals(argb(FlowBrandColors.LightPrimary), FlowColorPresets.seedOf(AppColorPreset.VIOLET))
    }

    @Test
    fun everyPresetHasASeed() {
        // seedOf uses getValue, so a preset added to the enum without a seed here throws rather
        // than silently rendering violet.
        AppColorPreset.entries.forEach { preset ->
            assertTrue("${preset.name} has a transparent seed", FlowColorPresets.seedOf(preset) and 0xFF000000L != 0L)
        }
    }

    @Test
    fun seedsAreDistinct() {
        val seeds = AppColorPreset.entries.map { FlowColorPresets.seedOf(it) }
        assertEquals(seeds.size, seeds.distinct().size)
    }

    @Test
    fun nonVioletPresetsAreGeneratedAndDifferFromTheBrandScheme() {
        AppColorPreset.entries.filter { it != AppColorPreset.VIOLET }.forEach { preset ->
            val scheme = FlowColorPresets.schemeOf(preset, dark = false)
            assertNotEquals("${preset.name} did not change primary", FlowBrandColors.LightPrimary, scheme.primary)
        }
    }

    @Test
    fun everyPresetSchemeKeepsBodyTextReadableInBothModes() {
        // Same guarantee as SeedColorSchemeTest, asserted one level up: through the Compose mapping,
        // so a role wired to the wrong field would be caught here rather than shipping.
        AppColorPreset.entries.forEach { preset ->
            listOf(false, true).forEach { dark ->
                val scheme = FlowColorPresets.schemeOf(preset, dark)
                listOf(
                    "onSurface/surface" to (scheme.onSurface to scheme.surface),
                    "onSurfaceVariant/surface" to (scheme.onSurfaceVariant to scheme.surface),
                    "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
                    "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
                    "onSecondaryContainer/secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
                    "onTertiary/tertiary" to (scheme.onTertiary to scheme.tertiary)
                ).forEach { (name, pair) ->
                    val ratio = ColorContrast.ratio(argb(pair.first), argb(pair.second))
                    assertTrue(
                        "${preset.name} dark=$dark $name = ${"%.2f".format(ratio)}:1",
                        ratio >= ColorContrast.AA_BODY_TEXT
                    )
                }
            }
        }
    }

    @Test
    fun customSeedsGenerateReadableSchemes() {
        listOf(0xFF00FF00L, 0xFFFFFF00L, 0xFF000000L, 0xFFFFFFFFL, 0xFF888888L).forEach { seed ->
            listOf(false, true).forEach { dark ->
                val scheme = FlowColorPresets.schemeFromSeed(seed, dark)
                val ratio = ColorContrast.ratio(argb(scheme.onSurface), argb(scheme.surface))
                assertTrue("seed $seed dark=$dark body ratio ${"%.2f".format(ratio)}", ratio >= ColorContrast.AA_BODY_TEXT)
            }
        }
    }

    @Test
    fun everySeedIsDarkEnoughForTheWhiteSelectionCheckmark() {
        // `ColorPresetGrid` tints the selected swatch's check mark white unconditionally. That is
        // only safe while every seed is dark; a light seed added later would make the check mark
        // invisible with nothing else failing. WCAG 1.4.11 puts non-text UI contrast at 3:1.
        AppColorPreset.entries.forEach { preset ->
            val ratio = ColorContrast.ratio(0xFFFFFFFFL, FlowColorPresets.seedOf(preset))
            assertTrue(
                "${preset.name}: white check mark is only ${"%.2f".format(ratio)}:1 on this seed — " +
                    "either darken the seed or make the tint contrast-aware",
                ratio >= ColorContrast.AA_LARGE_TEXT
            )
        }
    }

    @Test
    fun swatchMatchesTheSeed() {
        AppColorPreset.entries.forEach { preset ->
            assertEquals(FlowColorPresets.seedOf(preset), argb(FlowColorPresets.swatchOf(preset)))
        }
    }
}
