@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.flowreader.app.ui.screens.reader

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.WindowManager
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
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.AnnotationColor
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.model.Bookmark
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.PageMode
import com.flowreader.app.domain.model.ReaderTheme
import com.flowreader.app.domain.model.ReadingSettings
import com.flowreader.app.ui.screens.reader.components.*
import com.flowreader.app.ui.theme.ReaderColors
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
                        onPositionChanged = { viewModel.updatePosition(it) },
                        onTextSelected = { text, start, end ->
                            viewModel.addAnnotation(text, start, end)
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
                    onBookmarkClick = { viewModel.showBookmarks(true) },
                    onAddBookmark = {
                        val text = uiState.currentChapter?.content?.take(50) ?: ""
                        viewModel.addBookmark(text)
                    },
                    onAnnotationClick = { viewModel.showAnnotations(true) },
                    onShareClick = { viewModel.shareProgress() },
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
                    onTtsPlay = { viewModel.playTts() },
                    onTtsStop = { viewModel.stopTts() },
                    isTtsPlaying = viewModel.ttsState.value == com.flowreader.app.util.TtsState.PLAYING,
                    onDismiss = { viewModel.showSettings(false) },
                    textColor = textColor,
                    backgroundColor = backgroundColor
                )
            }

            if (uiState.showBookmarks) {
                BookmarksDialog(
                    bookmarks = uiState.bookmarks,
                    onBookmarkSelect = { chapterIndex -> viewModel.goToChapter(chapterIndex) },
                    onBookmarkDelete = { bookmark -> viewModel.deleteBookmark(bookmark) },
                    onDismiss = { viewModel.showBookmarks(false) },
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
