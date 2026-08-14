package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.flowreader.app.R
import com.flowreader.app.core.designsystem.reader.ReaderPalette
import com.flowreader.app.core.designsystem.reader.background
import com.flowreader.app.core.designsystem.reader.secondaryText
import com.flowreader.app.core.designsystem.reader.text
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.core.util.FlowFormatters

/**
 * The reader control layer.
 *
 * v52 changes: the bar respects window insets (it used to sit under the status bar in edge-to-edge
 * mode), the eight equally-weighted icons collapse to four plus an overflow, and the slider
 * reports true character-weighted progress instead of `chapterIndex / chapterCount`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderControls(
    bookTitle: String,
    chapterTitle: String,
    currentChapter: Int,
    totalChapters: Int,
    progressProvider: () -> Float,
    isTtsPlaying: Boolean,
    palette: ReaderPalette,
    chapterTitleAt: (Float) -> String,
    onBackClick: () -> Unit,
    onChapterClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onTtsClick: () -> Unit,
    onAnnotationClick: () -> Unit,
    onSearchClick: () -> Unit,
    onImmersiveClick: () -> Unit,
    onShareClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProgressCommit: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    // Read through the provider so scroll updates repaint the bar without touching the body.
    val sliderValue = if (isDragging) dragValue else progressProvider().coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = bookTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = palette.text
                    )
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = palette.secondaryText
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = palette.text
                    )
                }
            },
            actions = {
                IconButton(onClick = onChapterClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.reader_chapters_title),
                        tint = palette.text
                    )
                }
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = stringResource(R.string.reader_bookmarks_title),
                        tint = palette.text
                    )
                }
                IconButton(onClick = onTtsClick) {
                    Icon(
                        imageVector = if (isTtsPlaying) Icons.Default.PauseCircle else Icons.Default.RecordVoiceOver,
                        contentDescription = if (isTtsPlaying) stringResource(R.string.reader_tts_pause) else stringResource(R.string.reader_tts_play),
                        tint = palette.text
                    )
                }
                Box {
                    IconButton(onClick = { overflowExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more), tint = palette.text)
                    }
                    DropdownMenu(expanded = overflowExpanded, onDismissRequest = { overflowExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reader_menu_settings)) },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                overflowExpanded = false
                                onSettingsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reader_menu_annotations)) },
                            leadingIcon = { Icon(Icons.Default.Highlight, contentDescription = null) },
                            onClick = {
                                overflowExpanded = false
                                onAnnotationClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reader_menu_search)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            onClick = {
                                overflowExpanded = false
                                onSearchClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reader_menu_focus)) },
                            leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null) },
                            onClick = {
                                overflowExpanded = false
                                onImmersiveClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reader_menu_share)) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                overflowExpanded = false
                                onShareClick()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.background.copy(alpha = 0.95f)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars),
            color = palette.background.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FlowSpacing.lg, vertical = FlowSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isDragging) {
                    Text(
                        text = chapterTitleAt(dragValue),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        isDragging = true
                        dragValue = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onProgressCommit(dragValue)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = palette.text,
                        activeTrackColor = palette.text,
                        inactiveTrackColor = palette.secondaryText.copy(alpha = 0.3f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "第 $currentChapter / $totalChapters 章",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText
                    )
                    Text(
                        text = FlowFormatters.percentPrecise(sliderValue),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText
                    )
                }
            }
        }
    }
}
