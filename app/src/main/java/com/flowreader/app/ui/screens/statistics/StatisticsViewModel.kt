package com.flowreader.app.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.data.repository.SettingsRepository
import com.flowreader.app.domain.model.BookStats
import com.flowreader.app.domain.model.DailyStats
import com.flowreader.app.domain.model.ReaderTheme
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.ReadingStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val totalReadTime: Int = 0,
    val totalBooks: Int = 0,
    val totalChapters: Int = 0,
    val dailyStats: List<DailyStats> = emptyList(),
    val bookStats: List<BookStats> = emptyList(),
    val appTheme: ReaderTheme = ReaderTheme.SYSTEM,
    val error: String? = null
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val readingStatsRepository: ReadingStatsRepository,
    private val bookRepository: BookRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.appSettings.collect { settings ->
                _uiState.update { it.copy(appTheme = settings.theme) }
            }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = calendar.timeInMillis

                combine(
                    bookRepository.getAllBooks(),
                    readingStatsRepository.getDailyStats(weekAgo)
                ) { books, dailyStats ->
                    Pair(books, dailyStats)
                }.collect { (books, dailyStats) ->
                    val totalReadTime = dailyStats.sumOf { it.totalReadTime }
                    val totalChapters = books.sumOf { it.currentChapter + 1 }

                    val bookStatsList = books.filter { it.readingProgress > 0 || it.currentChapter > 0 }
                        .map { book ->
                            BookStats(
                                bookId = book.id,
                                bookTitle = book.title,
                                totalReadTime = 0,
                                totalChapters = book.currentChapter + 1,
                                totalPages = (book.readingProgress * 100).toInt(),
                                lastReadDate = book.lastReadTime
                            )
                        }.sortedByDescending { it.lastReadDate }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalReadTime = totalReadTime,
                            totalBooks = books.size,
                            totalChapters = totalChapters,
                            dailyStats = dailyStats,
                            bookStats = bookStatsList
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}