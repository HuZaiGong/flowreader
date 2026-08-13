package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.flowreader.app.core.designsystem.reader.ReaderPalette
import com.flowreader.app.core.designsystem.reader.background
import com.flowreader.app.core.util.ReaderBackgroundImage
import java.io.File

/**
 * Reader backdrop: the palette colour, optionally with a user-imported image behind a scrim.
 *
 * The scrim is not decoration. Reader readability is asserted against a flat background colour
 * (`ReaderPaletteContrastTest`), and an arbitrary photo has no such colour — so the image always
 * sits under the palette background at the configured scrim alpha, clamped by
 * `ReaderBackgroundImage`. See that object for the measured per-palette floors.
 *
 * [content] is drawn above both layers, so callers keep the same stacking they had when the
 * background was a plain `Modifier.background(...)`.
 */
@Composable
fun ReaderBackground(
    palette: ReaderPalette,
    backgroundImagePath: String?,
    scrimAlpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // A path can outlive the file it points at — the user may clear app data, or a restored backup
    // may reference a file that never came with it. Falling back to the flat colour keeps the
    // reader readable instead of showing an empty frame.
    val imageFile = backgroundImagePath
        ?.takeIf { ReaderBackgroundImage.isActive(it) }
        ?.let(::File)
        ?.takeIf { it.isFile && it.canRead() }

    Box(modifier = modifier.fillMaxSize().background(palette.background)) {
        if (imageFile != null) {
            AsyncImage(
                model = imageFile,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        palette.background.copy(
                            alpha = ReaderBackgroundImage.clampScrimAlpha(scrimAlpha)
                        )
                    )
            )
        }
        content()
    }
}
