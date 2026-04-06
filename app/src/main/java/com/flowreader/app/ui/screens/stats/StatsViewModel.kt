package com.flowreader.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.DailyStats
import com.flowreader.app.domain.model.ReadingSummary
import com.flowreader.app.domain.repository.ReadingStatsRepository
import com.flowreader.app.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val isLoading: Boolean = true,
    val todayReadTime: Long = 0,
    val todayReadPages: Int = 0,
    val totalReadTime: Long = 0,
    val totalReadPages: Int = 0,
    val totalBooks: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val recentDailyStats: List<DailyStats> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val readingStatsRepository: ReadingStatsRepository,
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val todayReadTime = readingStatsRepository.getTodayReadTime()
            val todayReadPages = readingStatsRepository.getTodayReadPages()
            val totalReadTime = readingStatsRepository.getTotalReadTime()
            val totalReadPages = readingStatsRepository.getTotalReadPages()
            val summary = readingStatsRepository.getReadingSummary()

            readingStatsRepository.getRecentDailyStats(7).collect { dailyStats ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        todayReadTime = todayReadTime,
                        todayReadPages = todayReadPages,
                        totalReadTime = totalReadTime,
                        totalReadPages = totalReadPages,
                        totalBooks = summary.totalBooks,
                        currentStreak = summary.currentStreak,
                        longestStreak = summary.longestStreak,
                        recentDailyStats = dailyStats
                    )
                }
            }
        }
    }

    fun refresh() {
        loadStats()
    }
}