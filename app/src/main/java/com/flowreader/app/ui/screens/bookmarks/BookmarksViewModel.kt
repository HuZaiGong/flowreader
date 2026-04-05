package com.flowreader.app.ui.screens.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.data.repository.SettingsRepository
import com.flowreader.app.domain.model.ReaderTheme
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookmarkWithBookTitle(
    val id: Long,
    val bookId: Long,
    val chapterIndex: Int,
    val position: Int,
    val text: String,
    val createdTime: java.util.Date,
    val bookTitle: String
)

data class BookmarksUiState(
    val isLoading: Boolean = true,
    val bookmarks: List<BookmarkWithBookTitle> = emptyList(),
    val appTheme: ReaderTheme = ReaderTheme.SYSTEM,
    val error: String? = null
)

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val bookRepository: BookRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    init {
        loadBookmarks()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.appSettings.collect { settings ->
                _uiState.update { it.copy(appTheme = settings.theme) }
            }
        }
    }

    private fun loadBookmarks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                combine(
                    bookmarkRepository.getAllBookmarks(),
                    bookRepository.getAllBooks()
                ) { bookmarks, books ->
                    val bookMap = books.associateBy { it.id }
                    bookmarks.map { bookmark ->
                        BookmarkWithBookTitle(
                            id = bookmark.id,
                            bookId = bookmark.bookId,
                            chapterIndex = bookmark.chapterIndex,
                            position = bookmark.position,
                            text = bookmark.text,
                            createdTime = bookmark.createdTime,
                            bookTitle = bookMap[bookmark.bookId]?.title ?: "未知书籍"
                        )
                    }
                }.collect { bookmarksWithTitle ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            bookmarks = bookmarksWithTitle.sortedByDescending { b -> b.createdTime }
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            try {
                bookmarkRepository.deleteBookmarkById(bookmarkId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteBookmarksByBookId(bookId: Long) {
        viewModelScope.launch {
            try {
                bookmarkRepository.deleteBookmarksByBookId(bookId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearAllBookmarks() {
        viewModelScope.launch {
            try {
                bookmarkRepository.clearAllBookmarks()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}