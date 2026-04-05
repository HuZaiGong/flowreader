@file:OptIn(ExperimentalMaterial3Api::class)

package com.flowreader.app.ui.screens.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.domain.model.*
import com.flowreader.app.ui.theme.ReaderColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val contentScrollState = rememberScrollState()
    val chapterScrollPositions = remember { mutableStateMapOf<Int, Int>() }

    LaunchedEffect(uiState.currentChapterIndex) {
        val savedPosition = chapterScrollPositions[uiState.currentChapterIndex] ?: 0
        if (savedPosition > 0) {
            contentScrollState.scrollTo(savedPosition)
        }
    }

    LaunchedEffect(contentScrollState.value, uiState.currentChapterIndex) {
        if (uiState.currentChapterIndex >= 0) {
            chapterScrollPositions[uiState.currentChapterIndex] = contentScrollState.value
        }
    }

    LaunchedEffect(uiState.readingSettings.theme) {
        when (uiState.readingSettings.theme) {
            ReaderTheme.AMOLED -> activity?.window?.decorView?.setBackgroundColor(Color.Black.toArgb())
            else -> activity?.window?.decorView?.setBackgroundColor(Color.Transparent.toArgb())
        }
    }

    val backgroundColor = when (uiState.readingSettings.theme) {
        ReaderTheme.LIGHT -> ReaderColors.LightBackground
        ReaderTheme.DARK, ReaderTheme.SYSTEM, ReaderTheme.MORNING, ReaderTheme.NOON, ReaderTheme.EVENING, ReaderTheme.NIGHT -> ReaderColors.DarkBackground
        ReaderTheme.SEPIA -> ReaderColors.SepiaBackground
        ReaderTheme.PAPER -> ReaderColors.PaperBackground
        ReaderTheme.AMOLED -> ReaderColors.AmoledBackground
    }

    val textColor = when (uiState.readingSettings.theme) {
        ReaderTheme.LIGHT -> ReaderColors.LightText
        ReaderTheme.DARK, ReaderTheme.SYSTEM, ReaderTheme.MORNING, ReaderTheme.NOON, ReaderTheme.EVENING, ReaderTheme.NIGHT -> ReaderColors.DarkText
        ReaderTheme.SEPIA -> ReaderColors.SepiaText
        ReaderTheme.PAPER -> ReaderColors.PaperText
        ReaderTheme.AMOLED -> ReaderColors.AmoledText
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            uiState.currentChapter?.let { chapter ->
                val book = uiState.book
                if (book != null && book.format == BookFormat.PDF) {
                    PdfViewer(
                        filePath = book.filePath,
                        currentPage = uiState.currentChapterIndex,
                        textColor = textColor,
                        backgroundColor = backgroundColor,
                        onPageChange = { viewModel.goToChapter(it) }
                    )
                } else {
                    ReaderContent(
                        chapter = chapter,
                        settings = uiState.readingSettings,
                        textColor = textColor,
                        backgroundColor = backgroundColor,
                        scrollState = contentScrollState,
                        onTap = { offset, size ->
                            val tapZoneWidth = size.width * uiState.readingSettings.tapZoneRatio
                            val middle = size.width / 2
                            when {
                                offset.x < (middle - tapZoneWidth) -> viewModel.goToPreviousChapter()
                                offset.x > (middle + tapZoneWidth) -> viewModel.goToNextChapter()
                                else -> viewModel.toggleControls()
                            }
                        },
                        onPositionChanged = { viewModel.updatePosition(it) }
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ReaderControls(
                    bookTitle = uiState.book?.title ?: "",
                    chapterTitle = uiState.currentChapter?.title ?: "",
                    currentChapter = uiState.currentChapterIndex + 1,
                    totalChapters = uiState.chapters.size,
                    onBackClick = onBackClick,
                    onChapterClick = { viewModel.showChapterList(true) },
                    onSettingsClick = { viewModel.showSettings(true) },
                    onBookmarkClick = { viewModel.showBookmarks(true) },
                    onAddBookmark = {
                        val text = uiState.currentChapter?.content?.take(50) ?: ""
                        viewModel.addBookmark(text)
                    },
                    onProgressChange = { progress ->
                        val chapterIndex = (progress * uiState.chapters.size).toInt().coerceIn(0, uiState.chapters.size - 1)
                        viewModel.goToChapter(chapterIndex)
                    },
                    textColor = textColor,
                    backgroundColor = backgroundColor
                )
            }

            if (uiState.showChapterList) {
                ChapterListDialog(
                    chapters = uiState.chapters,
                    currentChapter = uiState.currentChapterIndex,
                    onChapterSelect = { viewModel.goToChapter(it) },
                    onDismiss = { viewModel.showChapterList(false) },
                    textColor = textColor,
                    backgroundColor = backgroundColor
                )
            }

            if (uiState.showSettings) {
                ReaderSettingsDialog(
                    settings = uiState.readingSettings,
                    onFontSizeChange = { viewModel.updateFontSize(it) },
                    onLineSpacingChange = { viewModel.updateLineSpacing(it) },
                    onThemeChange = { viewModel.updateReaderTheme(it) },
                    onPageModeChange = { viewModel.updatePageMode(it) },
                    onDismiss = { viewModel.showSettings(false) },
                    textColor = textColor,
                    backgroundColor = backgroundColor
                )
            }

            if (uiState.showBookmarks) {
                BookmarksDialog(
                    bookmarks = uiState.bookmarks,
                    onBookmarkSelect = { viewModel.goToBookmark(it) },
                    onBookmarkDelete = { viewModel.deleteBookmark(it.id) },
                    onDismiss = { viewModel.showBookmarks(false) },
                    textColor = textColor,
                    backgroundColor = backgroundColor
                )
            }
        }
    }
}

