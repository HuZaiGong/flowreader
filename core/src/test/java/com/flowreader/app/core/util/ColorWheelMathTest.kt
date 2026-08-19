package com.flowreader.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorWheelMathTest {

    private val side = 240f

    @Test
    fun ringSitsBetweenTheTwoRadii() {
        val center = side / 2f
        val ringRadius = (ColorWheelMath.innerRadius(side) + ColorWheelMath.outerRadius(side)) / 2f
        assertTrue(ColorWheelMath.isInRing(center + ringRadius, center, side))
        // Dead centre is the square, not the ring; beyond the outer radius is outside the wheel.
        assertFalse(ColorWheelMath.isInRing(center, center, side))
        assertFalse(ColorWheelMath.isInRing(center + ColorWheelMath.outerRadius(side) + 5f, center, side))
    }

    @Test
    fun squareIsInsideTheRingAndCentered() {
        val (left, top) = ColorWheelMath.squareTopLeft(side)
        assertEquals(left, top, 0.001f)
        val squareSide = ColorWheelMath.squareSide(side)
        assertEquals(side / 2f, left + squareSide / 2f, 0.001f)
        assertTrue("square must fit inside the ring", squareSide < ColorWheelMath.innerRadius(side) * 2f)
        assertTrue(ColorWheelMath.isInSquare(side / 2f, side / 2f, side))
        assertFalse(ColorWheelMath.isInSquare(left - 2f, top - 2f, side))
    }

    @Test
    fun squareCornersStayWithinTheInnerCircle() {
        // A square whose corners poked past the ring's inner edge would be clipped by the ring.
        val squareSide = ColorWheelMath.squareSide(side)
        val cornerDistance = (squareSide / 2f) * kotlin.math.sqrt(2f)
        assertTrue(
            "corner at $cornerDistance exceeds inner radius ${ColorWheelMath.innerRadius(side)}",
            cornerDistance <= ColorWheelMath.innerRadius(side)
        )
    }

    @Test
    fun hueFollowsTheClockwiseSweepFromThreeOClock() {
        // Compose's y grows downward and Brush.sweepGradient starts at 3 o'clock, so hue 0 must sit
        // right and increase clockwise. Getting this backwards makes the handle mirror the gradient.
        val center = side / 2f
        val radius = ColorWheelMath.innerRadius(side) + ColorWheelMath.ringThickness(side) / 2f
        assertEquals(0f, ColorWheelMath.hueAt(center + radius, center, side), 0.5f)
        assertEquals(90f, ColorWheelMath.hueAt(center, center + radius, side), 0.5f)
        assertEquals(180f, ColorWheelMath.hueAt(center - radius, center, side), 0.5f)
        assertEquals(270f, ColorWheelMath.hueAt(center, center - radius, side), 0.5f)
    }

    @Test
    fun ringHandleRoundTripsThroughHueAt() {
        listOf(0f, 37f, 120f, 210f, 299f, 359f).forEach { hue ->
            val (x, y) = ColorWheelMath.ringHandleCenter(hue, side)
            assertEquals("handle placement disagrees with hit-testing at $hue", hue, ColorWheelMath.hueAt(x, y, side), 0.5f)
        }
    }

    @Test
    fun ringHandleLandsOnTheRing() {
        listOf(0f, 45f, 137f, 300f).forEach { hue ->
            val (x, y) = ColorWheelMath.ringHandleCenter(hue, side)
            assertTrue("handle for hue $hue is off the ring", ColorWheelMath.isInRing(x, y, side))
        }
    }

    @Test
    fun squareMapsSaturationRightAndValueUp() {
        val (left, top) = ColorWheelMath.squareTopLeft(side)
        val squareSide = ColorWheelMath.squareSide(side)

        val (sTopLeft, vTopLeft) = ColorWheelMath.saturationValueAt(left, top, side)
        assertEquals(0f, sTopLeft, 0.01f)
        assertEquals(1f, vTopLeft, 0.01f)

        val (sBottomRight, vBottomRight) = ColorWheelMath.saturationValueAt(left + squareSide, top + squareSide, side)
        assertEquals(1f, sBottomRight, 0.01f)
        assertEquals(0f, vBottomRight, 0.01f)
    }

    @Test
    fun squareClampsInsteadOfJumpingWhenADragLeavesIt() {
        val (left, top) = ColorWheelMath.squareTopLeft(side)
        val (s, v) = ColorWheelMath.saturationValueAt(left - 500f, top - 500f, side)
        assertEquals(0f, s, 0.001f)
        assertEquals(1f, v, 0.001f)
    }

    @Test
    fun squareHandleRoundTripsThroughSaturationValueAt() {
        listOf(0f to 0f, 0.25f to 0.8f, 1f to 1f, 0.5f to 0.5f).forEach { (saturation, value) ->
            val (x, y) = ColorWheelMath.squareHandleCenter(saturation, value, side)
            val (s, v) = ColorWheelMath.saturationValueAt(x, y, side)
            assertEquals(saturation, s, 0.01f)
            assertEquals(value, v, 0.01f)
        }
    }

    @Test
    fun spectrumBarCoversTheHueCircleWithoutWrapping() {
        assertEquals(0f, ColorWheelMath.hueAtBar(0f, 300f), 0.01f)
        assertEquals(180f, ColorWheelMath.hueAtBar(150f, 300f), 0.5f)
        // The far right must stay under 360 so it does not normalize back to 0 and send the ring
        // handle to the opposite end of the bar.
        assertTrue(ColorWheelMath.hueAtBar(300f, 300f) < 360f)
        assertTrue(ColorWheelMath.hueAtBar(300f, 300f) > 359f)
    }

    @Test
    fun barFractionInvertsHueAtBar() {
        listOf(0f, 90f, 180f, 359f).forEach { hue ->
            val fraction = ColorWheelMath.barFractionForHue(hue)
            assertEquals(hue, ColorWheelMath.hueAtBar(fraction * 300f, 300f), 0.5f)
        }
    }

    @Test
    fun barsClampOutOfRangeTouches() {
        assertEquals(0f, ColorWheelMath.valueAtBar(-40f, 300f), 0.001f)
        assertEquals(1f, ColorWheelMath.valueAtBar(400f, 300f), 0.001f)
        assertEquals(0f, ColorWheelMath.hueAtBar(-10f, 300f), 0.001f)
    }

    @Test
    fun zeroSizedCanvasDoesNotDivideByZero() {
        // The first composition measures at 0 before layout lands.
        assertEquals(0f, ColorWheelMath.hueAtBar(10f, 0f), 0.001f)
        assertEquals(0f, ColorWheelMath.valueAtBar(10f, 0f), 0.001f)
        val (s, v) = ColorWheelMath.saturationValueAt(0f, 0f, 0f)
        assertEquals(0f, s, 0.001f)
        assertEquals(0f, v, 0.001f)
    }
}
