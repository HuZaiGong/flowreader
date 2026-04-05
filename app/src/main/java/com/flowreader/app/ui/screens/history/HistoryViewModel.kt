package com.flowreader.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.data.repository.SettingsRepository
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.ReaderTheme
import com.flowreader.app.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = true,
    val history: List<Book> = emptyList(),
    val appTheme: ReaderTheme = ReaderTheme.SYSTEM,
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.appSettings.collect { settings ->
                _uiState.update { it.copy(appTheme = settings.theme) }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                bookRepository.getRecentlyReadBooks(50).collect { books ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            history = books.sortedByDescending { b -> b.lastReadTime ?: Date(0) }
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteHistory(bookId: Long) {
        viewModelScope.launch {
            try {
                val book = bookRepository.getBookById(bookId)
                book?.let {
                    bookRepository.updateReadingProgress(bookId, 0, 0, 0f)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                bookRepository.getAllBooks().first().forEach { book ->
                    bookRepository.updateReadingProgress(book.id, 0, 0, 0f)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}