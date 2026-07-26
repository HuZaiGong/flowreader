package com.flowreader.app.core.util

import androidx.compose.ui.graphics.Color
import com.flowreader.app.core.designsystem.token.FlowBrandColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {

    private fun argb(color: Color): Long = (color.value shr 32).toLong()

    @Test
    fun blackOnWhiteIsTheMaximumRatio() {
        assertEquals(21.0, ColorContrast.ratio(0xFF000000, 0xFFFFFFFF), 0.01)
    }

    @Test
    fun identicalColorsHaveNoContrast() {
        assertEquals(1.0, ColorContrast.ratio(0xFF336699, 0xFF336699), 0.0001)
    }

    @Test
    fun ratioIsSymmetric() {
        val forward = ColorContrast.ratio(0xFF1A1A1A, 0xFFFFFFFF)
        val backward = ColorContrast.ratio(0xFFFFFFFF, 0xFF1A1A1A)
        assertEquals(forward, backward, 0.0001)
    }

    @Test
    fun luminanceIgnoresTheAlphaChannel() {
        assertEquals(
            ColorContrast.relativeLuminance(0xFFFFFFFF),
            ColorContrast.relativeLuminance(0x00FFFFFF),
            0.0001
        )
    }

    @Test
    fun aaThresholdsAreEnforced() {
        assertTrue(ColorContrast.meetsAaBodyText(0xFF000000, 0xFFFFFFFF))
        assertFalse(ColorContrast.meetsAaBodyText(0xFF999999, 0xFFFFFFFF))
        assertTrue(ColorContrast.meetsAaLargeText(0xFF767676, 0xFFFFFFFF))
    }

    @Test
    fun brandSchemeBodyTextMeetsAa() {
        val lightPairs = listOf(
            argb(FlowBrandColors.LightOnSurface) to argb(FlowBrandColors.LightSurface),
            argb(FlowBrandColors.LightOnPrimary) to argb(FlowBrandColors.LightPrimary),
            argb(FlowBrandColors.LightOnSurfaceVariant) to argb(FlowBrandColors.LightSurfaceVariant),
            argb(FlowBrandColors.LightOnError) to argb(FlowBrandColors.LightError)
        )
        val darkPairs = listOf(
            argb(FlowBrandColors.DarkOnSurface) to argb(FlowBrandColors.DarkSurface),
            argb(FlowBrandColors.DarkOnPrimary) to argb(FlowBrandColors.DarkPrimary),
            argb(FlowBrandColors.DarkOnSurfaceVariant) to argb(FlowBrandColors.DarkSurfaceVariant),
            argb(FlowBrandColors.DarkOnError) to argb(FlowBrandColors.DarkError)
        )
        (lightPairs + darkPairs).forEach { (foreground, background) ->
            val ratio = ColorContrast.ratio(foreground, background)
            assertTrue("contrast $ratio below AA for $foreground on $background", ratio >= ColorContrast.AA_BODY_TEXT)
        }
    }
}
