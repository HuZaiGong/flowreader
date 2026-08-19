package com.flowreader.app.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.flowreader.app.R
import com.flowreader.app.core.designsystem.token.FlowRadius
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.core.util.ColorContrast
import com.flowreader.app.core.util.ColorSpaces
import com.flowreader.app.core.util.ColorWheelMath
import com.flowreader.app.core.util.SeedColorScheme

/** The full-hue gradient shared by the ring and the spectrum bar. */
private val SPECTRUM_COLORS: List<Color> = (0..12).map { Color(ColorSpaces.fromHsv(it * 30f, 1f, 1f)) }

private const val WHEEL_SIZE_DP = 232

/**
 * The color studio (v56.6): picks the seed color that [com.flowreader.app.domain.model.ColorSource]
 * `CUSTOM` generates the whole Material scheme from.
 *
 * Offers the same color three ways, all bound to one HSV state, so any of them can be used to
 * finish an adjustment the others started: a **hue ring with an inscribed saturation/value square**,
 * a **spectrum bar** for hue and a brightness bar, and a **hex field**.
 *
 * The hex field is not a convenience — it is the accessible path. A canvas cannot be operated by
 * TalkBack, and the two bars carry `setProgress` semantics for the same reason.
 *
 * Nothing is written until 应用: the caller only hears about [onConfirm], so backing out of the
 * dialog leaves the live theme untouched.
 */
