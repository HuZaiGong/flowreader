package com.flowreader.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.DailyStats
import com.flowreader.app.domain.model.ReadingReport
import com.flowreader.app.domain.model.ReadingSummary
import com.flowreader.app.domain.repository.ReadingStatsRepository
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val todayReadTime: Long = 0,
    val todayReadPages: Int = 0,
    val totalReadTime: Long = 0,
    val totalReadPages: Int = 0,
    val totalBooks: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val recentDailyStats: List<DailyStats> = emptyList(),
    val weeklyGoalMinutes: Int = 210,
    val monthlyGoalMinutes: Int = 900,
    val weeklyReport: ReadingReport? = null,
    val monthlyReport: ReadingReport? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val readingStatsRepository: ReadingStatsRepository,
    private val bookRepository: BookRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                readingStatsRepository.getRecentDailyStats(7).collect { dailyStats ->
                    val todayReadTime = readingStatsRepository.getTodayReadTime()
                    val todayReadPages = readingStatsRepository.getTodayReadPages()
                    val totalReadTime = readingStatsRepository.getTotalReadTime()
                    val totalReadPages = readingStatsRepository.getTotalReadPages()
                    val summary = readingStatsRepository.getReadingSummary()
                    val weeklyGoal = settingsRepository.getWeeklyReadingGoal().first()
                    val monthlyGoal = settingsRepository.getMonthlyReadingGoal().first()
                    val weeklyReport = readingStatsRepository.getReadingReport(7)
                    val monthlyReport = readingStatsRepository.getReadingReport(30)
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
                            recentDailyStats = dailyStats,
                            weeklyGoalMinutes = weeklyGoal,
                            monthlyGoalMinutes = monthlyGoal,
                            weeklyReport = weeklyReport,
                            monthlyReport = monthlyReport
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "加载统计失败: ${e.localizedMessage ?: "未知错误"}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun refresh() {
        loadStats()
    }

    fun updateWeeklyGoal(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateWeeklyReadingGoal(minutes)
            refresh()
        }
    }

    fun updateMonthlyGoal(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateMonthlyReadingGoal(minutes)
            refresh()
        }
    }
}
