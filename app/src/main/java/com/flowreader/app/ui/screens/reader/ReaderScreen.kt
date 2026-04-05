@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.flowreader.app.ui.screens.reader

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.domain.model.*
import com.flowreader.app.ui.theme.ReaderColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

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

    var showBrightnessSlider by remember { mutableStateOf(false) }
    var controlsAutoHideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun resetAutoHide() {
        controlsAutoHideJob?.cancel()
        controlsAutoHideJob = viewModel.scope.launch {
            val delayMs = uiState.readingSettings.controlsHideDelaySeconds * 1000L
            delay(delayMs)
            if (uiState.showControls) {
                viewModel.toggleControls()
            }
        }
    }

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
            ReaderTheme.AMOLED, ReaderTheme.NIGHT -> activity?.window?.decorView?.setBackgroundColor(Color.Black.toArgb())
            else -> activity?.window?.decorView?.setBackgroundColor(Color.Transparent.toArgb())
        }
    }

    val backgroundColor = remember(uiState.readingSettings.theme) {
        getReaderBackgroundColor(uiState.readingSettings.theme)
    }

    val textColor = remember(uiState.readingSettings.theme) {
        getReaderTextColor(uiState.readingSettings.theme)
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
                ReaderContent(
                    chapter = chapter,
                    settings = uiState.readingSettings,
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    scrollState = contentScrollState,
                    onTap = { offset, size ->
                        val tapZone = uiState.readingSettings.tapZoneConfig
                        val leftBound = size.width * tapZone.leftRatio
                        val rightBound = size.width * (1 - tapZone.rightRatio)
                        
                        when {
                            offset.x < leftBound -> handleGestureAction(viewModel, tapZone.leftAction)
                            offset.x > rightBound -> handleGestureAction(viewModel, tapZone.rightAction)
                            else -> handleGestureAction(viewModel, tapZone.middleAction)
                        }
                        resetAutoHide()
                    },
                    onDoubleFingerSwipe = { isUp ->
                        val useGestures = uiState.readingSettings.doubleFingerGesture
                        val action = if (isUp) {
                            if (useGestures) GestureAction.NEXT_PAGE else GestureAction.PREVIOUS_PAGE
                        } else {
                            if (useGestures) GestureAction.PREVIOUS_PAGE else GestureAction.NEXT_PAGE
                        }
                        handleGestureAction(viewModel, action)
                        resetAutoHide()
                    },
                    onPositionChanged = { viewModel.updatePosition(it) }
                )
            }

            AnimatedVisibility(
                visible = uiState.showControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
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
                    onBrightnessClick = { showBrightnessSlider = !showBrightnessSlider },
                    textColor = textColor,
                    backgroundColor = backgroundColor
                )
            }

            AnimatedVisibility(
                visible = showBrightnessSlider,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                BrightnessSlider(
                    onBrightnessChange = { brightness ->
                        val layoutParams = activity?.window?.attributes
                        layoutParams?.screenBrightness = brightness
                        activity?.window?.attributes = layoutParams
                    },
                    onDismiss = { showBrightnessSlider = false },
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
                    onAutoTimeThemeChange = { viewModel.updateAutoTimeTheme(it) },
                    onParagraphSpacingChange = { viewModel.updateParagraphSpacing(it) },
                    onFirstLineIndentChange = { viewModel.updateFirstLineIndent(it) },
                    onJustifyTextChange = { viewModel.updateJustifyText(it) },
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

private fun handleGestureAction(viewModel: ReaderViewModel, action: GestureAction) {
    when (action) {
        GestureAction.PREVIOUS_PAGE -> viewModel.goToPreviousChapter()
        GestureAction.NEXT_PAGE -> viewModel.goToNextChapter()
        GestureAction.SHOW_MENU -> viewModel.toggleControls()
        GestureAction.SHOW_SETTINGS -> viewModel.showSettings(true)
        GestureAction.SHOW_BOOKMARKS -> viewModel.showBookmarks(true)
    }
}

private fun getReaderBackgroundColor(theme: ReaderTheme): Color {
    return when (theme) {
        ReaderTheme.LIGHT, ReaderTheme.MORNING, ReaderTheme.AFTERNOON -> ReaderColors.LightBackground
        ReaderTheme.DARK, ReaderTheme.SYSTEM, ReaderTheme.NIGHT -> ReaderColors.DarkBackground
        ReaderTheme.SEPIA, ReaderTheme.EVENING -> ReaderColors.SepiaBackground
        ReaderTheme.PAPER -> ReaderColors.PaperBackground
        ReaderTheme.AMOLED -> ReaderColors.AmoledBackground
        ReaderTheme.EINK_PURE -> ReaderColors.EinkPureBackground
        ReaderTheme.EINK_GRAY -> ReaderColors.EinkGrayBackground
        ReaderTheme.EINK_WARM -> ReaderColors.EinkWarmBackground
    }
}

private fun getReaderTextColor(theme: ReaderTheme): Color {
    return when (theme) {
        ReaderTheme.LIGHT, ReaderTheme.MORNING, ReaderTheme.AFTERNOON -> ReaderColors.LightText
        ReaderTheme.DARK, ReaderTheme.SYSTEM, ReaderTheme.NIGHT -> ReaderColors.DarkText
        ReaderTheme.SEPIA, ReaderTheme.EVENING -> ReaderColors.SepiaText
        ReaderTheme.PAPER -> ReaderColors.PaperText
        ReaderTheme.AMOLED -> ReaderColors.AmoledText
        ReaderTheme.EINK_PURE -> ReaderColors.EinkPureText
        ReaderTheme.EINK_GRAY -> ReaderColors.EinkGrayText
        ReaderTheme.EINK_WARM -> ReaderColors.EinkWarmText
    }
}

@Composable
private fun BrightnessSlider(
    onBrightnessChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    textColor: Color,
    backgroundColor: Color
) {
    var sliderValue by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(sliderValue) {
        onBrightnessChange(sliderValue)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "亮度调节",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.BrightnessLow,
                    contentDescription = null,
                    tint = textColor
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.BrightnessHigh,
                    contentDescription = null,
                    tint = textColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("完成", color = textColor)
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
    onDoubleFingerSwipe: (isUp: Boolean) -> Unit,
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
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, _, _ ->
                    if (abs(pan.y) > 50) {
                        onDoubleFingerSwipe(pan.y < 0)
                    }
                }
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
                    lineHeight = (settings.fontSize * settings.lineSpacing + 8).sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                ),
                color = textColor,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            val paragraphs = chapter.content.split("\n\n")
            paragraphs.forEach { paragraph ->
                if (paragraph.isNotBlank()) {
                    val displayText = if (settings.firstLineIndent) {
                        "    ${paragraph.trim()}"
                    } else {
                        paragraph.trim()
                    }
                    
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = settings.fontSize.sp,
                            lineHeight = (settings.fontSize * settings.lineSpacing).sp,
                            textAlign = if (settings.justifyText) TextAlign.Justify else TextAlign.Start,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        ),
                        color = textColor,
                        modifier = Modifier.padding(bottom = (settings.fontSize * settings.paragraphSpacing).toInt().dp)
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
    onBrightnessClick: () -> Unit,
    textColor: Color,
    backgroundColor: Color
) {
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = textColor)
                }
            },
            actions = {
                IconButton(onClick = onBrightnessClick) {
                    Icon(Icons.Default.Brightness6, contentDescription = "亮度", tint = textColor)
                }
                IconButton(onClick = onChapterClick) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "目录", tint = textColor)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currentChapter / $totalChapters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f)
                )
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
    onAutoTimeThemeChange: (Boolean) -> Unit,
    onParagraphSpacingChange: (ParagraphSpacing) -> Unit,
    onFirstLineIndentChange: (Boolean) -> Unit,
    onJustifyTextChange: (Boolean) -> Unit,
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

                Text("段落间距", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ParagraphSpacing.values().take(4).forEach { preset ->
                        FilterChip(
                            selected = settings.paragraphSpacingPreset == preset,
                            onClick = { onParagraphSpacingChange(preset) },
                            label = { Text(preset.displayName, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("首行缩进", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.firstLineIndent,
                        onCheckedChange = onFirstLineIndentChange
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("两端对齐", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.justifyText,
                        onCheckedChange = onJustifyTextChange
                    )
                }

                Text("阅读主题", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        ReaderTheme.LIGHT to "浅",
                        ReaderTheme.DARK to "深",
                        ReaderTheme.SEPIA to "护眼",
                        ReaderTheme.PAPER to "羊皮",
                        ReaderTheme.AMOLED to "夜间"
                    ).forEach { (theme, label) ->
                        FilterChip(
                            selected = settings.theme == theme,
                            onClick = { onThemeChange(theme) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("时光模式 (自动)", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.autoTimeTheme,
                        onCheckedChange = onAutoTimeThemeChange
                    )
                }

                Text("翻页模式", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PageMode.values().forEach { mode ->
                        FilterChip(
                            selected = settings.pageMode == mode,
                            onClick = { onPageModeChange(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        PageMode.SLIDE -> "滑动"
                                        PageMode.SIMULATION -> "仿真"
                                        PageMode.NONE -> "无"
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
