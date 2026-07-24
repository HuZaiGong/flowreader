package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.AppSettings
import com.flowreader.app.domain.model.GestureSettings
import com.flowreader.app.domain.model.PageMode
import com.flowreader.app.domain.model.ReaderTheme
import com.flowreader.app.domain.model.ReadingSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    companion object {
        const val THEME_KEY = "theme"
    }

    val appSettings: Flow<AppSettings>

    suspend fun updateTheme(theme: ReaderTheme)
    suspend fun updateReadingSettings(settings: ReadingSettings)
    suspend fun updateReaderTheme(theme: ReaderTheme)
    suspend fun updateFontSize(size: Int)
    suspend fun updateLineSpacing(spacing: Float)
    suspend fun updatePageMode(mode: PageMode)
    suspend fun updateKeepScreenOn(keepOn: Boolean)
    suspend fun updateScreenTimeout(minutes: Int)
    suspend fun updateReadingReminder(enabled: Boolean, hour: Int = 20, minute: Int = 0)
    suspend fun addSearchHistory(query: String)
    fun getSearchHistory(): Flow<List<String>>
    suspend fun clearSearchHistory()
    suspend fun updateDailyReadingGoal(minutes: Int)
    fun getDailyReadingGoal(): Flow<Int>
    suspend fun updateGestureSettings(settings: GestureSettings)
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted()
}
