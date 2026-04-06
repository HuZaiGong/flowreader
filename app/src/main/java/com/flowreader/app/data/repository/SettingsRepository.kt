package com.flowreader.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.flowreader.app.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val FONT_SIZE = intPreferencesKey("font_size")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val PARAGRAPH_SPACING = floatPreferencesKey("paragraph_spacing")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val READER_THEME = stringPreferencesKey("reader_theme")
        val PAGE_MODE = stringPreferencesKey("page_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SCREEN_TIMEOUT_MINUTES = intPreferencesKey("screen_timeout_minutes")
        val AUTO_TIME_THEME = booleanPreferencesKey("auto_time_theme")
        val TIME_THEME_START_HOUR = intPreferencesKey("time_theme_start_hour")
        val TIME_THEME_END_HOUR = intPreferencesKey("time_theme_end_hour")
        val READING_REMINDER_ENABLED = booleanPreferencesKey("reading_reminder_enabled")
        val READING_REMINDER_HOUR = intPreferencesKey("reading_reminder_hour")
        val READING_REMINDER_MINUTE = intPreferencesKey("reading_reminder_minute")
        val SEARCH_HISTORY = stringPreferencesKey("search_history")
        val DAILY_READING_GOAL_MINUTES = intPreferencesKey("daily_reading_goal_minutes")
        val GESTURE_LEFT_TAP = stringPreferencesKey("gesture_left_tap")
        val GESTURE_MIDDLE_TAP = stringPreferencesKey("gesture_middle_tap")
        val GESTURE_RIGHT_TAP = stringPreferencesKey("gesture_right_tap")
        val GESTURE_SWIPE_LEFT = stringPreferencesKey("gesture_swipe_left")
        val GESTURE_SWIPE_RIGHT = stringPreferencesKey("gesture_swipe_right")
        val GESTURE_DOUBLE_TAP = stringPreferencesKey("gesture_double_tap")
        val GESTURE_LONG_PRESS = stringPreferencesKey("gesture_long_press")
        val GESTURE_EDGE_ENABLED = booleanPreferencesKey("gesture_edge_enabled")
    }

    private fun getGestureSettings(preferences: Preferences): GestureSettings = GestureSettings(
        leftTapAction = preferences[PreferencesKeys.GESTURE_LEFT_TAP]?.let { 
            try { GestureAction.valueOf(it) } catch (e: Exception) { GestureAction.PREVIOUS_PAGE } 
        } ?: GestureAction.PREVIOUS_PAGE,
        middleTapAction = preferences[PreferencesKeys.GESTURE_MIDDLE_TAP]?.let { 
            try { GestureAction.valueOf(it) } catch (e: Exception) { GestureAction.TOGGLE_CONTROLS } 
        } ?: GestureAction.TOGGLE_CONTROLS,
        rightTapAction = preferences[PreferencesKeys.GESTURE_RIGHT_TAP]?.let { 
            try { GestureAction.valueOf(it) } catch (e: Exception) { GestureAction.NEXT_PAGE } 
        } ?: GestureAction.NEXT_PAGE,
        swipeLeftAction = preferences[PreferencesKeys.GESTURE_SWIPE_LEFT]?.let { 
            try { GestureAction.valueOf(it) } catch (e: Exception) { GestureAction.NEXT_PAGE } 
        } ?: GestureAction.NEXT_PAGE,
        swipeRightAction = preferences[PreferencesKeys.GESTURE_SWIPE_RIGHT]?.let { 
            try { GestureAction.valueOf(it) } catch (e: Exception) { GestureAction.PREVIOUS_PAGE } 
        } ?: GestureAction.PREVIOUS_PAGE,
        doubleTapAction = preferences[PreferencesKeys.GESTURE_DOUBLE_TAP]?.let { 
            try { GestureAction.valueOf(it) } catch (e: Exception) { GestureAction.SHOW_SETTINGS } 
        } ?: GestureAction.SHOW_SETTINGS,
        longPressAction = preferences[PreferencesKeys.GESTURE_LONG_PRESS]?.let { 
            try { GestureAction.valueOf(it) } catch (e: Exception) { GestureAction.ADD_BOOKMARK } 
        } ?: GestureAction.ADD_BOOKMARK,
        edgeGestureEnabled = preferences[PreferencesKeys.GESTURE_EDGE_ENABLED] ?: true
    )

    val appSettings: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                theme = try {
                    ReaderTheme.valueOf(preferences[PreferencesKeys.THEME] ?: ReaderTheme.SYSTEM.name)
                } catch (e: Exception) {
                    ReaderTheme.SYSTEM
                },
                defaultReadingSettings = ReadingSettings(
                    fontSize = preferences[PreferencesKeys.FONT_SIZE] ?: 18,
                    lineSpacing = preferences[PreferencesKeys.LINE_SPACING] ?: 1.5f,
                    paragraphSpacing = preferences[PreferencesKeys.PARAGRAPH_SPACING] ?: 1.0f,
                    fontFamily = try {
                        FontFamily.valueOf(preferences[PreferencesKeys.FONT_FAMILY] ?: FontFamily.DEFAULT.name)
                    } catch (e: Exception) {
                        FontFamily.DEFAULT
                    },
                    theme = try {
                        ReaderTheme.valueOf(preferences[PreferencesKeys.READER_THEME] ?: ReaderTheme.LIGHT.name)
                    } catch (e: Exception) {
                        ReaderTheme.LIGHT
                    },
                    pageMode = try {
                        PageMode.valueOf(preferences[PreferencesKeys.PAGE_MODE] ?: PageMode.SLIDE.name)
                    } catch (e: Exception) {
                        PageMode.SLIDE
                    },
                    keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: true,
                    screenTimeoutMinutes = preferences[PreferencesKeys.SCREEN_TIMEOUT_MINUTES] ?: 0,
                    gestureSettings = getGestureSettings(preferences)
                ),
                autoTimeTheme = preferences[PreferencesKeys.AUTO_TIME_THEME] ?: false,
                timeThemeStartHour = preferences[PreferencesKeys.TIME_THEME_START_HOUR] ?: 20,
                timeThemeEndHour = preferences[PreferencesKeys.TIME_THEME_END_HOUR] ?: 6,
                readingReminderEnabled = preferences[PreferencesKeys.READING_REMINDER_ENABLED] ?: false,
                readingReminderHour = preferences[PreferencesKeys.READING_REMINDER_HOUR] ?: 20,
                readingReminderMinute = preferences[PreferencesKeys.READING_REMINDER_MINUTE] ?: 0
            )
        }

    suspend fun updateTheme(theme: ReaderTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun updateReadingSettings(settings: ReadingSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = settings.fontSize
            preferences[PreferencesKeys.LINE_SPACING] = settings.lineSpacing
            preferences[PreferencesKeys.PARAGRAPH_SPACING] = settings.paragraphSpacing
            preferences[PreferencesKeys.FONT_FAMILY] = settings.fontFamily.name
            preferences[PreferencesKeys.READER_THEME] = settings.theme.name
            preferences[PreferencesKeys.PAGE_MODE] = settings.pageMode.name
            preferences[PreferencesKeys.KEEP_SCREEN_ON] = settings.keepScreenOn
            preferences[PreferencesKeys.SCREEN_TIMEOUT_MINUTES] = settings.screenTimeoutMinutes
        }
    }

    suspend fun updateReaderTheme(theme: ReaderTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.READER_THEME] = theme.name
        }
    }

    suspend fun updateFontSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = size
        }
    }

    suspend fun updateLineSpacing(spacing: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LINE_SPACING] = spacing
        }
    }

    suspend fun updatePageMode(mode: PageMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PAGE_MODE] = mode.name
        }
    }

    suspend fun updateKeepScreenOn(keepOn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEEP_SCREEN_ON] = keepOn
        }
    }

    suspend fun updateScreenTimeout(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCREEN_TIMEOUT_MINUTES] = minutes
        }
    }

    suspend fun updateAutoTimeTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_TIME_THEME] = enabled
        }
    }

    suspend fun updateTimeThemeHours(startHour: Int, endHour: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TIME_THEME_START_HOUR] = startHour
            preferences[PreferencesKeys.TIME_THEME_END_HOUR] = endHour
        }
    }

    suspend fun updateReadingReminder(enabled: Boolean, hour: Int = 20, minute: Int = 0) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.READING_REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.READING_REMINDER_HOUR] = hour
            preferences[PreferencesKeys.READING_REMINDER_MINUTE] = minute
        }
    }

    suspend fun addSearchHistory(query: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SEARCH_HISTORY] ?: ""
            val historyList = if (current.isNotEmpty()) current.split("|").toMutableList() else mutableListOf()
            if (!historyList.contains(query)) {
                historyList.add(0, query)
                if (historyList.size > 10) historyList.removeAt(historyList.lastIndex)
            }
            preferences[PreferencesKeys.SEARCH_HISTORY] = historyList.joinToString("|")
        }
    }

    fun getSearchHistory(): Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val history = preferences[PreferencesKeys.SEARCH_HISTORY] ?: ""
            if (history.isNotEmpty()) history.split("|") else emptyList()
        }

    suspend fun clearSearchHistory() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEARCH_HISTORY] = ""
        }
    }

    suspend fun updateDailyReadingGoal(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_READING_GOAL_MINUTES] = minutes
        }
    }

    fun getDailyReadingGoal(): Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DAILY_READING_GOAL_MINUTES] ?: 30
        }

    suspend fun updateGestureSettings(settings: GestureSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GESTURE_LEFT_TAP] = settings.leftTapAction.name
            preferences[PreferencesKeys.GESTURE_MIDDLE_TAP] = settings.middleTapAction.name
            preferences[PreferencesKeys.GESTURE_RIGHT_TAP] = settings.rightTapAction.name
            preferences[PreferencesKeys.GESTURE_SWIPE_LEFT] = settings.swipeLeftAction.name
            preferences[PreferencesKeys.GESTURE_SWIPE_RIGHT] = settings.swipeRightAction.name
            preferences[PreferencesKeys.GESTURE_DOUBLE_TAP] = settings.doubleTapAction.name
            preferences[PreferencesKeys.GESTURE_LONG_PRESS] = settings.longPressAction.name
            preferences[PreferencesKeys.GESTURE_EDGE_ENABLED] = settings.edgeGestureEnabled
        }
    }
}
