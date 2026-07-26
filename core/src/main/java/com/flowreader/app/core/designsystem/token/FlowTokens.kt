package com.flowreader.app.core.designsystem.token

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The single spacing scale. Nothing in the UI is allowed to invent a value outside these six
 * steps — that is what kept every screen's rhythm inconsistent before v52.
 */
object FlowSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
}

/** Corner radius scale, aligned with the M3 expressive shape steps. */
object FlowRadius {
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 28.dp
}

/** Elevation scale. Reader surfaces stay flat; only floating containers lift. */
object FlowElevation {
    val none: Dp = 0.dp
    val raised: Dp = 2.dp
    val floating: Dp = 6.dp
    val overlay: Dp = 12.dp
}

/**
 * Motion scale. Four durations and three curves — anything longer than [emphasized] is a bug,
 * and nothing here may run while the reader body is scrolling.
 */
object FlowMotion {
    const val INSTANT_MS = 0
    const val QUICK_MS = 150
    const val STANDARD_MS = 250
    const val EMPHASIZED_MS = 400

    val standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
}

val FlowShapes = Shapes(
    extraSmall = RoundedCornerShape(FlowRadius.sm),
    small = RoundedCornerShape(FlowRadius.sm),
    medium = RoundedCornerShape(FlowRadius.md),
    large = RoundedCornerShape(FlowRadius.lg),
    extraLarge = RoundedCornerShape(FlowRadius.xl)
)