@Composable
private fun ReaderContent(
    chapter: Chapter,
    settings: ReadingSettings,
    textColor: Color,
    backgroundColor: Color,
    scrollState: ScrollState,
    onTap: (offset: androidx.compose.ui.geometry.Offset, size: androidx.compose.ui.geometry.Size) -> Unit,
    onPositionChanged: (Int) -> Unit
) {
    LaunchedEffect(scrollState.value) {
        onPositionChanged(scrollState.value)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        onTap(
                            offset,
                            androidx.compose.ui.geometry.Size(
                                size.width.toFloat(),
                                size.height.toFloat()
                            )
                        )
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = 20.dp,
                    vertical = 80.dp
                )
        ) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = (settings.fontSize + 4).sp,
                    lineHeight = (settings.fontSize * settings.lineSpacing + 8).sp
                ),
                color = textColor,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            val paragraphs = chapter.content.split("\n\n")
            paragraphs.forEach { paragraph ->
                if (paragraph.isNotBlank()) {
                    Text(
                        text = paragraph.trim(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = settings.fontSize.sp,
                            lineHeight = (settings.fontSize * settings.lineSpacing).sp,
                            textAlign = TextAlign.Justify
                        ),
                        color = textColor,
                        modifier = Modifier.padding(bottom = settings.paragraphSpacing.toInt().dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ReaderControls(
    bookTitle: String,
    chapterTitle: String,
    currentChapter: Int,
    totalChapters: Int,
    onBackClick: () -> Unit,
    onChapterClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onAddBookmark: () -> Unit,
    onProgressChange: (Float) -> Unit,
    textColor: Color,
    backgroundColor: Color
) {
    val progress = if (totalChapters > 0) currentChapter.toFloat() / totalChapters else 0f

    Box(modifier = Modifier.fillMaxSize()) {
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = textColor)
                }
            },
            actions = {
                IconButton(onClick = onChapterClick) {
                    Icon(Icons.Default.List, contentDescription = "目录", tint = textColor)
                }
                IconButton(onClick = onBookmarkClick) {
                    Icon(Icons.Default.Bookmark, contentDescription = "书签", tint = textColor)
                }
                IconButton(onClick = onAddBookmark) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = "添加书签", tint = textColor)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "设置", tint = textColor)
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$currentChapter / $totalChapters",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterListDialog(
    chapters: List<Chapter>,
    currentChapter: Int,
    onChapterSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    textColor: Color,
    backgroundColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("目录") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(chapters.size) { index ->
                    val isCurrentChapter = index == currentChapter
                    ListItem(
                        headlineContent = {
                            Text(
                                text = chapters[index].title,
                                color = if (isCurrentChapter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingContent = {
                            Text(
                                text = "${index + 1}",
                                color = if (isCurrentChapter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier.clickable { onChapterSelect(index) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsDialog(
    settings: ReadingSettings,
    onFontSizeChange: (Int) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onPageModeChange: (PageMode) -> Unit,
    onDismiss: () -> Unit,
    textColor: Color,
    backgroundColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("阅读设置") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("字体大小: ${settings.fontSize}sp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.fontSize.toFloat(),
                    onValueChange = { onFontSizeChange(it.toInt()) },
                    valueRange = 12f..32f,
                    steps = 19
                )

                Text("行间距: ${String.format("%.1f", settings.lineSpacing)}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.lineSpacing,
                    onValueChange = onLineSpacingChange,
                    valueRange = 1f..2.5f,
                    steps = 14
                )

                Text("阅读主题", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ReaderTheme.values().filter { it != ReaderTheme.SYSTEM }.forEach { theme ->
                        FilterChip(
                            selected = settings.theme == theme,
                            onClick = { onThemeChange(theme) },
                            label = {
                                Text(
                                    when (theme) {
                                        ReaderTheme.LIGHT -> "浅"
                                        ReaderTheme.DARK -> "深"
                                        ReaderTheme.SEPIA -> "护眼"
                                        ReaderTheme.PAPER -> "羊皮"
                                        ReaderTheme.AMOLED -> "夜间"
                                        else -> ""
                                    }
                                )
                            }
                        )
                    }
                }

                Text("翻页模式", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PageMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.pageMode == mode,
                            onClick = { onPageModeChange(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        PageMode.SLIDE -> "滑动"
                                        PageMode.SIMULATION -> "仿真"
                                        PageMode.NONE -> "无"
                                        PageMode.CURL -> "卷曲"
                                        PageMode.SLIDE_OVER -> "滑动覆盖"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun BookmarksDialog(
    bookmarks: List<Bookmark>,
    onBookmarkSelect: (Bookmark) -> Unit,
    onBookmarkDelete: (Bookmark) -> Unit,
    onDismiss: () -> Unit,
    textColor: Color,
    backgroundColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("书签") },
        text = {
            if (bookmarks.isEmpty()) {
                Text("暂无书签")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(bookmarks.size) { index ->
                        ListItem(
                            headlineContent = { Text(bookmarks[index].text) },
                            supportingContent = {
                                Text("第 ${bookmarks[index].chapterIndex + 1} 章")
                            },
                            trailingContent = {
                                IconButton(onClick = { onBookmarkDelete(bookmarks[index]) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除")
                                }
                            },
                            modifier = Modifier.clickable { onBookmarkSelect(bookmarks[index]) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun PdfViewer(
    filePath: String,
    currentPage: Int,
    textColor: Color,
    backgroundColor: Color,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(filePath, currentPage) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        pageCount = renderer.pageCount
                        if (currentPage in 0 until pageCount) {
                            renderer.openPage(currentPage).use { page ->
                                val bitmap = Bitmap.createBitmap(
                                    page.width * 2,
                                    page.height * 2,
                                    Bitmap.Config.ARGB_8888
                                )
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                pdfBitmap = bitmap
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val tapZoneWidth = size.width * 0.3f
                        val middle = size.width / 2
                        when {
                            offset.x < tapZoneWidth && currentPage > 0 -> onPageChange(currentPage - 1)
                            offset.x > size.width - tapZoneWidth && currentPage < pageCount - 1 -> onPageChange(currentPage + 1)
                            else -> { }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        pdfBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF第${currentPage + 1}页",
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = textColor)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(backgroundColor.copy(alpha = 0.8f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Slider(
                value = currentPage.toFloat(),
                onValueChange = { onPageChange(it.toInt()) },
                valueRange = 0f..(pageCount - 1).coerceAtLeast(0).toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = textColor,
                    activeTrackColor = textColor
                )
            )
            Text(
                text = "${currentPage + 1} / $pageCount",
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
    }
}