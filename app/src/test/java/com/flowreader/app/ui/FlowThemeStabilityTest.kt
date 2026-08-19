package com.flowreader.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.flowreader.app.core.designsystem.theme.FlowTheme
import com.flowreader.app.domain.model.AppColorPreset
import com.flowreader.app.domain.model.AppThemeMode
import com.flowreader.app.domain.model.ColorSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins `FlowTheme`'s ColorScheme *identity* contract.
 *
 * Why identity and not colour values: `MaterialTheme` publishes its `ColorScheme` through a
 * **`staticCompositionLocalOf`** (verified in the material3 1.3.1 bytecode) and `ColorScheme` does
 * **not** override `equals` — it declares only `toString`. A static composition local rebuilds its
 * entire subtree when the provided value changes, and under identity comparison "changed" means
 * "is a different instance". Handing `MaterialTheme` a freshly built scheme therefore recomposes the
 * whole app UI, however identical its colours are.
 *
 * Two honest notes about what this does and does not prove:
 *
 * 1. This is **not** a regression gate for a bug that shipped. v56.6.0 computed the scheme inline,
 *    which looks like the above failure — but the compiler reports `FlowTheme` as
 *    `restartable skippable` with all parameters `stable`, so Compose skips it whenever its inputs
 *    are unchanged and the body never re-ran. An earlier version of this test "passed" against the
 *    inline code for exactly that reason.
 * 2. What it does guard is the **outcome**: a stable scheme instance across content recomposition.
 *    That outcome currently rests on two independent mechanisms (skippability *and* the `remember`
 *    in `FlowTheme`). If a future edit adds an unstable parameter and silently costs `FlowTheme` its
 *    skippability — the exact regression class v56.5.2 was about — this test still holds the line.
 *
 * [changingTheSeedDoesProduceANewScheme] covers the other half: caching must not outlive its inputs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FlowThemeStabilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Recomposes [FlowTheme] [times] times without changing any of its colour inputs, and returns
     * every `ColorScheme` instance `MaterialTheme` saw.
     */
    private fun captureSchemesAcrossRecompositions(
        colorSource: ColorSource,
        colorPreset: AppColorPreset,
        customSeedArgb: Long?,
        times: Int = 3
    ): List<ColorScheme> {
        val captured = mutableListOf<ColorScheme>()
        var tick by mutableIntStateOf(0)

        composeRule.setContent {
            // `tick` is read HERE, in the scope that *calls* FlowTheme — not inside the content
            // lambda. That distinction is the whole test: Compose invalidates the narrowest scope
            // that read the state, so reading it inside `content` would recompose only the content
            // and leave FlowTheme itself untouched — which passes whether or not the bug is present.
            // Reading it out here re-invokes FlowTheme with a fresh `content` lambda, which is
            // exactly what `FlowReaderNavHost` does on every navigation.
            val currentTick = tick
            FlowTheme(
                themeMode = AppThemeMode.LIGHT,
                colorSource = colorSource,
                colorPreset = colorPreset,
                customSeedArgb = customSeedArgb
            ) {
                captured += MaterialTheme.colorScheme
                Text("tick $currentTick")
            }
        }
        composeRule.waitForIdle()

        repeat(times) {
            tick += 1
            composeRule.waitForIdle()
        }
        return captured
    }

    @Test
    fun customSeedKeepsTheSameColorSchemeInstanceAcrossRecompositions() {
        val schemes = captureSchemesAcrossRecompositions(
            colorSource = ColorSource.CUSTOM,
            colorPreset = AppColorPreset.VIOLET,
            customSeedArgb = 0xFF0B6BC4
        )
        assertEquals(1, schemes.distinctBy { System.identityHashCode(it) }.size)
        schemes.forEach { assertSame(schemes.first(), it) }
    }

    @Test
    fun generatedPresetKeepsTheSameColorSchemeInstanceAcrossRecompositions() {
        // TEAL is generated, unlike VIOLET which returns a singleton and would pass either way.
        val schemes = captureSchemesAcrossRecompositions(
            colorSource = ColorSource.BRAND,
            colorPreset = AppColorPreset.TEAL,
            customSeedArgb = null
        )
        assertEquals(1, schemes.distinctBy { System.identityHashCode(it) }.size)
    }

    @Test
    fun violetDefaultAlsoStaysStable() {
        val schemes = captureSchemesAcrossRecompositions(
            colorSource = ColorSource.BRAND,
            colorPreset = AppColorPreset.VIOLET,
            customSeedArgb = null
        )
        assertEquals(1, schemes.distinctBy { System.identityHashCode(it) }.size)
    }

    @Test
    fun changingTheSeedDoesProduceANewScheme() {
        // The other half of the contract: caching must not outlive the inputs, or picking a colour
        // in the studio would do nothing until process death.
        var seed by mutableIntStateOf(0)
        val captured = mutableListOf<ColorScheme>()

        composeRule.setContent {
            FlowTheme(
                themeMode = AppThemeMode.LIGHT,
                colorSource = ColorSource.CUSTOM,
                customSeedArgb = if (seed == 0) 0xFF0B6BC4 else 0xFFB3261E
            ) {
                captured += MaterialTheme.colorScheme
                Text("seed $seed")
            }
        }
        composeRule.waitForIdle()
        val before = captured.last()

        seed = 1
        composeRule.waitForIdle()
        val after = captured.last()

        assertNotSame(before, after)
        // assertNotEquals, not assertNotSame: Color is a value class, so identity says nothing here.
        assertNotEquals(before.primary, after.primary)
    }
}
