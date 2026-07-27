package com.flowreader.app.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flowreader.app.core.designsystem.theme.FlowTheme
import com.flowreader.app.core.designsystem.token.FlowRadius
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.core.R

/**
 * A shimmering placeholder rectangle.
 *
 * The shimmer sweep is driven off an infinite transition rather than a per-frame loop, and it is
 * disabled inside `@Preview` / inspection so tooling renders a stable frame instead of a blur.
 */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(FlowRadius.sm)) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val animate = !LocalInspectionMode.current

    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                val width = size.width.coerceAtLeast(1f)
                val sweep = if (animate) progress * width * 2f - width else 0f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(base, highlight, base),
                        start = Offset(sweep, 0f),
                        end = Offset(sweep + width, 0f)
                    )
                )
            }
    )
}

/** A single skeleton text line. [widthFraction] mimics the ragged right edge of real copy. */
@Composable
fun SkeletonLine(modifier: Modifier = Modifier, widthFraction: Float = 1f, height: Dp = 14.dp) {
    SkeletonBox(
        modifier = modifier
            .fillMaxWidth(widthFraction.coerceIn(0.05f, 1f))
            .height(height),
        shape = RoundedCornerShape(height / 2)
    )
}

/**
 * The library cold-start placeholder. Mirrors the real shelf row geometry so the swap to content
 * does not jump — a centred spinner used to leave the first paint completely empty.
 */
@Composable
fun BookShelfSkeleton(modifier: Modifier = Modifier, itemCount: Int = 5) {
    val label = stringResource(R.string.flow_state_loading)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FlowSpacing.lg)
            .semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(FlowSpacing.lg)
    ) {
        SkeletonLine(widthFraction = 0.3f, height = 18.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.md)) {
            repeat(3) {
                SkeletonBox(
                    modifier = Modifier.size(width = 100.dp, height = 170.dp),
                    shape = RoundedCornerShape(FlowRadius.sm)
                )
            }
        }
        SkeletonLine(widthFraction = 0.3f, height = 18.dp)
        repeat(itemCount.coerceIn(1, 12)) { index ->
            BookRowSkeleton(titleWidthFraction = if (index % 2 == 0) 0.72f else 0.55f)
        }
    }
}

@Composable
private fun BookRowSkeleton(titleWidthFraction: Float) {
    Surface(
        shape = RoundedCornerShape(FlowRadius.md),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(FlowSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(modifier = Modifier.size(width = 70.dp, height = 100.dp))
            Spacer(modifier = Modifier.width(FlowSpacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FlowSpacing.sm)
            ) {
                SkeletonLine(widthFraction = titleWidthFraction, height = 16.dp)
                SkeletonLine(widthFraction = 0.35f, height = 12.dp)
                SkeletonLine(widthFraction = 0.5f, height = 10.dp)
            }
        }
    }
}

private const val SHIMMER_MS = 1200

@FlowComponentPreviews
@Composable
private fun BookShelfSkeletonPreview() {
    FlowTheme {
        Surface { BookShelfSkeleton(itemCount = 2) }
    }
}
