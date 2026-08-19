package com.flowreader.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorSpacesTest {

    @Test
    fun primariesRoundTripThroughHsv() {
        listOf(0xFFFF0000L, 0xFF00FF00L, 0xFF0000FFL, 0xFFFFFF00L, 0xFF00FFFFL, 0xFFFF00FFL).forEach { argb ->
            val hsv = ColorSpaces.toHsv(argb)
            assertEquals("round trip failed for ${ColorSpaces.toHexString(argb)}", argb, ColorSpaces.fromHsv(hsv))
        }
    }

    @Test
    fun arbitraryColorsRoundTripWithinRounding() {
        // 8-bit channels quantize, so a round trip is allowed to move by one step per channel.
        listOf(0xFF6750A4L, 0xFF3A2C30L, 0xFF0B6BC4L, 0xFF5A5F66L, 0xFF123456L).forEach { argb ->
            val result = ColorSpaces.fromHsv(ColorSpaces.toHsv(argb))
            listOf(
                ColorSpaces.red(argb) to ColorSpaces.red(result),
                ColorSpaces.green(argb) to ColorSpaces.green(result),
                ColorSpaces.blue(argb) to ColorSpaces.blue(result)
            ).forEach { (expected, actual) ->
                assertTrue("channel drifted from $expected to $actual", kotlin.math.abs(expected - actual) <= 1)
            }
        }
    }

    @Test
    fun greyHasZeroSaturationAndNoHue() {
        val hsv = ColorSpaces.toHsv(0xFF808080L)
        assertEquals(0f, hsv.saturation, 0.001f)
        // 0, not NaN: the picker reads this back to place the ring handle.
        assertEquals(0f, hsv.hue, 0.001f)
    }

    @Test
    fun hslLightnessExtremesAreBlackAndWhite() {
        assertEquals(0xFF000000L, ColorSpaces.fromHsl(210f, 0.8f, 0f))
        assertEquals(0xFFFFFFFFL, ColorSpaces.fromHsl(210f, 0.8f, 1f))
    }

    @Test
    fun hslMidLightnessFullSaturationIsThePureHue() {
        assertEquals(0xFFFF0000L, ColorSpaces.fromHsl(0f, 1f, 0.5f))
        assertEquals(0xFF00FF00L, ColorSpaces.fromHsl(120f, 1f, 0.5f))
        assertEquals(0xFF0000FFL, ColorSpaces.fromHsl(240f, 1f, 0.5f))
    }

    @Test
    fun hslOfRoundTripsThroughFromHsl() {
        listOf(0xFF6750A4L, 0xFF14733CL, 0xFFB3261EL, 0xFFEADDFFL).forEach { argb ->
            val (hue, saturation, lightness) = ColorSpaces.hslOf(argb)
            val result = ColorSpaces.fromHsl(hue, saturation, lightness)
            listOf(
                ColorSpaces.red(argb) to ColorSpaces.red(result),
                ColorSpaces.green(argb) to ColorSpaces.green(result),
                ColorSpaces.blue(argb) to ColorSpaces.blue(result)
            ).forEach { (expected, actual) ->
                assertTrue("channel drifted from $expected to $actual", kotlin.math.abs(expected - actual) <= 1)
            }
        }
    }

    @Test
    fun hueWrapsIntoZeroTo360() {
        assertEquals(10f, ColorSpaces.normalizeHue(370f), 0.001f)
        assertEquals(350f, ColorSpaces.normalizeHue(-10f), 0.001f)
        assertEquals(0f, ColorSpaces.normalizeHue(360f), 0.001f)
        assertEquals(0f, ColorSpaces.normalizeHue(Float.NaN), 0.001f)
    }

    @Test
    fun generatedColorsAreAlwaysOpaque() {
        // Every consumer is a theme colour. A translucent role would make the contrast math — which
        // assumes opaque compositing — silently wrong rather than visibly broken.
        assertEquals(ColorSpaces.OPAQUE_ALPHA, ColorSpaces.fromHsv(200f, 0.5f, 0.5f) and 0xFF000000L)
        assertEquals(ColorSpaces.OPAQUE_ALPHA, ColorSpaces.fromHsl(200f, 0.5f, 0.5f) and 0xFF000000L)
        assertEquals(ColorSpaces.OPAQUE_ALPHA, ColorSpaces.argb(1, 2, 3) and 0xFF000000L)
    }

    @Test
    fun channelsClampInsteadOfWrapping() {
        assertEquals(0xFFFF0000L, ColorSpaces.argb(300, -5, 0))
    }

    @Test
    fun hexFormatsWithoutAlpha() {
        assertEquals("#6750A4", ColorSpaces.toHexString(0xFF6750A4L))
        assertEquals("#000000", ColorSpaces.toHexString(0xFF000000L))
    }

    @Test
    fun hexParsesAllThreeAcceptedForms() {
        assertEquals(0xFF6750A4L, ColorSpaces.parseHex("#6750A4"))
        assertEquals(0xFF6750A4L, ColorSpaces.parseHex("6750a4"))
        assertEquals(0xFFFFCC00L, ColorSpaces.parseHex("#FC0"))
        // Supplied alpha is discarded rather than honoured.
        assertEquals(0xFF6750A4L, ColorSpaces.parseHex("#806750A4"))
    }

    @Test
    fun hexRejectsNonColors() {
        listOf("", "#", "12345", "#GGGGGG", "#12345", "hello").forEach { raw ->
            assertNull("expected null for '$raw'", ColorSpaces.parseHex(raw))
        }
    }
}
