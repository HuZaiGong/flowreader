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
    }

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
                    screenTimeoutMinutes = preferences[PreferencesKeys.SCREEN_TIMEOUT_MINUTES] ?: 0
                )
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
}
