package com.flowreader.app.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.data.repository.SettingsRepository
import com.flowreader.app.data.local.DataInitializer
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.ReaderTheme
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.util.BookParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder {
    ADDED_TIME,
    LAST_READ,
    TITLE,
    AUTHOR
}

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val recentlyRead: List<Book> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val appTheme: ReaderTheme = ReaderTheme.SYSTEM,
    val sortOrder: SortOrder = SortOrder.ADDED_TIME
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val bookParser: BookParser,
    private val settingsRepository: SettingsRepository,
    private val dataInitializer: DataInitializer
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.ADDED_TIME)

    init {
        initializeDefaultBooks()
        loadBooks()
        loadSettings()
    }

    private fun initializeDefaultBooks() {
        viewModelScope.launch {
            dataInitializer.initializeDefaultBooks()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.appSettings.collect { settings ->
                _uiState.update { it.copy(appTheme = settings.theme) }
            }
        }
    }

    private fun loadBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                bookRepository.getAllBooks(),
                bookRepository.getRecentlyReadBooks(5),
                _searchQuery.debounce(300),
                _sortOrder
            ) { allBooks, recentlyRead, query, sortOrder ->
                val sortedBooks = when (sortOrder) {
                    SortOrder.ADDED_TIME -> allBooks.sortedByDescending { it.addedTime }
                    SortOrder.LAST_READ -> allBooks.sortedByDescending { it.lastReadTime ?: it.addedTime }
                    SortOrder.TITLE -> allBooks.sortedBy { it.title }
                    SortOrder.AUTHOR -> allBooks.sortedBy { it.author }
                }
                val filteredBooks = if (query.isBlank()) {
                    sortedBooks
                } else {
                    sortedBooks.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
                    }
                }
                Triple(filteredBooks, recentlyRead, query)
            }.collect { (books, recentlyRead, query) ->
                _uiState.update {
                    it.copy(
                        books = books,
                        recentlyRead = recentlyRead,
                        searchQuery = query,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadMoreBooks() {
        val currentCount = _uiState.value.books.size
        viewModelScope.launch {
            val moreBooks = bookRepository.getBooksPaged(currentCount, PAGE_SIZE)
            if (moreBooks.isNotEmpty()) {
                _uiState.update {
                    it.copy(books = it.books + moreBooks)
                }
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val existingBook = bookParser.copyFileToInternal(uri)
                val parseResult = bookParser.parseBook(uri)

                parseResult.onSuccess { result ->
                    val internalPath = bookParser.copyFileToInternal(uri)
                    val book = result.book.copy(filePath = internalPath ?: "")
                    val bookId = bookRepository.insertBook(book)
                    val chaptersWithBookId = result.chapters.map { it.copy(bookId = bookId) }
                    chapterRepository.insertChapters(chaptersWithBookId)
                }.onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "导入失败"
                    )
                }
            }
        }
    }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            bookRepository.deleteBookById(bookId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