@Composable
fun ColorStudioDialog(
    initialArgb: Long,
    dark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    onClear: (() -> Unit)?
) {
    val initialHsv = remember(initialArgb) { ColorSpaces.toHsv(initialArgb) }
    var hue by remember(initialArgb) { mutableFloatStateOf(initialHsv.hue) }
    var saturation by remember(initialArgb) { mutableFloatStateOf(initialHsv.saturation) }
    var value by remember(initialArgb) { mutableFloatStateOf(initialHsv.value) }

    val argb = ColorSpaces.fromHsv(hue, saturation, value)

    // A null draft means "display the pickers' colour"; a non-null one means the user is mid-typing
    // and owns the field, so a half-entered "#1A" is not overwritten between keystrokes.
    //
    // The display is *derived* rather than stored on purpose. v56.6.0 kept it in state and assigned
    // to it during composition, then read it again in the same pass — a backwards write, so every
    // wheel drag paid two composition passes instead of one. The draft is only ever cleared from
    // event handlers below, never from composition.
    var hexDraft by remember(initialArgb) { mutableStateOf<String?>(null) }
    val hexDisplay = hexDraft ?: ColorSpaces.toHexString(argb)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.color_studio_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(FlowSpacing.md)
            ) {
                SchemePreview(argb = argb, dark = dark)

                // Every picker releases the hex draft: touching a picker means the user is no longer
                // typing, so the field should follow the colour again.
                HueRingPicker(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onHueChange = {
                        hue = it
                        hexDraft = null
                    },
                    onSaturationValueChange = { s, v ->
                        saturation = s
                        value = v
                        hexDraft = null
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(WHEEL_SIZE_DP.dp)
                )

                GradientBar(
                    label = stringResource(R.string.color_studio_spectrum),
                    fraction = ColorWheelMath.barFractionForHue(hue),
                    brush = Brush.horizontalGradient(SPECTRUM_COLORS),
                    onFractionChange = {
                        hue = it * 359.999f
                        hexDraft = null
                    }
                )

                GradientBar(
                    label = stringResource(R.string.color_studio_brightness),
                    fraction = value,
                    brush = Brush.horizontalGradient(
                        listOf(Color.Black, Color(ColorSpaces.fromHsv(hue, saturation, 1f)))
                    ),
                    onFractionChange = {
                        value = it
                        hexDraft = null
                    }
                )

                GradientBar(
                    label = stringResource(R.string.color_studio_saturation),
                    fraction = saturation,
                    brush = Brush.horizontalGradient(
                        listOf(Color(ColorSpaces.fromHsv(hue, 0f, value)), Color(ColorSpaces.fromHsv(hue, 1f, value)))
                    ),
                    onFractionChange = {
                        saturation = it
                        hexDraft = null
                    }
                )

                OutlinedTextField(
                    value = hexDisplay,
                    onValueChange = { raw ->
                        hexDraft = raw
                        ColorSpaces.parseHex(raw)?.let { parsed ->
                            val hsv = ColorSpaces.toHsv(parsed)
                            // Grey has no hue to read back, so keep the current one rather than
                            // snapping the ring to red when the user types #808080.
                            if (hsv.saturation > 0f) hue = hsv.hue
                            saturation = hsv.saturation
                            value = hsv.value
                        }
                    },
                    singleLine = true,
                    isError = ColorSpaces.parseHex(hexDisplay) == null,
                    label = { Text(stringResource(R.string.color_studio_hex)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        // Leaving the field abandons an unparseable draft rather than stranding the
                        // user on a red error they can only clear by moving a picker.
                        .onFocusChanged { if (!it.isFocused) hexDraft = null }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(argb) }) {
                Text(stringResource(R.string.color_studio_apply))
            }
        },
        dismissButton = {
            Row {
                if (onClear != null) {
                    TextButton(onClick = onClear) {
                        Text(stringResource(R.string.color_studio_reset))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}

/**
 * Hue ring with the saturation/value square inscribed in it.
 *
 * Which control a gesture drives is decided **once, at touch-down**, and held for the whole drag.
 * Deciding per-move would hand the gesture to the ring the moment a saturation drag crossed the
 * square's edge, which reads as the color randomly jumping hue mid-adjustment.
 *
 * All coordinate math lives in `ColorWheelMath` so it can be unit-tested; this composable only
 * draws and forwards positions.
 */
@Composable
private fun HueRingPicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onHueChange: (Float) -> Unit,
    onSaturationValueChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueLabel = stringResource(R.string.color_studio_wheel)
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = hueLabel }
            .pointerInput(Unit) {
                var ringGrabbed = false
                detectDragGestures(
                    onDragStart = { offset ->
                        val side = size.width.toFloat()
                        ringGrabbed = ColorWheelMath.isInRing(offset.x, offset.y, side)
                        if (ringGrabbed) {
                            onHueChange(ColorWheelMath.hueAt(offset.x, offset.y, side))
                        } else if (ColorWheelMath.isInSquare(offset.x, offset.y, side)) {
                            val (s, v) = ColorWheelMath.saturationValueAt(offset.x, offset.y, side)
                            onSaturationValueChange(s, v)
                        }
                    }
                ) { change, _ ->
                    val side = size.width.toFloat()
                    if (ringGrabbed) {
                        onHueChange(ColorWheelMath.hueAt(change.position.x, change.position.y, side))
                    } else {
                        val (s, v) = ColorWheelMath.saturationValueAt(change.position.x, change.position.y, side)
                        onSaturationValueChange(s, v)
                    }
                }
            }
            .pointerInput(Unit) {
                // detectDragGestures only fires past the touch slop, so a plain tap needs its own
                // handler or the wheel would ignore single taps entirely.
                detectTapGestures { offset ->
                    val side = size.width.toFloat()
                    if (ColorWheelMath.isInRing(offset.x, offset.y, side)) {
                        onHueChange(ColorWheelMath.hueAt(offset.x, offset.y, side))
                    } else if (ColorWheelMath.isInSquare(offset.x, offset.y, side)) {
                        val (s, v) = ColorWheelMath.saturationValueAt(offset.x, offset.y, side)
                        onSaturationValueChange(s, v)
                    }
                }
            }
    ) {
        val side = size.minDimension
        val center = Offset(side / 2f, side / 2f)
        val thickness = ColorWheelMath.ringThickness(side)

        drawCircle(
            brush = Brush.sweepGradient(SPECTRUM_COLORS, center),
            radius = ColorWheelMath.outerRadius(side) - thickness / 2f,
            center = center,
            style = Stroke(width = thickness)
        )

        val squareSide = ColorWheelMath.squareSide(side)
        val (left, top) = ColorWheelMath.squareTopLeft(side)
        val squareTopLeft = Offset(left, top)
        val squareSize = Size(squareSide, squareSide)
        val corner = CornerRadius(squareSide * 0.06f)

        // Saturation left→right over white, then value top→bottom into black. Two passes rather
        // than one because the vertical darkening has to apply to the tinted result, not beside it.
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White, Color(ColorSpaces.fromHsv(hue, 1f, 1f))),
                startX = left,
                endX = left + squareSide
            ),
            topLeft = squareTopLeft,
            size = squareSize,
            cornerRadius = corner
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = top,
                endY = top + squareSide
            ),
            topLeft = squareTopLeft,
            size = squareSize,
            cornerRadius = corner
        )

        val (ringX, ringY) = ColorWheelMath.ringHandleCenter(hue, side)
        drawHandle(Offset(ringX, ringY), thickness * 0.34f)

        val (sx, sy) = ColorWheelMath.squareHandleCenter(saturation, value, side)
        drawHandle(Offset(sx, sy), thickness * 0.30f)
    }
}

