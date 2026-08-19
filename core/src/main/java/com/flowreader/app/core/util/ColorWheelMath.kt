package com.flowreader.app.core.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geometry for the color studio's three pickers (v56.6): the hue ring, the saturation/value square
 * inscribed in it, and the linear hue / brightness bars.
 *
 * Compose-free on purpose. Hit-testing and selector placement are where an off-by-one in the
 * inverse mapping shows up as "the dot doesn't follow my finger", and that is only cheap to catch in
 * a JVM test. The composable in `:app` owns drawing and gesture plumbing; every coordinate it uses
 * comes from here.
 *
 * Coordinate convention is the Compose one: origin top-left, **y grows downward**. Angles therefore
 * run clockwise from the 3 o'clock position, which is also how the drawn sweep gradient runs, so
 * hue 0 (red) sits at the right and increases clockwise.
 */
object ColorWheelMath {

    /** Ring thickness as a fraction of the canvas side. */
    const val RING_THICKNESS_RATIO = 0.13f

    /** Gap between the ring's inner edge and the SV square's corners, as a fraction of the side. */
    private const val SQUARE_GAP_RATIO = 0.035f

    fun outerRadius(side: Float): Float = side / 2f

    fun ringThickness(side: Float): Float = side * RING_THICKNESS_RATIO

    fun innerRadius(side: Float): Float = outerRadius(side) - ringThickness(side)

    /**
     * Side length of the largest axis-aligned square that fits inside the ring's inner circle, less
     * a small gap. A square inscribed in a circle of radius r has side `r * sqrt(2)`.
     */
    fun squareSide(side: Float): Float = ((innerRadius(side) - side * SQUARE_GAP_RATIO) * sqrt(2f)).coerceAtLeast(0f)

    /** Top-left offset of the SV square within a [side] x [side] canvas, as (x, y). */
    fun squareTopLeft(side: Float): Pair<Float, Float> {
        val origin = (side - squareSide(side)) / 2f
        return origin to origin
    }

    /** True when a touch at ([x], [y]) landed on the hue ring rather than inside or outside it. */
    fun isInRing(x: Float, y: Float, side: Float): Boolean {
        val center = side / 2f
        val distance = hypot(x - center, y - center)
        return distance in innerRadius(side)..outerRadius(side)
    }

    /** True when a touch at ([x], [y]) landed inside the SV square. */
    fun isInSquare(x: Float, y: Float, side: Float): Boolean {
        val (left, top) = squareTopLeft(side)
        val squareSide = squareSide(side)
        return x >= left && x <= left + squareSide && y >= top && y <= top + squareSide
    }

    /**
     * Hue of a ring touch, in degrees. Reads the angle only, so a drag that wanders off the ring
     * still tracks — the caller decides whether the *initial* touch counted as a ring grab.
     */
    fun hueAt(x: Float, y: Float, side: Float): Float {
        val center = side / 2f
        val degrees = Math.toDegrees(atan2((y - center).toDouble(), (x - center).toDouble())).toFloat()
        return ColorSpaces.normalizeHue(degrees)
    }

    /** Center of the ring's selector handle for [hue], as (x, y). */
    fun ringHandleCenter(hue: Float, side: Float): Pair<Float, Float> {
        val center = side / 2f
        val radius = (innerRadius(side) + outerRadius(side)) / 2f
        val radians = Math.toRadians(ColorSpaces.normalizeHue(hue).toDouble())
        return (center + radius * cos(radians).toFloat()) to (center + radius * sin(radians).toFloat())
    }

    /**
     * Saturation and value for a touch inside the SV square: saturation grows left→right, value
     * grows bottom→top. Values are clamped, so a drag that leaves the square pins to its edge
     * instead of jumping.
     */
    fun saturationValueAt(x: Float, y: Float, side: Float): Pair<Float, Float> {
        val (left, top) = squareTopLeft(side)
        val squareSide = squareSide(side)
        if (squareSide <= 0f) return 0f to 0f
        val saturation = ((x - left) / squareSide).coerceIn(0f, 1f)
        val value = (1f - (y - top) / squareSide).coerceIn(0f, 1f)
        return saturation to value
    }

    /** Inverse of [saturationValueAt]: where to draw the square's selector dot, as (x, y). */
    fun squareHandleCenter(saturation: Float, value: Float, side: Float): Pair<Float, Float> {
        val (left, top) = squareTopLeft(side)
        val squareSide = squareSide(side)
        val x = left + saturation.coerceIn(0f, 1f) * squareSide
        val y = top + (1f - value.coerceIn(0f, 1f)) * squareSide
        return x to y
    }

    /** Hue for a touch at [x] on a horizontal spectrum bar of [width]. */
    fun hueAtBar(x: Float, width: Float): Float {
        if (width <= 0f) return 0f
        // 359.999f, not 360f: a full-right tap must stay in [0, 360) so it round-trips to red
        // without wrapping to hue 0 and snapping the ring handle back to the left.
        return (x / width).coerceIn(0f, 1f) * 359.999f
    }

    /** Fraction along a horizontal bar for [hue], for placing that bar's handle. */
    fun barFractionForHue(hue: Float): Float = ColorSpaces.normalizeHue(hue) / 360f

    /** Value (brightness) for a touch at [x] on a horizontal bar of [width]. */
    fun valueAtBar(x: Float, width: Float): Float {
        if (width <= 0f) return 0f
        return (x / width).coerceIn(0f, 1f)
    }

    private fun hypot(dx: Float, dy: Float): Float = sqrt(dx * dx + dy * dy)
}
