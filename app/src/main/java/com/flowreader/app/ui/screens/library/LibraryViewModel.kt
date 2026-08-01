package com.flowreader.app.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.model.Category
import com.flowreader.app.domain.model.LibraryViewMode
import com.flowreader.app.domain.model.ReadingList
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.CategoryRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.domain.repository.ReadingListRepository
import com.flowreader.app.domain.repository.SearchRepository
import com.flowreader.app.domain.repository.SettingsRepository
import com.flowreader.app.util.BookParser
import com.flowreader.app.util.ZipImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder {
    ADDED_TIME,
    LAST_READ,
    TITLE,
    AUTHOR
}

/**
 * Localizable feedback. The ViewModel reports *what* happened; `LibraryScreen` decides how to say
 * it, so batch results survive the v53 in-app language switch instead of being frozen Chinese.
 */
sealed interface LibraryMessage {
    data class Deleted(val count: Int) : LibraryMessage
    data class Moved(val count: Int) : LibraryMessage
    data class Updated(val count: Int) : LibraryMessage
    data class AddedToList(val listName: String) : LibraryMessage
    data class ArchiveImported(val count: Int) : LibraryMessage
}

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val recentlyRead: List<Book> = emptyList(),
    val categories: List<Category> = emptyList(),
    val readingLists: List<ReadingList> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val message: LibraryMessage? = null,
    val sortOrder: SortOrder = SortOrder.ADDED_TIME,
    val selectedBookIds: Set<Long> = emptySet(),
    val viewMode: LibraryViewMode = LibraryViewMode.LIST
) {
    val selectionMode: Boolean get() = selectedBookIds.isNotEmpty()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val categoryRepository: CategoryRepository,
    private val readingListRepository: ReadingListRepository,
    private val bookParser: BookParser,
    private val zipImporter: ZipImporter,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            settingsRepository.getLibraryViewMode().collect { mode ->
                _uiState.update { it.copy(viewMode = mode) }
            }
        }
    }

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ADDED_TIME)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private var booksCollectionJob: Job? = null

    init {
        loadCategories()
        loadReadingLists()
        loadBooks()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    private fun loadReadingLists() {
        viewModelScope.launch {
            readingListRepository.getAllLists().collect { lists ->
                _uiState.update { it.copy(readingLists = lists) }
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
                _sortOrder,
                _selectedCategoryId
            ) { allBooks, recentlyRead, sortOrder, categoryId ->
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
                sortedBooks to recentlyRead
            }.collect { (books, recentlyRead) ->
                val visibleIds = books.map { it.id }.toSet()
                _uiState.update {
                    it.copy(
                        books = books,
                        recentlyRead = recentlyRead,
                        selectedCategoryId = _selectedCategoryId.value,
                        sortOrder = _sortOrder.value,
                        isLoading = false,
                        // Books can disappear under an active selection (delete, filter change);
                        // keeping their ids would let a later batch action target nothing.
                        selectedBookIds = it.selectedBookIds intersect visibleIds
                    )
                }
            }
        }
    }

    fun setViewMode(mode: LibraryViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        viewModelScope.launch {
            settingsRepository.setLibraryViewMode(mode)
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

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    /**
     * Single entry point for the picker. A `.fb2.zip` is a book, a plain `.zip` is an archive of
     * books — format detection runs first so the two do not collide.
     */
    fun importBook(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val name = bookParser.displayName(uri)
            if (BookParser.detectFormatStatic(name) == BookFormat.UNKNOWN && isZipArchiveName(name)) {
                if (zipImporter.isComicArchive(uri)) {
                    importSingle(uri).fold(
                        onSuccess = { _uiState.update { state -> state.copy(isLoading = false) } },
                        onFailure = { error -> _uiState.update { state -> state.copy(isLoading = false, error = error.message ?: "导入失败") } }
                    )
                } else {
                    importArchiveInternal(uri)
                }
                return@launch
            }
            val result = importSingle(uri)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isLoading = false) },
                    onFailure = { state.copy(isLoading = false, error = it.message ?: "导入失败") }
                )
            }
        }
    }

    private fun isZipArchiveName(name: String): Boolean =
        name.endsWith(".zip", ignoreCase = true) || name.endsWith(".cbz", ignoreCase = true)

    private suspend fun importArchiveInternal(uri: Uri) {
        zipImporter.extract(uri)
            .onSuccess { files ->
                var imported = 0
                val failures = mutableListOf<String>()
                files.forEach { file ->
                    importSingle(Uri.fromFile(file))
                        .onSuccess { imported++ }
                        .onFailure { failures.add("${file.name}: ${it.message ?: "解析失败"}") }
                    file.delete()
                }
                files.firstOrNull()?.parentFile?.deleteRecursively()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = LibraryMessage.ArchiveImported(imported),
                        // Report the first few failures rather than silently importing 3 of 30.
                        error = failures.take(3).joinToString("\n").takeIf { text -> text.isNotEmpty() }
                    )
                }
            }
            .onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message ?: "压缩包导入失败") }
            }
    }

    private suspend fun importSingle(uri: Uri): Result<Long> =
        bookParser.parseBook(uri).mapCatching { result ->
            val internalPath = result.pdfFilePath ?: bookParser.copyFileToInternal(uri)
            val book = result.book.copy(filePath = internalPath ?: "")
            val bookId = bookRepository.insertBook(book)
            chapterRepository.insertChapters(result.chapters.map { it.copy(bookId = bookId) })
            bookId
        }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            bookRepository.deleteBookById(bookId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun refreshBooks() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadBooks()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // ---- Multi-select (v53) -------------------------------------------------

    fun toggleSelection(bookId: Long) {
        _uiState.update { state ->
            val next = if (bookId in state.selectedBookIds) {
                state.selectedBookIds - bookId
            } else {
                state.selectedBookIds + bookId
            }
            state.copy(selectedBookIds = next)
        }
    }

    fun selectAllVisible() {
        _uiState.update { state -> state.copy(selectedBookIds = state.books.map { it.id }.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedBookIds = emptySet()) }
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedBookIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching { bookRepository.deleteBooksByIds(ids) }
                .onSuccess {
                    _uiState.update { it.copy(selectedBookIds = emptySet(), message = LibraryMessage.Deleted(ids.size)) }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.message ?: "批量删除失败") } }
        }
    }

    fun moveSelectedToCategory(categoryId: Long?) {
        val ids = _uiState.value.selectedBookIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching { bookRepository.moveBooksToCategory(ids, categoryId) }
                .onSuccess {
                    _uiState.update { it.copy(selectedBookIds = emptySet(), message = LibraryMessage.Moved(ids.size)) }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.message ?: "批量移动失败") } }
        }
    }

    fun updateSelectedMetadata(author: String?, tagsText: String?) {
        val ids = _uiState.value.selectedBookIds.toList()
        if (ids.isEmpty()) return
        val tags = tagsText?.takeIf { it.isNotBlank() }
            ?.split(",", "，", " ")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
        viewModelScope.launch {
            runCatching { bookRepository.updateBooksMetadata(ids, author?.takeIf { it.isNotBlank() }, tags) }
                .onSuccess {
                    _uiState.update { it.copy(selectedBookIds = emptySet(), message = LibraryMessage.Updated(ids.size)) }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.message ?: "批量编辑失败") } }
        }
    }

    fun addSelectedToList(list: ReadingList) {
        val ids = _uiState.value.selectedBookIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching { readingListRepository.addBooks(list.id, ids) }
                .onSuccess {
                    _uiState.update {
                        it.copy(selectedBookIds = emptySet(), message = LibraryMessage.AddedToList(list.name))
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.message ?: "加入书单失败") } }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
