package com.flowreader.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flowreader.app.domain.model.AppLanguage
import com.flowreader.app.domain.model.AppSettings
import com.flowreader.app.domain.model.AppThemeMode
import com.flowreader.app.domain.model.ColorSource
import com.flowreader.app.domain.model.GestureAction
import com.flowreader.app.domain.model.GestureSettings
import com.flowreader.app.domain.model.PageMode
import com.flowreader.app.domain.model.ReaderFontFamily
import com.flowreader.app.domain.model.ReaderPaletteId
import com.flowreader.app.domain.model.ReadingSettings
import com.flowreader.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {
    private object PreferencesKeys {
        val THEME = stringPreferencesKey(SettingsRepository.THEME_KEY)
        val COLOR_SOURCE = stringPreferencesKey("color_source")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val FONT_SIZE = intPreferencesKey("font_size")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val PARAGRAPH_SPACING = floatPreferencesKey("paragraph_spacing")
        val FIRST_LINE_INDENT = booleanPreferencesKey("first_line_indent")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val READER_PALETTE = stringPreferencesKey("reader_theme")
        val READER_NIGHT_PALETTE = stringPreferencesKey("reader_night_palette")
        val PAGE_MODE = stringPreferencesKey("page_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SCREEN_TIMEOUT_MINUTES = intPreferencesKey("screen_timeout_minutes")
        val EYE_PROTECTION_INTERVAL_MINUTES = intPreferencesKey("eye_protection_interval_minutes")
        val AUTO_NIGHT_MODE = booleanPreferencesKey("auto_night_mode")
        val TAP_ZONE_RATIO = floatPreferencesKey("tap_zone_ratio")
        val READING_REMINDER_ENABLED = booleanPreferencesKey("reading_reminder_enabled")
        val READING_REMINDER_HOUR = intPreferencesKey("reading_reminder_hour")
        val READING_REMINDER_MINUTE = intPreferencesKey("reading_reminder_minute")
        val WIDGET_BOOK_TITLE = stringPreferencesKey("widget_book_title")
        val WIDGET_PROGRESS_PERCENT = intPreferencesKey("widget_progress_percent")
        val SEARCH_HISTORY = stringPreferencesKey("search_history")
        val DAILY_READING_GOAL_MINUTES = intPreferencesKey("daily_reading_goal_minutes")
        val WEEKLY_READING_GOAL_MINUTES = intPreferencesKey("weekly_reading_goal_minutes")
        val MONTHLY_READING_GOAL_MINUTES = intPreferencesKey("monthly_reading_goal_minutes")
        val GESTURE_LEFT_TAP = stringPreferencesKey("gesture_left_tap")
        val GESTURE_MIDDLE_TAP = stringPreferencesKey("gesture_middle_tap")
        val GESTURE_RIGHT_TAP = stringPreferencesKey("gesture_right_tap")
        val GESTURE_SWIPE_LEFT = stringPreferencesKey("gesture_swipe_left")
        val GESTURE_SWIPE_RIGHT = stringPreferencesKey("gesture_swipe_right")
        val GESTURE_DOUBLE_TAP = stringPreferencesKey("gesture_double_tap")
        val GESTURE_LONG_PRESS = stringPreferencesKey("gesture_long_press")
        val GESTURE_EDGE_ENABLED = booleanPreferencesKey("gesture_edge_enabled")
        val GESTURE_LEFT_EDGE_WIDTH = intPreferencesKey("gesture_left_edge_width")
        val GESTURE_RIGHT_EDGE_WIDTH = intPreferencesKey("gesture_right_edge_width")
        val CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    private fun readGestureSettings(preferences: Preferences): GestureSettings {
        val defaults = GestureSettings()
        return GestureSettings(
            leftTapAction = GestureAction.fromStoredName(preferences[PreferencesKeys.GESTURE_LEFT_TAP], defaults.leftTapAction),
            middleTapAction = GestureAction.fromStoredName(preferences[PreferencesKeys.GESTURE_MIDDLE_TAP], defaults.middleTapAction),
            rightTapAction = GestureAction.fromStoredName(preferences[PreferencesKeys.GESTURE_RIGHT_TAP], defaults.rightTapAction),
            swipeLeftAction = GestureAction.fromStoredName(preferences[PreferencesKeys.GESTURE_SWIPE_LEFT], defaults.swipeLeftAction),
            swipeRightAction = GestureAction.fromStoredName(preferences[PreferencesKeys.GESTURE_SWIPE_RIGHT], defaults.swipeRightAction),
            doubleTapAction = GestureAction.fromStoredName(preferences[PreferencesKeys.GESTURE_DOUBLE_TAP], defaults.doubleTapAction),
            longPressAction = GestureAction.fromStoredName(preferences[PreferencesKeys.GESTURE_LONG_PRESS], defaults.longPressAction),
            edgeGestureEnabled = preferences[PreferencesKeys.GESTURE_EDGE_ENABLED] ?: defaults.edgeGestureEnabled,
            leftEdgeWidth = (preferences[PreferencesKeys.GESTURE_LEFT_EDGE_WIDTH] ?: defaults.leftEdgeWidth).coerceIn(0, 45),
            rightEdgeWidth = (preferences[PreferencesKeys.GESTURE_RIGHT_EDGE_WIDTH] ?: defaults.rightEdgeWidth).coerceIn(0, 45)
        )
    }

    private fun readReadingSettings(preferences: Preferences): ReadingSettings {
        val palette = ReaderPaletteId.fromStoredName(preferences[PreferencesKeys.READER_PALETTE], ReaderPaletteId.PAPER)
        return ReadingSettings(
            fontSize = (preferences[PreferencesKeys.FONT_SIZE] ?: 18).coerceIn(12, 32),
            lineSpacing = (preferences[PreferencesKeys.LINE_SPACING] ?: 1.5f).coerceIn(1.0f, 2.5f),
            paragraphSpacing = ReadingSettings.normalizeParagraphSpacing(preferences[PreferencesKeys.PARAGRAPH_SPACING] ?: 1.0f),
            firstLineIndent = preferences[PreferencesKeys.FIRST_LINE_INDENT] ?: true,
            fontFamily = ReaderFontFamily.fromStoredName(preferences[PreferencesKeys.FONT_FAMILY]),
            customFontPath = preferences[PreferencesKeys.CUSTOM_FONT_PATH],
            palette = palette,
            nightPalette = ReaderPaletteId.fromStoredName(
                preferences[PreferencesKeys.READER_NIGHT_PALETTE],
                ReaderPaletteId.NIGHT
            ).takeIf { it.isDark } ?: ReaderPaletteId.NIGHT,
            pageMode = PageMode.fromStoredName(preferences[PreferencesKeys.PAGE_MODE]),
            keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: true,
            screenTimeoutMinutes = preferences[PreferencesKeys.SCREEN_TIMEOUT_MINUTES] ?: 0,
            eyeProtectionIntervalMinutes = preferences[PreferencesKeys.EYE_PROTECTION_INTERVAL_MINUTES] ?: 20,
            autoNightMode = preferences[PreferencesKeys.AUTO_NIGHT_MODE] ?: false,
            tapZoneRatio = (preferences[PreferencesKeys.TAP_ZONE_RATIO] ?: 0.3f).coerceIn(0.1f, 0.45f),
            gestureSettings = readGestureSettings(preferences)
        )
    }

    override val appSettings: Flow<AppSettings> = context.dataStore.data
        .retry(3) { it is IOException }
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                themeMode = AppThemeMode.fromStoredName(preferences[PreferencesKeys.THEME]),
                colorSource = ColorSource.fromStoredName(preferences[PreferencesKeys.COLOR_SOURCE]),
                language = AppLanguage.fromStoredName(preferences[PreferencesKeys.APP_LANGUAGE]),
                defaultReadingSettings = readReadingSettings(preferences),
                readingReminderEnabled = preferences[PreferencesKeys.READING_REMINDER_ENABLED] ?: false,
                readingReminderHour = preferences[PreferencesKeys.READING_REMINDER_HOUR] ?: 20,
                readingReminderMinute = preferences[PreferencesKeys.READING_REMINDER_MINUTE] ?: 0
            )
        }

    override suspend fun updateThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = mode.name
        }
    }

    override suspend fun updateColorSource(source: ColorSource) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COLOR_SOURCE] = source.name
        }
    }

    override suspend fun updateLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = language.name
        }
    }

    override suspend fun updateReadingSettings(settings: ReadingSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = settings.fontSize
            preferences[PreferencesKeys.LINE_SPACING] = settings.lineSpacing
            preferences[PreferencesKeys.PARAGRAPH_SPACING] = settings.paragraphSpacing
            preferences[PreferencesKeys.FIRST_LINE_INDENT] = settings.firstLineIndent
            preferences[PreferencesKeys.FONT_FAMILY] = settings.fontFamily.name
            preferences[PreferencesKeys.READER_PALETTE] = settings.palette.name
            preferences[PreferencesKeys.READER_NIGHT_PALETTE] = settings.nightPalette.name
            preferences[PreferencesKeys.PAGE_MODE] = settings.pageMode.name
            preferences[PreferencesKeys.KEEP_SCREEN_ON] = settings.keepScreenOn
            preferences[PreferencesKeys.SCREEN_TIMEOUT_MINUTES] = settings.screenTimeoutMinutes
            preferences[PreferencesKeys.EYE_PROTECTION_INTERVAL_MINUTES] = settings.eyeProtectionIntervalMinutes
            preferences[PreferencesKeys.AUTO_NIGHT_MODE] = settings.autoNightMode
            preferences[PreferencesKeys.TAP_ZONE_RATIO] = settings.tapZoneRatio
            writeGestureSettings(preferences, settings.gestureSettings)
            val customFontPath = settings.customFontPath
            if (customFontPath != null) {
                preferences[PreferencesKeys.CUSTOM_FONT_PATH] = customFontPath
            } else {
                preferences.remove(PreferencesKeys.CUSTOM_FONT_PATH)
            }
        }
    }

    private fun writeGestureSettings(preferences: MutablePreferences, settings: GestureSettings) {
        preferences[PreferencesKeys.GESTURE_LEFT_TAP] = settings.leftTapAction.name
        preferences[PreferencesKeys.GESTURE_MIDDLE_TAP] = settings.middleTapAction.name
        preferences[PreferencesKeys.GESTURE_RIGHT_TAP] = settings.rightTapAction.name
        preferences[PreferencesKeys.GESTURE_SWIPE_LEFT] = settings.swipeLeftAction.name
        preferences[PreferencesKeys.GESTURE_SWIPE_RIGHT] = settings.swipeRightAction.name
        preferences[PreferencesKeys.GESTURE_DOUBLE_TAP] = settings.doubleTapAction.name
        preferences[PreferencesKeys.GESTURE_LONG_PRESS] = settings.longPressAction.name
        preferences[PreferencesKeys.GESTURE_EDGE_ENABLED] = settings.edgeGestureEnabled
        preferences[PreferencesKeys.GESTURE_LEFT_EDGE_WIDTH] = settings.leftEdgeWidth
        preferences[PreferencesKeys.GESTURE_RIGHT_EDGE_WIDTH] = settings.rightEdgeWidth
    }

    override suspend fun updateReadingReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.READING_REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.READING_REMINDER_HOUR] = hour
            preferences[PreferencesKeys.READING_REMINDER_MINUTE] = minute
        }
    }

    override suspend fun addSearchHistory(query: String) {
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

    override fun getSearchHistory(): Flow<List<String>> = context.dataStore.data
        .retry(3) { it is IOException }
        .map { preferences ->
            val history = preferences[PreferencesKeys.SEARCH_HISTORY] ?: ""
            if (history.isNotEmpty()) history.split("|") else emptyList()
        }

    override suspend fun clearSearchHistory() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEARCH_HISTORY] = ""
        }
    }

    override suspend fun updateDailyReadingGoal(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_READING_GOAL_MINUTES] = minutes
        }
    }

    override fun getDailyReadingGoal(): Flow<Int> = context.dataStore.data
        .retry(3) { it is IOException }
        .map { preferences ->
            preferences[PreferencesKeys.DAILY_READING_GOAL_MINUTES] ?: 30
        }

    override suspend fun updateWeeklyReadingGoal(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEEKLY_READING_GOAL_MINUTES] = minutes
        }
    }

    override fun getWeeklyReadingGoal(): Flow<Int> = context.dataStore.data
        .retry(3) { it is IOException }
        .map { preferences ->
            preferences[PreferencesKeys.WEEKLY_READING_GOAL_MINUTES] ?: 210
        }

    override suspend fun updateMonthlyReadingGoal(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONTHLY_READING_GOAL_MINUTES] = minutes
        }
    }

    override fun getMonthlyReadingGoal(): Flow<Int> = context.dataStore.data
        .retry(3) { it is IOException }
        .map { preferences ->
            preferences[PreferencesKeys.MONTHLY_READING_GOAL_MINUTES] ?: 900
        }

    override suspend fun updateReadingWidgetSnapshot(bookTitle: String, progressPercent: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIDGET_BOOK_TITLE] = bookTitle
            preferences[PreferencesKeys.WIDGET_PROGRESS_PERCENT] = progressPercent.coerceIn(0, 100)
        }
    }

    override fun isOnboardingCompleted(): Flow<Boolean> = context.dataStore.data
        .retry(3) { it is IOException }
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    override suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = true
        }
    }

    override suspend fun updateCustomFontPath(path: String?) {
        context.dataStore.edit { preferences ->
            if (path != null) {
                preferences[PreferencesKeys.CUSTOM_FONT_PATH] = path
            } else {
                preferences.remove(PreferencesKeys.CUSTOM_FONT_PATH)
            }
        }
    }

    override fun getCustomFontPath(): Flow<String?> = context.dataStore.data
        .retry(3) { it is IOException }
        .map { preferences ->
            preferences[PreferencesKeys.CUSTOM_FONT_PATH]
        }
}
