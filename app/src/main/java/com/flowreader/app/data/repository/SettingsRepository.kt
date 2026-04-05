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
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.IOException
import org.json.JSONObject
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
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val PARAGRAPH_SPACING = floatPreferencesKey("paragraph_spacing")
        val PARAGRAPH_SPACING_PRESET = stringPreferencesKey("paragraph_spacing_preset")
        val FIRST_LINE_INDENT = booleanPreferencesKey("first_line_indent")
        val JUSTIFY_TEXT = booleanPreferencesKey("justify_text")
        val READER_THEME = stringPreferencesKey("reader_theme")
        val AUTO_TIME_THEME = booleanPreferencesKey("auto_time_theme")
        val PAGE_MODE = stringPreferencesKey("page_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SCREEN_TIMEOUT_MINUTES = intPreferencesKey("screen_timeout_minutes")
        val TAP_ZONE_LEFT_ACTION = stringPreferencesKey("tap_zone_left_action")
        val TAP_ZONE_MIDDLE_ACTION = stringPreferencesKey("tap_zone_middle_action")
        val TAP_ZONE_RIGHT_ACTION = stringPreferencesKey("tap_zone_right_action")
        val TAP_ZONE_LEFT_RATIO = floatPreferencesKey("tap_zone_left_ratio")
        val TAP_ZONE_RIGHT_RATIO = floatPreferencesKey("tap_zone_right_ratio")
        val DOUBLE_FINGER_GESTURE = booleanPreferencesKey("double_finger_gesture")
        val LONG_PRESS_GESTURE = booleanPreferencesKey("long_press_gesture")
        val CONTROLS_AUTO_HIDE = booleanPreferencesKey("controls_auto_hide")
        val CONTROLS_HIDE_DELAY = intPreferencesKey("controls_hide_delay")
        val ENABLE_SPLASH_SCREEN = booleanPreferencesKey("enable_splash_screen")
        val ENABLE_BACKUP_REMINDER = booleanPreferencesKey("enable_backup_reminder")
        val BACKGROUND_TEXTURE = stringPreferencesKey("background_texture")
        val SOUND_EFFECT = stringPreferencesKey("sound_effect")
        val WORD_COUNT = intPreferencesKey("word_count")
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
                    fontFamily = try {
                        FontFamily.valueOf(preferences[PreferencesKeys.FONT_FAMILY] ?: FontFamily.DEFAULT.name)
                    } catch (e: Exception) {
                        FontFamily.DEFAULT
                    },
                    lineSpacing = preferences[PreferencesKeys.LINE_SPACING] ?: 1.5f,
                    paragraphSpacing = preferences[PreferencesKeys.PARAGRAPH_SPACING] ?: 1.0f,
                    paragraphSpacingPreset = try {
                        ParagraphSpacing.valueOf(preferences[PreferencesKeys.PARAGRAPH_SPACING_PRESET] ?: ParagraphSpacing.STANDARD.name)
                    } catch (e: Exception) {
                        ParagraphSpacing.STANDARD
                    },
                    firstLineIndent = preferences[PreferencesKeys.FIRST_LINE_INDENT] ?: true,
                    justifyText = preferences[PreferencesKeys.JUSTIFY_TEXT] ?: true,
                    theme = try {
                        ReaderTheme.valueOf(preferences[PreferencesKeys.READER_THEME] ?: ReaderTheme.LIGHT.name)
                    } catch (e: Exception) {
                        ReaderTheme.LIGHT
                    },
                    autoTimeTheme = preferences[PreferencesKeys.AUTO_TIME_THEME] ?: false,
                    pageMode = try {
                        PageMode.valueOf(preferences[PreferencesKeys.PAGE_MODE] ?: PageMode.SLIDE.name)
                    } catch (e: Exception) {
                        PageMode.SLIDE
                    },
                    keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: true,
                    screenTimeoutMinutes = preferences[PreferencesKeys.SCREEN_TIMEOUT_MINUTES] ?: 0,
                    tapZoneConfig = TapZoneConfig(
                        leftAction = try {
                            GestureAction.valueOf(preferences[PreferencesKeys.TAP_ZONE_LEFT_ACTION] ?: GestureAction.PREVIOUS_PAGE.name)
                        } catch (e: Exception) {
                            GestureAction.PREVIOUS_PAGE
                        },
                        middleAction = try {
                            GestureAction.valueOf(preferences[PreferencesKeys.TAP_ZONE_MIDDLE_ACTION] ?: GestureAction.SHOW_MENU.name)
                        } catch (e: Exception) {
                            GestureAction.SHOW_MENU
                        },
                        rightAction = try {
                            GestureAction.valueOf(preferences[PreferencesKeys.TAP_ZONE_RIGHT_ACTION] ?: GestureAction.NEXT_PAGE.name)
                        } catch (e: Exception) {
                            GestureAction.NEXT_PAGE
                        },
                        leftRatio = preferences[PreferencesKeys.TAP_ZONE_LEFT_RATIO] ?: 0.3f,
                        rightRatio = preferences[PreferencesKeys.TAP_ZONE_RIGHT_RATIO] ?: 0.3f
                    ),
                    doubleFingerGesture = preferences[PreferencesKeys.DOUBLE_FINGER_GESTURE] ?: true,
                    longPressGesture = preferences[PreferencesKeys.LONG_PRESS_GESTURE] ?: true,
                    controlsAutoHide = preferences[PreferencesKeys.CONTROLS_AUTO_HIDE] ?: true,
                    controlsHideDelaySeconds = preferences[PreferencesKeys.CONTROLS_HIDE_DELAY] ?: 3,
                    backgroundTexture = try {
                        BackgroundTexture.valueOf(preferences[PreferencesKeys.BACKGROUND_TEXTURE] ?: BackgroundTexture.SOLID.name)
                    } catch (e: Exception) {
                        BackgroundTexture.SOLID
                    },
                    soundEffect = try {
                        SoundEffect.valueOf(preferences[PreferencesKeys.SOUND_EFFECT] ?: SoundEffect.NONE.name)
                    } catch (e: Exception) {
                        SoundEffect.NONE
                    },
                    wordCount = preferences[PreferencesKeys.WORD_COUNT] ?: 0
                ),
                enableSplashScreen = preferences[PreferencesKeys.ENABLE_SPLASH_SCREEN] ?: true,
                enableBackupReminder = preferences[PreferencesKeys.ENABLE_BACKUP_REMINDER] ?: true
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
            preferences[PreferencesKeys.FONT_FAMILY] = settings.fontFamily.name
            preferences[PreferencesKeys.LINE_SPACING] = settings.lineSpacing
            preferences[PreferencesKeys.PARAGRAPH_SPACING] = settings.paragraphSpacing
            preferences[PreferencesKeys.PARAGRAPH_SPACING_PRESET] = settings.paragraphSpacingPreset.name
            preferences[PreferencesKeys.FIRST_LINE_INDENT] = settings.firstLineIndent
            preferences[PreferencesKeys.JUSTIFY_TEXT] = settings.justifyText
            preferences[PreferencesKeys.READER_THEME] = settings.theme.name
            preferences[PreferencesKeys.AUTO_TIME_THEME] = settings.autoTimeTheme
            preferences[PreferencesKeys.PAGE_MODE] = settings.pageMode.name
            preferences[PreferencesKeys.KEEP_SCREEN_ON] = settings.keepScreenOn
            preferences[PreferencesKeys.SCREEN_TIMEOUT_MINUTES] = settings.screenTimeoutMinutes
            preferences[PreferencesKeys.TAP_ZONE_LEFT_ACTION] = settings.tapZoneConfig.leftAction.name
            preferences[PreferencesKeys.TAP_ZONE_MIDDLE_ACTION] = settings.tapZoneConfig.middleAction.name
            preferences[PreferencesKeys.TAP_ZONE_RIGHT_ACTION] = settings.tapZoneConfig.rightAction.name
            preferences[PreferencesKeys.TAP_ZONE_LEFT_RATIO] = settings.tapZoneConfig.leftRatio
            preferences[PreferencesKeys.TAP_ZONE_RIGHT_RATIO] = settings.tapZoneConfig.rightRatio
            preferences[PreferencesKeys.DOUBLE_FINGER_GESTURE] = settings.doubleFingerGesture
            preferences[PreferencesKeys.LONG_PRESS_GESTURE] = settings.longPressGesture
            preferences[PreferencesKeys.CONTROLS_AUTO_HIDE] = settings.controlsAutoHide
            preferences[PreferencesKeys.CONTROLS_HIDE_DELAY] = settings.controlsHideDelaySeconds
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

    suspend fun updateTapZoneConfig(config: TapZoneConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TAP_ZONE_LEFT_ACTION] = config.leftAction.name
            preferences[PreferencesKeys.TAP_ZONE_MIDDLE_ACTION] = config.middleAction.name
            preferences[PreferencesKeys.TAP_ZONE_RIGHT_ACTION] = config.rightAction.name
            preferences[PreferencesKeys.TAP_ZONE_LEFT_RATIO] = config.leftRatio
            preferences[PreferencesKeys.TAP_ZONE_RIGHT_RATIO] = config.rightRatio
        }
    }

    suspend fun updateParagraphSpacingPreset(preset: ParagraphSpacing) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PARAGRAPH_SPACING_PRESET] = preset.name
            preferences[PreferencesKeys.PARAGRAPH_SPACING] = preset.value
        }
    }

    suspend fun updateFirstLineIndent(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_LINE_INDENT] = enabled
        }
    }

    suspend fun updateJustifyText(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.JUSTIFY_TEXT] = enabled
        }
    }

    suspend fun updateControlsAutoHide(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTROLS_AUTO_HIDE] = enabled
        }
    }

    suspend fun updateControlsHideDelay(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTROLS_HIDE_DELAY] = seconds
        }
    }

    suspend fun updateBackgroundTexture(texture: BackgroundTexture) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKGROUND_TEXTURE] = texture.name
        }
    }

    suspend fun updateSoundEffect(effect: SoundEffect) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_EFFECT] = effect.name
        }
    }

    suspend fun updateWordCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WORD_COUNT] = count
        }
    }

    suspend fun createBackup(): File {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        val backupFile = File(backupDir, "flowreader_backup_$timestamp.json")

        val preferences = context.dataStore.data.first()
        val backupJson = JSONObject()

        backupJson.put("version", "9.1.0")
        backupJson.put("timestamp", timestamp)
        backupJson.put("theme", preferences[PreferencesKeys.THEME] ?: ReaderTheme.SYSTEM.name)
        backupJson.put("fontSize", preferences[PreferencesKeys.FONT_SIZE] ?: 18)
        backupJson.put("fontFamily", preferences[PreferencesKeys.FONT_FAMILY] ?: FontFamily.DEFAULT.name)
        backupJson.put("lineSpacing", preferences[PreferencesKeys.LINE_SPACING] ?: 1.5f)
        backupJson.put("paragraphSpacing", preferences[PreferencesKeys.PARAGRAPH_SPACING] ?: 1.0f)
        backupJson.put("paragraphSpacingPreset", preferences[PreferencesKeys.PARAGRAPH_SPACING_PRESET] ?: ParagraphSpacing.STANDARD.name)
        backupJson.put("firstLineIndent", preferences[PreferencesKeys.FIRST_LINE_INDENT] ?: true)
        backupJson.put("justifyText", preferences[PreferencesKeys.JUSTIFY_TEXT] ?: true)
        backupJson.put("readerTheme", preferences[PreferencesKeys.READER_THEME] ?: ReaderTheme.LIGHT.name)
        backupJson.put("autoTimeTheme", preferences[PreferencesKeys.AUTO_TIME_THEME] ?: false)
        backupJson.put("pageMode", preferences[PreferencesKeys.PAGE_MODE] ?: PageMode.SLIDE.name)
        backupJson.put("keepScreenOn", preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: true)
        backupJson.put("screenTimeoutMinutes", preferences[PreferencesKeys.SCREEN_TIMEOUT_MINUTES] ?: 0)
        backupJson.put("tapZoneLeftAction", preferences[PreferencesKeys.TAP_ZONE_LEFT_ACTION] ?: GestureAction.PREVIOUS_PAGE.name)
        backupJson.put("tapZoneMiddleAction", preferences[PreferencesKeys.TAP_ZONE_MIDDLE_ACTION] ?: GestureAction.SHOW_MENU.name)
        backupJson.put("tapZoneRightAction", preferences[PreferencesKeys.TAP_ZONE_RIGHT_ACTION] ?: GestureAction.NEXT_PAGE.name)
        backupJson.put("tapZoneLeftRatio", preferences[PreferencesKeys.TAP_ZONE_LEFT_RATIO] ?: 0.3f)
        backupJson.put("tapZoneRightRatio", preferences[PreferencesKeys.TAP_ZONE_RIGHT_RATIO] ?: 0.3f)
        backupJson.put("doubleFingerGesture", preferences[PreferencesKeys.DOUBLE_FINGER_GESTURE] ?: true)
        backupJson.put("longPressGesture", preferences[PreferencesKeys.LONG_PRESS_GESTURE] ?: true)
        backupJson.put("controlsAutoHide", preferences[PreferencesKeys.CONTROLS_AUTO_HIDE] ?: true)
        backupJson.put("controlsHideDelay", preferences[PreferencesKeys.CONTROLS_HIDE_DELAY] ?: 3)
        backupJson.put("enableSplashScreen", preferences[PreferencesKeys.ENABLE_SPLASH_SCREEN] ?: true)
        backupJson.put("enableBackupReminder", preferences[PreferencesKeys.ENABLE_BACKUP_REMINDER] ?: true)

        backupFile.writeText(backupJson.toString())
        return backupFile
    }

    suspend fun restoreBackup(backupFile: File): Boolean {
        return try {
            val json = JSONObject(backupFile.readText())
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME] = json.optString("theme", ReaderTheme.SYSTEM.name)
                preferences[PreferencesKeys.FONT_SIZE] = json.optInt("fontSize", 18)
                preferences[PreferencesKeys.FONT_FAMILY] = json.optString("fontFamily", FontFamily.DEFAULT.name)
                preferences[PreferencesKeys.LINE_SPACING] = json.optDouble("lineSpacing", 1.5).toFloat()
                preferences[PreferencesKeys.PARAGRAPH_SPACING] = json.optDouble("paragraphSpacing", 1.0).toFloat()
                preferences[PreferencesKeys.PARAGRAPH_SPACING_PRESET] = json.optString("paragraphSpacingPreset", ParagraphSpacing.STANDARD.name)
                preferences[PreferencesKeys.FIRST_LINE_INDENT] = json.optBoolean("firstLineIndent", true)
                preferences[PreferencesKeys.JUSTIFY_TEXT] = json.optBoolean("justifyText", true)
                preferences[PreferencesKeys.READER_THEME] = json.optString("readerTheme", ReaderTheme.LIGHT.name)
                preferences[PreferencesKeys.AUTO_TIME_THEME] = json.optBoolean("autoTimeTheme", false)
                preferences[PreferencesKeys.PAGE_MODE] = json.optString("pageMode", PageMode.SLIDE.name)
                preferences[PreferencesKeys.KEEP_SCREEN_ON] = json.optBoolean("keepScreenOn", true)
                preferences[PreferencesKeys.SCREEN_TIMEOUT_MINUTES] = json.optInt("screenTimeoutMinutes", 0)
                preferences[PreferencesKeys.TAP_ZONE_LEFT_ACTION] = json.optString("tapZoneLeftAction", GestureAction.PREVIOUS_PAGE.name)
                preferences[PreferencesKeys.TAP_ZONE_MIDDLE_ACTION] = json.optString("tapZoneMiddleAction", GestureAction.SHOW_MENU.name)
                preferences[PreferencesKeys.TAP_ZONE_RIGHT_ACTION] = json.optString("tapZoneRightAction", GestureAction.NEXT_PAGE.name)
                preferences[PreferencesKeys.TAP_ZONE_LEFT_RATIO] = json.optDouble("tapZoneLeftRatio", 0.3).toFloat()
                preferences[PreferencesKeys.TAP_ZONE_RIGHT_RATIO] = json.optDouble("tapZoneRightRatio", 0.3).toFloat()
                preferences[PreferencesKeys.DOUBLE_FINGER_GESTURE] = json.optBoolean("doubleFingerGesture", true)
                preferences[PreferencesKeys.LONG_PRESS_GESTURE] = json.optBoolean("longPressGesture", true)
                preferences[PreferencesKeys.CONTROLS_AUTO_HIDE] = json.optBoolean("controlsAutoHide", true)
                preferences[PreferencesKeys.CONTROLS_HIDE_DELAY] = json.optInt("controlsHideDelay", 3)
                preferences[PreferencesKeys.ENABLE_SPLASH_SCREEN] = json.optBoolean("enableSplashScreen", true)
                preferences[PreferencesKeys.ENABLE_BACKUP_REMINDER] = json.optBoolean("enableBackupReminder", true)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
