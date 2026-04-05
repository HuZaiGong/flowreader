package com.flowreader.app.ui.screens.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.Bookmark
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.ReaderTheme
import com.flowreader.app.domain.repository.BookmarkRepository
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailUiState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = true,
    val appTheme: ReaderTheme = ReaderTheme.SYSTEM
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: 0L

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        loadBookDetails()
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.appSettings.collect { settings ->
                _uiState.update { it.copy(appTheme = settings.theme) }
            }
        }
    }

    private fun loadBookDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val book = bookRepository.getBookById(bookId)
            if (book != null) {
                combine(
                    chapterRepository.getChaptersByBookId(bookId),
                    bookmarkRepository.getBookmarksByBookId(bookId)
                ) { chapters, bookmarks ->
                    Pair(chapters, bookmarks)
                }.collect { (chapters, bookmarks) ->
                    _uiState.update {
                        it.copy(
                            book = book,
                            chapters = chapters,
                            bookmarks = bookmarks,
                            isLoading = false
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            bookmarkRepository.deleteBookmarkById(bookmarkId)
        }
    }
}
