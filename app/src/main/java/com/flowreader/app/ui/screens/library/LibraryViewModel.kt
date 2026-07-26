package com.flowreader.app.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.repository.SettingsRepository
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.Category
import com.flowreader.app.domain.model.GlobalSearchResult
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.CategoryRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.domain.repository.SearchRepository
import com.flowreader.app.util.BookParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
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
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val sortOrder: SortOrder = SortOrder.ADDED_TIME,
    val globalSearchResults: List<GlobalSearchResult> = emptyList(),
    val isGlobalSearching: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val categoryRepository: CategoryRepository,
    private val bookParser: BookParser,
    private val settingsRepository: SettingsRepository,
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.ADDED_TIME)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private var booksCollectionJob: Job? = null
    private var globalSearchJob: Job? = null

    init {
        loadCategories()
        loadBooks()
        observeGlobalSearch()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun loadBooks() {
        booksCollectionJob?.cancel()
        booksCollectionJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                bookRepository.getAllBooks(),
                bookRepository.getRecentlyReadBooks(5),
                _searchQuery.debounce(300),
                _sortOrder,
                _selectedCategoryId
            ) { allBooks, recentlyRead, query, sortOrder, categoryId ->
                var filteredBooks = allBooks

                if (categoryId != null) {
                    filteredBooks = filteredBooks.filter { it.categoryId == categoryId }
                }

                val sortedBooks = when (sortOrder) {
                    SortOrder.ADDED_TIME -> filteredBooks.sortedByDescending { it.addedTime }
                    SortOrder.LAST_READ -> filteredBooks.sortedByDescending { it.lastReadTime ?: it.addedTime }
                    SortOrder.TITLE -> filteredBooks.sortedBy { it.title }
                    SortOrder.AUTHOR -> filteredBooks.sortedBy { it.author }
                }
                val filteredByQuery = if (query.isBlank()) {
                    sortedBooks
                } else {
                    sortedBooks.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
                    }
                }
                Triple(filteredByQuery, recentlyRead, query)
                }.collect { (books, recentlyRead, query) ->
                    _uiState.update {
                        it.copy(
                            books = books,
                            recentlyRead = recentlyRead,
                            searchQuery = query,
                            selectedCategoryId = _selectedCategoryId.value,
                            sortOrder = _sortOrder.value,
                            isLoading = false
                        )
                    }
            }
        }
    }

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            val category = Category(name = name)
            categoryRepository.insertCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    fun updateBookCategory(bookId: Long, categoryId: Long?) {
        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId)
            book?.let {
                bookRepository.updateBook(it.copy(categoryId = categoryId))
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
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    @OptIn(FlowPreview::class)
    private fun observeGlobalSearch() {
        globalSearchJob = viewModelScope.launch {
            _searchQuery
                .debounce(350)
                .map { it.trim() }
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _uiState.update { it.copy(globalSearchResults = emptyList(), isGlobalSearching = false) }
                        return@collectLatest
                    }
                    _uiState.update { it.copy(isGlobalSearching = true) }
                    try {
                        val results = searchRepository.searchAll(query)
                        _uiState.update { it.copy(globalSearchResults = results, isGlobalSearching = false) }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(globalSearchResults = emptyList(), isGlobalSearching = false, error = "全局搜索失败: ${e.localizedMessage ?: "未知错误"}") }
                    }
                }
            }
    }

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val parseResult = bookParser.parseBook(uri)

                parseResult.onSuccess { result ->
                    val internalPath = result.pdfFilePath ?: bookParser.copyFileToInternal(uri)
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

    fun refreshBooks() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 重新加载书籍列表
                loadBooks()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
