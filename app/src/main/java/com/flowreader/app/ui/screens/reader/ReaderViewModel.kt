package com.flowreader.app.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.*
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.BookmarkRepository
import com.flowreader.app.domain.repository.ChapterRepository
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
    val showControls: Boolean = true,
    val showChapterList: Boolean = false,
    val showSettings: Boolean = false,
    val showBookmarks: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: 0L

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var progressSaveJob: Job? = null
    private val progressDebounceMs = 3000L

    init {
        loadBook()
        loadSettings()
    }

    override fun onCleared() {
        super.onCleared()
        progressSaveJob?.cancel()
        saveProgressImmediately()
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
            settingsRepository.appSettings.collect { settings ->
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
}
