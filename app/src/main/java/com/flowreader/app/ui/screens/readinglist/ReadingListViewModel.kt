package com.flowreader.app.ui.screens.readinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.ReadingList
import com.flowreader.app.domain.model.ReadingListBook
import com.flowreader.app.domain.model.ReadingListOrder
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.ReadingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReadingListUiState(
    val lists: List<ReadingList> = emptyList(),
    val openList: ReadingList? = null,
    val openListBooks: List<ReadingListBook> = emptyList(),
    val shelf: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Master-detail in one ViewModel: [openList] `null` means the screen is showing the list of lists.
 *
 * The detail order is held optimistically — a drag updates [openListBooks] immediately and only
 * writes to Room when the finger lifts, so a 20-item reorder is one transaction, not twenty.
 */
@HiltViewModel
class ReadingListViewModel @Inject constructor(
    private val readingListRepository: ReadingListRepository,
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingListUiState())
    val uiState: StateFlow<ReadingListUiState> = _uiState.asStateFlow()

    private var detailJob: Job? = null

    init {
        observeLists()
        loadShelf()
    }

    private fun observeLists() {
        viewModelScope.launch {
            readingListRepository.getAllLists().collect { lists ->
                _uiState.update { state ->
                    state.copy(
                        lists = lists,
                        isLoading = false,
                        // Keep the open list's own metadata fresh after a rename.
                        openList = state.openList?.let { open -> lists.firstOrNull { it.id == open.id } }
                    )
                }
            }
        }
    }

    private fun loadShelf() {
        viewModelScope.launch {
            runCatching { bookRepository.getAllBooks().first() }
                .onSuccess { books -> _uiState.update { it.copy(shelf = books) } }
        }
    }

    fun openList(list: ReadingList) {
        _uiState.update { it.copy(openList = list, openListBooks = emptyList()) }
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            readingListRepository.getBooksInList(list.id).collect { books ->
                _uiState.update { it.copy(openListBooks = books) }
            }
        }
    }

    fun closeList() {
        detailJob?.cancel()
        detailJob = null
        _uiState.update { it.copy(openList = null, openListBooks = emptyList()) }
    }

    fun createList(name: String, description: String) {
        viewModelScope.launch {
            runCatching { readingListRepository.createList(name, description) }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
        }
    }

    fun renameList(listId: Long, name: String, description: String) {
        viewModelScope.launch {
            runCatching { readingListRepository.renameList(listId, name, description) }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
        }
    }

    fun deleteList(listId: Long) {
        viewModelScope.launch {
            runCatching { readingListRepository.deleteList(listId) }
                .onSuccess { if (_uiState.value.openList?.id == listId) closeList() }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
        }
    }

    fun addBooks(bookIds: List<Long>) {
        val listId = _uiState.value.openList?.id ?: return
        viewModelScope.launch {
            runCatching { readingListRepository.addBooks(listId, bookIds) }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
        }
    }

    fun removeBook(bookId: Long) {
        val listId = _uiState.value.openList?.id ?: return
        viewModelScope.launch {
            runCatching { readingListRepository.removeBook(listId, bookId) }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
        }
    }

    /** Optimistic, in-memory only. Call [commitOrder] once the gesture ends. */
    fun moveBook(from: Int, to: Int) {
        _uiState.update { state ->
            state.copy(openListBooks = ReadingListOrder.move(state.openListBooks, from, to))
        }
    }

    fun commitOrder() {
        val state = _uiState.value
        val listId = state.openList?.id ?: return
        val order = state.openListBooks.map { it.book.id }
        if (order.isEmpty()) return
        viewModelScope.launch {
            runCatching { readingListRepository.reorder(listId, order) }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
