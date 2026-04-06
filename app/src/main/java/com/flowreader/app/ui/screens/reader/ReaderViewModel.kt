package com.flowreader.app.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.AnnotationColor
import com.flowreader.app.domain.model.AnnotationType
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.Bookmark
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.PageMode
import com.flowreader.app.domain.model.ReaderTheme
import com.flowreader.app.domain.model.ReadingSettings
import com.flowreader.app.domain.repository.AnnotationRepository
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.BookmarkRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.domain.repository.ReadingStatsRepository
import com.flowreader.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val currentChapter: Chapter? = null,
    val currentChapterIndex: Int = 0,
    val currentPosition: Int = 0,
    val readingSettings: ReadingSettings = ReadingSettings(),
    val bookmarks: List<Bookmark> = emptyList(),
    val annotations: List<Annotation> = emptyList(),
    val showControls: Boolean = true,
    val showChapterList: Boolean = false,
    val showSettings: Boolean = false,
    val showBookmarks: Boolean = false,
    val showAnnotations: Boolean = false,
    val isLoading: Boolean = true,
    val todayReadTime: Long = 0,
    val todayReadPages: Int = 0,
    val shareText: String? = null
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val annotationRepository: AnnotationRepository,
    private val settingsRepository: SettingsRepository,
    private val readingStatsRepository: ReadingStatsRepository
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: 0L

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var progressSaveJob: Job? = null
    private var statsUpdateJob: Job? = null
    private val progressDebounceMs = 3000L

    private var sessionStartTime: Long = 0
    private var sessionReadPages: Int = 0

    init {
        loadBook()
        loadSettings()
        loadTodayStats()
    }

    private fun loadTodayStats() {
        viewModelScope.launch {
            val readTime = readingStatsRepository.getTodayReadTime()
            val readPages = readingStatsRepository.getTodayReadPages()
            _uiState.update {
                it.copy(todayReadTime = readTime, todayReadPages = readPages)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressSaveJob?.cancel()
        saveProgressImmediately()
        saveReadingStats()
    }

    private fun saveReadingStats() {
        val sessionTime = (System.currentTimeMillis() - sessionStartTime) / 1000
        if (sessionTime > 0 && sessionReadPages > 0) {
            viewModelScope.launch {
                readingStatsRepository.updateTodayStats(
                    bookId = bookId,
                    readPages = sessionReadPages,
                    readTimeSeconds = sessionTime
                )
            }
        }
    }

    private fun saveProgressImmediately() {
        val state = _uiState.value
        if (state.chapters.isNotEmpty()) {
            val progress = (state.currentChapterIndex.toFloat() + 1) / state.chapters.size
            viewModelScope.launch {
                bookRepository.updateReadingProgress(
                    bookId, 
                    state.currentChapterIndex, 
                    state.currentPosition, 
                    progress
                )
            }
        }
    }

    private fun debouncedSaveProgress(chapterIndex: Int, position: Int, progress: Float) {
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            delay(progressDebounceMs)
            bookRepository.updateReadingProgress(bookId, chapterIndex, position, progress)
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.appSettings
                .first()
                .let { settings ->
                    _uiState.update {
                        it.copy(readingSettings = settings.defaultReadingSettings)
                    }
                }
        }
    }

    private fun loadBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val book = bookRepository.getBookById(bookId)
            val chapters = chapterRepository.getChaptersListByBookId(bookId)
            val bookmarks = bookmarkRepository.getBookmarksListByBookId(bookId)
            val annotations = annotationRepository.getAnnotationsListByBookId(bookId)

            if (book != null && chapters.isNotEmpty()) {
                val currentChapterIndex = book.currentChapter.coerceIn(0, chapters.size - 1)
                val currentChapter = chapters.getOrNull(currentChapterIndex)

                _uiState.update {
                    it.copy(
                        book = book,
                        chapters = chapters,
                        currentChapter = currentChapter,
                        currentChapterIndex = currentChapterIndex,
                        currentPosition = book.currentPosition,
                        bookmarks = bookmarks,
                        annotations = annotations,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun goToNextChapter() {
        val state = _uiState.value
        if (state.currentChapterIndex < state.chapters.size - 1) {
            goToChapter(state.currentChapterIndex + 1)
        }
    }

    fun goToPreviousChapter() {
        val state = _uiState.value
        if (state.currentChapterIndex > 0) {
            goToChapter(state.currentChapterIndex - 1)
        }
    }

    fun goToChapter(index: Int) {
        val state = _uiState.value
        if (index in state.chapters.indices) {
            val chapter = state.chapters[index]
            val progress = if (state.chapters.isNotEmpty()) {
                (index.toFloat() + 1) / state.chapters.size
            } else 0f

            viewModelScope.launch {
                bookRepository.updateReadingProgress(bookId, index, 0, progress)
            }

            _uiState.update {
                it.copy(
                    currentChapter = chapter,
                    currentChapterIndex = index,
                    currentPosition = 0,
                    showChapterList = false
                )
            }
        }
    }

    fun updatePosition(position: Int) {
        _uiState.update { it.copy(currentPosition = position) }

        val state = _uiState.value
        if (state.chapters.isNotEmpty()) {
            val progress = (state.currentChapterIndex.toFloat() + 1) / state.chapters.size
            debouncedSaveProgress(state.currentChapterIndex, position, progress)
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun showChapterList(show: Boolean) {
        _uiState.update { it.copy(showChapterList = show, showControls = !show) }
    }

    fun showSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show, showControls = !show) }
    }

    fun showBookmarks(show: Boolean) {
        _uiState.update { it.copy(showBookmarks = show, showControls = !show) }
    }

    fun updateFontSize(size: Int) {
        _uiState.update {
            it.copy(readingSettings = it.readingSettings.copy(fontSize = size))
        }
        viewModelScope.launch {
            settingsRepository.updateFontSize(size)
        }
    }

    fun updateLineSpacing(spacing: Float) {
        _uiState.update {
            it.copy(readingSettings = it.readingSettings.copy(lineSpacing = spacing))
        }
        viewModelScope.launch {
            settingsRepository.updateLineSpacing(spacing)
        }
    }

    fun updateReaderTheme(theme: ReaderTheme) {
        _uiState.update {
            it.copy(readingSettings = it.readingSettings.copy(theme = theme))
        }
        viewModelScope.launch {
            settingsRepository.updateReaderTheme(theme)
        }
    }

    fun updatePageMode(mode: PageMode) {
        _uiState.update {
            it.copy(readingSettings = it.readingSettings.copy(pageMode = mode))
        }
        viewModelScope.launch {
            settingsRepository.updatePageMode(mode)
        }
    }

    fun addBookmark(text: String) {
        val state = _uiState.value
        val bookmark = Bookmark(
            bookId = bookId,
            chapterIndex = state.currentChapterIndex,
            position = state.currentPosition,
            text = text
        )

        viewModelScope.launch {
            bookmarkRepository.insertBookmark(bookmark)
            val bookmarks = bookmarkRepository.getBookmarksListByBookId(bookId)
            _uiState.update { it.copy(bookmarks = bookmarks) }
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            bookmarkRepository.deleteBookmarkById(bookmarkId)
            val bookmarks = bookmarkRepository.getBookmarksListByBookId(bookId)
            _uiState.update { it.copy(bookmarks = bookmarks) }
        }
    }

    fun goToBookmark(bookmark: Bookmark) {
        goToChapter(bookmark.chapterIndex)
    }

    fun addAnnotation(
        selectedText: String,
        startPosition: Int,
        endPosition: Int,
        note: String = "",
        color: AnnotationColor = AnnotationColor.YELLOW,
        type: AnnotationType = AnnotationType.HIGHLIGHT
    ) {
        val state = _uiState.value
        val annotation = Annotation(
            bookId = bookId,
            chapterIndex = state.currentChapterIndex,
            startPosition = startPosition,
            endPosition = endPosition,
            selectedText = selectedText,
            note = note,
            color = color,
            type = type
        )

        viewModelScope.launch {
            annotationRepository.insertAnnotation(annotation)
            val annotations = annotationRepository.getAnnotationsListByBookId(bookId)
            _uiState.update { it.copy(annotations = annotations) }
        }
    }

    fun updateAnnotationNote(annotationId: Long, note: String) {
        viewModelScope.launch {
            val annotation = annotationRepository.getAnnotationById(annotationId)
            annotation?.let {
                annotationRepository.updateAnnotation(it.copy(note = note, modifiedTime = java.util.Date()))
                val annotations = annotationRepository.getAnnotationsListByBookId(bookId)
                _uiState.update { it.copy(annotations = annotations) }
            }
        }
    }

    fun deleteAnnotation(annotationId: Long) {
        viewModelScope.launch {
            annotationRepository.deleteAnnotationById(annotationId)
            val annotations = annotationRepository.getAnnotationsListByBookId(bookId)
            _uiState.update { it.copy(annotations = annotations) }
        }
    }

    fun showAnnotations(show: Boolean) {
        _uiState.update { it.copy(showAnnotations = show, showControls = !show) }
    }

    fun shareProgress() {
        val state = _uiState.value
        val book = state.book ?: return
        val progress = if (state.chapters.isNotEmpty()) {
            (state.currentChapterIndex + 1).toFloat() / state.chapters.size
        } else 0f

        val shareText = buildString {
            append("📖 《${book.title}》\n")
            append("📑 当前章节: ${state.currentChapter?.title ?: "未知"}\n")
            append("📊 阅读进度: ${String.format("%.1f", progress * 100)}%\n")
            append("🔖 第${state.currentChapterIndex + 1}章/共${state.chapters.size}章\n")
            append("\n来自心流阅读")
        }
        _uiState.update { it.copy(shareText = shareText) }
    }

    fun clearShareText() {
        _uiState.update { it.copy(shareText = null) }
    }
}
