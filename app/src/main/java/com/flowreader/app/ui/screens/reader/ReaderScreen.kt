@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.flowreader.app.ui.screens.reader

import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.model.Bookmark
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.PageMode
import com.flowreader.app.domain.model.ReaderTheme
import com.flowreader.app.domain.model.ReadingSettings
import com.flowreader.app.ui.screens.reader.components.*
import com.flowreader.app.ui.theme.ReaderColors
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val view = LocalView.current

    val contentScrollState = rememberScrollState()

    LaunchedEffect(uiState.scrollRequestVersion) {
        contentScrollState.scrollTo(uiState.currentPosition.coerceAtLeast(0))
    }

    val effectiveTheme = if (uiState.readingSettings.autoNightMode) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour >= 19 || hour < 7) ReaderTheme.DARK else ReaderTheme.LIGHT
    } else {
        uiState.readingSettings.theme
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

    val backgroundColor = when (effectiveTheme) {
        ReaderTheme.LIGHT -> ReaderColors.LightBackground
        ReaderTheme.DARK -> ReaderColors.DarkBackground
    }

    val textColor = when (effectiveTheme) {
        ReaderTheme.LIGHT -> ReaderColors.LightText
        ReaderTheme.DARK -> ReaderColors.DarkText
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val error = uiState.error
        if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error,
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { viewModel.retryLoadBook() }) {
                            Text("重试")
                        }
                        OutlinedButton(onClick = onBackClick) {
                            Text("返回")
                        }
                    }
                }
            }
        } else if (uiState.isLoading) {
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
                        annotations = uiState.annotations,
                        onTap = { offset, size ->
                            val tapZoneWidth = size.width * uiState.readingSettings.tapZoneRatio
                            val middle = size.width / 2
                            when {
                                offset.x < (middle - tapZoneWidth) -> viewModel.goToPreviousChapter()
                                offset.x > (middle + tapZoneWidth) -> viewModel.goToNextChapter()
                                else -> viewModel.toggleControls()
                            }
                        },
                        onPositionChanged = { viewModel.updatePosition(it) },
                        onTextSelected = { text, start, end, color ->
                            viewModel.addAnnotation(text, start, end, color)
                        },
                        onBookmarkRequested = { note, position ->
                            viewModel.addBookmark(note, position)
                        }
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
                    onSearchClick = { viewModel.showSearch(true) },
                    onAnnotationClick = { viewModel.showAnnotations(true) },
                    onShareClick = { viewModel.shareProgress() },
                    onBookmarkClick = { viewModel.showBookmarks(true) },
                    onTtsClick = { viewModel.toggleTts() },
                    onImmersiveClick = { viewModel.toggleImmersiveMode() },
                    isTtsPlaying = uiState.isTtsPlaying,
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
                ReaderSettingsDialog(
                    settings = uiState.readingSettings,
                    onFontSizeChange = { viewModel.updateFontSize(it) },
                    onLineSpacingChange = { viewModel.updateLineSpacing(it) },
                    onFontFamilyChange = { viewModel.updateFontFamily(it) },
                    onThemeChange = { viewModel.updateReaderTheme(it) },
                    onPageModeChange = { viewModel.updatePageMode(it) },
                    onEyeProtectionIntervalChange = { viewModel.updateEyeProtectionInterval(it) },
                    onAutoNightModeChange = { viewModel.updateAutoNightMode(it) },
                    onDismiss = { viewModel.showSettings(false) },
                    textColor = textColor,
                    backgroundColor = backgroundColor
                )
            }

            if (uiState.showAnnotations) {
                AnnotationsDialog(
                    annotations = uiState.annotations.filter { it.chapterIndex == uiState.currentChapterIndex },
                    onAnnotationSelect = { annotation -> viewModel.goToChapter(annotation.chapterIndex) },
                    onAnnotationDelete = { annotation -> viewModel.deleteAnnotation(annotation) },
                    onAnnotationNoteUpdate = { id, note -> viewModel.updateAnnotationNote(id, note) },
                    onDismiss = { viewModel.showAnnotations(false) },
                    textColor = textColor,
                    backgroundColor = backgroundColor
                )
            }

            if (uiState.showBookmarks) {
                BookmarksDialog(
                    bookmarks = uiState.bookmarks,
                    currentChapterIndex = uiState.currentChapterIndex,
                    onBookmarkSelect = { viewModel.goToBookmark(it) },
                    onBookmarkDelete = { viewModel.deleteBookmark(it) },
                    onDismiss = { viewModel.showBookmarks(false) },
                    textColor = textColor,
                    backgroundColor = backgroundColor
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
}