/**
 * White ring over a black ring: a single-colour handle disappears against part of any spectrum, and
 * the two strokes together stay visible on every hue and on both ends of the value axis.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandle(center: Offset, radius: Float) {
    drawCircle(color = Color.White, radius = radius, center = center, style = Stroke(width = radius * 0.45f))
    drawCircle(color = Color.Black, radius = radius * 1.28f, center = center, style = Stroke(width = radius * 0.16f))
}

/**
 * A draggable gradient bar — the 光谱条 for hue, and the same control reused for brightness and
 * saturation.
 *
 * Carries `progressBarRangeInfo` + `setProgress` so TalkBack can both read and change it; the raw
 * canvas above it can do neither.
 */
@Composable
private fun GradientBar(
    label: String,
    fraction: Float,
    brush: Brush,
    onFractionChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(FlowSpacing.xs)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .semantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction.coerceIn(0f, 1f), 0f..1f)
                    setProgress { target ->
                        onFractionChange(target.coerceIn(0f, 1f))
                        true
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onFractionChange(ColorWheelMath.valueAtBar(offset.x, size.width.toFloat()))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        onFractionChange(ColorWheelMath.valueAtBar(change.position.x, size.width.toFloat()))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(brush = brush, cornerRadius = radius)
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.18f),
                    cornerRadius = radius,
                    style = Stroke(width = 1.dp.toPx())
                )
                val x = fraction.coerceIn(0f, 1f) * size.width
                val clamped = x.coerceIn(size.height / 2f, size.width - size.height / 2f)
                drawHandle(Offset(clamped, size.height / 2f), size.height * 0.30f)
            }
        }
    }
}

/**
 * Live preview of what the seed will actually produce, in the theme's current darkness.
 *
 * Shows the generated roles rather than the raw seed, because the seed is *not* what the user ends
 * up looking at — `SeedColorScheme` re-tones it and may sweep the on-colors for contrast, so a bare
 * swatch would routinely mispredict the result. The measured body-text ratio is surfaced for the
 * same reason: it is the one number that decides whether the scheme is usable.
 */
@Composable
private fun SchemePreview(argb: Long, dark: Boolean) {
    val generated = remember(argb, dark) { SeedColorScheme.generate(argb, dark) }
    val ratio = remember(generated) { ColorContrast.ratio(generated.onSurface, generated.surface) }

    Column(verticalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(FlowRadius.md))
                    .background(Color(argb))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(FlowRadius.md))
            )
            Column {
                Text(
                    text = ColorSpaces.toHexString(argb),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    // The `%.1f` lives in the string resource, not in a bare `String.format` here:
                    // `getString` formats with the resource configuration's locale, so the decimal
                    // separator follows the in-app language (7,2 in de/fr/ru) instead of the device's.
                    text = stringResource(R.string.color_studio_contrast, ratio),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FlowRadius.sm))
        ) {
            listOf(
                generated.primary,
                generated.primaryContainer,
                generated.secondary,
                generated.tertiary,
                generated.surfaceVariant,
                generated.surface
            ).forEach { role ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(Color(role))
                )
            }
        }
    }
}
