package com.flowreader.app.ui.screens.reader

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.core.designsystem.component.FlowStateHost
import com.flowreader.app.core.designsystem.reader.ReaderPalettes
import com.flowreader.app.core.designsystem.reader.background
import com.flowreader.app.core.designsystem.reader.rememberReaderFontFamily
import com.flowreader.app.core.designsystem.reader.text
import com.flowreader.app.core.designsystem.token.FlowMotion
import com.flowreader.app.core.util.ReaderBehavior
import com.flowreader.app.core.util.ReadingProgress
import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.model.GestureAction
import com.flowreader.app.domain.model.PageMode
import com.flowreader.app.ui.screens.reader.components.AnnotationsDialog
import com.flowreader.app.ui.screens.reader.components.BookmarksDialog
import com.flowreader.app.ui.screens.reader.components.ChapterListDialog
import com.flowreader.app.ui.screens.reader.components.ComicReader
import com.flowreader.app.ui.screens.reader.components.PdfViewer
import com.flowreader.app.ui.screens.reader.components.ReaderContent
import com.flowreader.app.ui.screens.reader.components.ReaderControls
import com.flowreader.app.ui.screens.reader.components.ReaderSettingsSheet
import com.flowreader.app.ui.screens.reader.components.SearchDialog
import com.flowreader.app.ui.screens.reader.components.ShareProgressDialog
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val view = LocalView.current
    val settings = uiState.readingSettings

    val contentScrollState = rememberScrollState()

    LaunchedEffect(uiState.scrollRequestVersion) {
        val target = uiState.currentPosition.coerceAtLeast(0)
        if (settings.pageMode == PageMode.SLIDE) {
            contentScrollState.animateScrollTo(target)
        } else {
            contentScrollState.scrollTo(target)
        }
    }

    // Auto night mode is re-evaluated every minute, so 19:00 actually arrives while reading.
    // Before v52 the hour was sampled once during composition and never again.
    val isNightWindow by produceState(
        initialValue = ReaderBehavior.isNightHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
        key1 = settings.autoNightMode
    ) {
        while (settings.autoNightMode) {
            value = ReaderBehavior.isNightHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
            delay(60_000L)
        }
    }

    val palette = remember(settings.palette, settings.nightPalette, settings.autoNightMode, isNightWindow) {
        val id = if (settings.autoNightMode && isNightWindow) settings.nightPalette else settings.palette
        ReaderPalettes.of(id)
    }

    DisposableEffect(activity, view, uiState.isImmersiveMode) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            if (uiState.isImmersiveMode) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity?.window?.let { WindowCompat.getInsetsController(it, view).show(WindowInsetsCompat.Type.systemBars()) }
        }
    }

    fun dispatchGesture(action: GestureAction) {
        when (action) {
            GestureAction.PREVIOUS_PAGE -> viewModel.goToPreviousChapter()
            GestureAction.NEXT_PAGE -> viewModel.goToNextChapter()
            GestureAction.TOGGLE_CONTROLS -> viewModel.toggleControls()
            GestureAction.SHOW_SETTINGS -> viewModel.showSettings(true)
            GestureAction.SHOW_BOOKMARKS -> viewModel.showBookmarks(true)
            GestureAction.SHOW_TOC -> viewModel.showChapterList(true)
            GestureAction.ADD_BOOKMARK -> viewModel.addBookmark("第 ${uiState.currentChapterIndex + 1} 章书签")
            GestureAction.NONE -> Unit
        }
    }

    val fontFamily = rememberReaderFontFamily(settings)

    // Derived so scroll updates repaint only the control layer, never the body.
    val chapterFraction by remember {
        derivedStateOf { ReadingProgress.scrollFraction(contentScrollState.value, contentScrollState.maxValue) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        FlowStateHost(
            isLoading = uiState.isLoading,
            isEmpty = uiState.currentChapter == null && uiState.error == null && !uiState.isLoading,
            error = uiState.error,
            emptyTitle = "本章暂无内容",
            onRetry = { viewModel.retryLoadBook() },
            onDismissError = onBackClick,
            contentColor = palette.text
        ) {
            val chapter = uiState.currentChapter
            val book = uiState.book
            if (chapter != null) {
                if (book != null && book.format == BookFormat.COMIC) {
                    ComicReader(
                        chapters = uiState.chapters,
                        currentChapterIndex = uiState.currentChapterIndex,
                        settings = settings,
                        palette = palette,
                        onTap = { offset, size ->
                            dispatchGesture(ReaderBehavior.tapAction(offset.x, size.width, settings))
                        },
                        onHorizontalDrag = { drag -> dispatchGesture(ReaderBehavior.swipeAction(drag, settings)) },
                        onPageVisible = { index -> viewModel.setCurrentComicPage(index) },
                        onPositionChanged = { position ->
                            val total = uiState.chapters.size
                            val fraction = if (total > 1) position.toFloat() / (total - 1) else 0f
                            viewModel.updatePosition(position, fraction)
                        }
                    )
                } else if (book != null && book.format == BookFormat.PDF) {
                    PdfViewer(
                        filePath = book.filePath,
                        currentPage = uiState.currentChapterIndex,
                        textColor = palette.text,
                        backgroundColor = palette.background,
                        onPageChange = { viewModel.goToChapter(it) }
                    )
                } else {
                    ReaderContent(
                        chapter = chapter,
                        settings = settings,
                        palette = palette,
                        fontFamily = fontFamily,
                        scrollState = contentScrollState,
                        annotations = uiState.annotations,
                        onTap = { offset, size ->
                            dispatchGesture(ReaderBehavior.tapAction(offset.x, size.width, settings))
                        },
                        onDoubleTap = { dispatchGesture(settings.gestureSettings.doubleTapAction) },
                        onHorizontalDrag = { drag ->
                            dispatchGesture(ReaderBehavior.swipeAction(drag, settings))
                        },
                        onHighlightSelection = { text, start, end ->
                            viewModel.addAnnotation(text, start, end)
                        },
                        onBookmarkSelection = { text, start, end ->
                            viewModel.addBookmark(text.ifBlank { "选中文本书签" }, start)
                        },
                        onPositionChanged = { position ->
                            viewModel.updatePosition(position, chapterFraction)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.showControls && uiState.error == null && !uiState.isLoading,
            enter = fadeIn(tween(FlowMotion.QUICK_MS)) + slideInVertically(tween(FlowMotion.QUICK_MS)) { -it / 4 },
            exit = fadeOut(tween(FlowMotion.QUICK_MS)) + slideOutVertically(tween(FlowMotion.QUICK_MS)) { -it / 4 }
        ) {
            ReaderControls(
                bookTitle = uiState.book?.title ?: "",
                chapterTitle = uiState.currentChapter?.title ?: "",
                currentChapter = uiState.currentChapterIndex + 1,
                totalChapters = uiState.chapters.size,
                progressProvider = {
                    ReadingProgress.fraction(uiState.currentChapterIndex, chapterFraction, uiState.chapters.size)
                },
                isTtsPlaying = uiState.isTtsPlaying,
                palette = palette,
                chapterTitleAt = { fraction ->
                    val index = ReadingProgress.chapterAt(fraction, uiState.chapters.size)
                    uiState.chapters.getOrNull(index)?.title ?: "第 ${index + 1} 章"
                },
                onBackClick = onBackClick,
                onChapterClick = { viewModel.showChapterList(true) },
                onBookmarkClick = { viewModel.showBookmarks(true) },
                onTtsClick = { viewModel.toggleTts() },
                onAnnotationClick = { viewModel.showAnnotations(true) },
                onSearchClick = { viewModel.showSearch(true) },
                onImmersiveClick = { viewModel.toggleImmersiveMode() },
                onShareClick = { viewModel.shareProgress() },
                onSettingsClick = { viewModel.showSettings(true) },
                onProgressCommit = { viewModel.goToProgress(it) }
            )
        }

        if (uiState.showChapterList) {
            ChapterListDialog(
                chapters = uiState.chapters,
                currentChapter = uiState.currentChapterIndex,
                onChapterSelect = { viewModel.goToChapter(it) },
                onDismiss = { viewModel.showChapterList(false) },
                textColor = palette.text,
                backgroundColor = palette.background
            )
        }

        if (uiState.showSearch) {
            SearchDialog(
                query = uiState.searchQuery,
                results = uiState.searchResults,
                isSearching = uiState.isSearching,
                hasSearched = uiState.hasSearched,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { viewModel.searchInBook() },
                onResultClick = { viewModel.goToSearchResult(it) },
                onDismiss = { viewModel.showSearch(false) }
            )
        }

        if (uiState.showSettings) {
            ReaderSettingsSheet(
                settings = settings,
                onSettingsChange = { viewModel.updateReadingSettings(it) },
                onDismiss = { viewModel.showSettings(false) }
            )
        }

        if (uiState.showAnnotations) {
            AnnotationsDialog(
                annotations = uiState.annotations.filter { it.chapterIndex == uiState.currentChapterIndex },
                onAnnotationSelect = { annotation -> viewModel.goToChapter(annotation.chapterIndex) },
                onAnnotationDelete = { annotation -> viewModel.deleteAnnotation(annotation) },
                onAnnotationNoteUpdate = { id, note -> viewModel.updateAnnotationNote(id, note) },
                onDismiss = { viewModel.showAnnotations(false) },
                textColor = palette.text,
                backgroundColor = palette.background
            )
        }

        if (uiState.showBookmarks) {
            BookmarksDialog(
                bookmarks = uiState.bookmarks,
                currentChapterIndex = uiState.currentChapterIndex,
                onBookmarkSelect = { viewModel.goToBookmark(it) },
                onBookmarkDelete = { viewModel.deleteBookmark(it) },
                onDismiss = { viewModel.showBookmarks(false) },
                textColor = palette.text,
                backgroundColor = palette.background
            )
        }

        uiState.shareText?.let { shareText ->
            ShareProgressDialog(
                shareText = shareText,
                onDismiss = { viewModel.clearShareText() },
                onShare = { intent ->
                    context.startActivity(intent)
                    viewModel.clearShareText()
                }
            )
        }
    }
}
