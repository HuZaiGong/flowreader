package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.flowreader.app.core.designsystem.reader.ReaderPalette
import com.flowreader.app.core.designsystem.reader.background
import com.flowreader.app.core.designsystem.reader.text
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.PageMode
import com.flowreader.app.domain.model.ReadingSettings
import java.io.File

@Composable
fun ComicReader(
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    settings: ReadingSettings,
    palette: ReaderPalette,
    scrollState: ScrollState,
    onTap: (Offset, Size) -> Unit,
    onHorizontalDrag: (Float) -> Unit,
    onPageVisible: (Int) -> Unit,
    onPositionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalStitch = settings.pageMode == PageMode.NONE

    LaunchedEffect(scrollState.value, verticalStitch) {
        if (verticalStitch) onPositionChanged(scrollState.value)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .pointerInput(settings.gestureSettings, settings.tapZoneRatio) {
                detectTapGestures(
                    onTap = { offset -> onTap(offset, Size(size.width.toFloat(), size.height.toFloat())) }
                )
            }
            .pointerInput(settings.gestureSettings, verticalStitch) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { onHorizontalDrag(total) },
                    onHorizontalDrag = { _, delta -> total += delta }
                )
            }
    ) {
        if (verticalStitch) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                chapters.forEachIndexed { index, chapter ->
                    ComicPage(
                        path = chapter.comicImagePath(),
                        tint = palette.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                    if (index == currentChapterIndex) {
                        LaunchedEffect(index) { onPageVisible(index) }
                    }
                }
            }
        } else {
            val chapter = chapters.getOrNull(currentChapterIndex)
            LaunchedEffect(currentChapterIndex) {
                onPageVisible(currentChapterIndex)
                onPositionChanged(0)
            }
            ComicPage(
                path = chapter?.comicImagePath().orEmpty(),
                tint = palette.text,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ComicPage(path: String, tint: Color, modifier: Modifier = Modifier) {
    val file = remember(path) { File(path) }
    if (file.isFile) {
        AsyncImage(
            model = file,
            contentDescription = null,
            modifier = modifier.padding(vertical = 1.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = null,
                tint = tint.copy(alpha = 0.35f)
            )
        }
    }
}

private fun Chapter.comicImagePath(): String =
    content.removePrefix("[COMIC:").removeSuffix("]").takeIf { content.startsWith("[COMIC:") } ?: content
