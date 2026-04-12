package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderControls(
    bookTitle: String,
    chapterTitle: String,
    currentChapter: Int,
    totalChapters: Int,
    onBackClick: () -> Unit,
    onChapterClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onAddBookmark: () -> Unit,
    onAnnotationClick: () -> Unit,
    onShareClick: () -> Unit,
    onProgressChange: (Float) -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    backgroundColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val progress = if (totalChapters > 0) currentChapter.toFloat() / totalChapters else 0f

    Box(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = bookTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor
                    )
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = textColor
                    )
                }
            },
            actions = {
                IconButton(onClick = onChapterClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = "目录",
                        tint = textColor
                    )
                }
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = "书签",
                        tint = textColor
                    )
                }
                IconButton(onClick = onAddBookmark) {
                    Icon(
                        Icons.Default.BookmarkAdd,
                        contentDescription = "添加书签",
                        tint = textColor
                    )
                }
                IconButton(onClick = onAnnotationClick) {
                    Icon(
                        Icons.Default.Highlight,
                        contentDescription = "高亮/笔记",
                        tint = textColor
                    )
                }
                IconButton(onClick = onShareClick) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "分享进度",
                        tint = textColor
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = textColor
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = backgroundColor.copy(alpha = 0.95f)
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = backgroundColor.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Slider(
                    value = progress,
                    onValueChange = onProgressChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = textColor,
                        activeTrackColor = textColor
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$currentChapter / $totalChapters",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .height(4.dp),
                        color = textColor,
                        trackColor = textColor.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "${String.format("%.1f", progress * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
