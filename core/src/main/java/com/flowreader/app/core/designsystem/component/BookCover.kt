package com.flowreader.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.flowreader.app.core.designsystem.theme.FlowTheme
import com.flowreader.app.core.designsystem.token.FlowRadius
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.core.util.CoverArt
import com.flowreader.core.R
import java.io.File

/**
 * The one cover renderer.
 *
 * Callers pass a path, not a painter: resolving the file is Coil's job and happens off the main
 * thread, which is why the pre-v53 `File(path).exists()` check inside composition is gone. Books
 * without artwork fall back to a [CoverArt]-seeded gradient instead of a shared grey glyph.
 */
@Composable
fun BookCover(
    title: String,
    modifier: Modifier = Modifier,
    author: String = "",
    coverPath: String? = null,
    shape: Shape = RoundedCornerShape(FlowRadius.sm),
    showTitleOnFallback: Boolean = true
) {
    val description = stringResource(R.string.flow_cover_description, title)

    Box(modifier = modifier.clip(shape)) {
        if (coverPath.isNullOrBlank()) {
            GeneratedCover(title = title, author = author, showTitle = showTitleOnFallback, description = description)
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(coverPath))
                    .crossfade(true)
                    .memoryCacheKey(coverPath)
                    .diskCacheKey(coverPath)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = description,
                contentScale = ContentScale.Crop,
                loading = { SkeletonBox(modifier = Modifier.fillMaxSize(), shape = shape) },
                error = {
                    GeneratedCover(
                        title = title,
                        author = author,
                        showTitle = showTitleOnFallback,
                        description = description
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Deterministic gradient plus initials — same book, same cover, forever.
 *
 * `clearAndSetSemantics` collapses the initials and the title into one node so TalkBack announces
 * "《书名》封面" once instead of reading the title twice.
 */
@Composable
private fun GeneratedCover(title: String, author: String, showTitle: Boolean, description: String) {
    val seed = "$title|$author"
    val gradient = remember(seed) {
        GENERATED_COVER_GRADIENTS[CoverArt.paletteIndex(seed, GENERATED_COVER_GRADIENTS.size)]
    }
    val initials = remember(title) { CoverArt.initials(title) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = gradient,
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
            }
            .clearAndSetSemantics { contentDescription = description }
    ) {
        Text(
            text = initials,
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            maxLines = 1,
            modifier = Modifier.align(Alignment.Center)
        )
        if (showTitle) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = FlowSpacing.xs, vertical = FlowSpacing.sm)
            )
        }
    }
}

private val GENERATED_COVER_GRADIENTS: List<List<Color>> = listOf(
    listOf(Color(0xFF6750A4), Color(0xFF9A82DB)),
    listOf(Color(0xFF1B6C5A), Color(0xFF44A08D)),
    listOf(Color(0xFF8E3B46), Color(0xFFC96A76)),
    listOf(Color(0xFF1F4B8E), Color(0xFF4A83D6)),
    listOf(Color(0xFF8A5A20), Color(0xFFCB9250)),
    listOf(Color(0xFF3E4A5B), Color(0xFF75879E)),
    listOf(Color(0xFF5B2C6F), Color(0xFF9B59B6)),
    listOf(Color(0xFF16697A), Color(0xFF489FB5))
)

@FlowComponentPreviews
@Composable
private fun BookCoverPreview() {
    FlowTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(FlowSpacing.md),
            modifier = Modifier.padding(FlowSpacing.md)
        ) {
            BookCover(title = "心流", author = "米哈里", modifier = Modifier.size(70.dp, 100.dp))
            BookCover(title = "Deep Work", author = "Cal Newport", modifier = Modifier.size(70.dp, 100.dp))
            BookCover(title = "断舍离", modifier = Modifier.size(70.dp, 100.dp), showTitleOnFallback = false)
        }
    }
}
