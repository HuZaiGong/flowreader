package com.flowreader.app.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.data.repository.SettingsRepository
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.Category
import com.flowreader.app.domain.model.ReaderTheme
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.CategoryRepository
import com.flowreader.app.domain.usecase.ImportBookUseCase
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
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val appTheme: ReaderTheme = ReaderTheme.SYSTEM,
    val sortOrder: SortOrder = SortOrder.ADDED_TIME
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val categoryRepository: CategoryRepository,
    private val importBookUseCase: ImportBookUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    val isRefreshing: StateFlow<Boolean> = _uiState
        .map { it.isRefreshing }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.ADDED_TIME)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)

    init {
        loadBooks()
        loadSettings()
        loadCategories()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.appSettings.collect { settings ->
                _uiState.update { it.copy(appTheme = settings.theme) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
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
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
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
                importBookUseCase(uri).onFailure { error ->
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
            _uiState.update { it.copy(isRefreshing = true) }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}
