package com.flowreader.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.AppLanguage
import com.flowreader.app.domain.model.AppThemeMode
import com.flowreader.app.domain.model.ColorSource
import com.flowreader.app.domain.model.GestureSettings
import com.flowreader.app.domain.model.ReadingSettings
import com.flowreader.app.domain.repository.SettingsRepository
import com.flowreader.app.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    val colorSource: ColorSource = ColorSource.BRAND,
    val language: AppLanguage = AppLanguage.FOLLOW_SYSTEM,
    val readingSettings: ReadingSettings = ReadingSettings(),
    val isLoading: Boolean = true,
    val readingReminderEnabled: Boolean = false,
    val readingReminderHour: Int = 20,
    val readingReminderMinute: Int = 0,
    val dailyReadingGoal: Int = 30,
    val exportResult: String? = null,
    val importResult: String? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val customFontPath: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.appSettings,
                settingsRepository.getDailyReadingGoal(),
                settingsRepository.isOnboardingCompleted(),
                settingsRepository.getCustomFontPath()
            ) { settings, goal, onboardingCompleted, fontPath ->
                SettingsUiState(
                    themeMode = settings.themeMode,
                    colorSource = settings.colorSource,
                    language = settings.language,
                    readingSettings = settings.defaultReadingSettings,
                    isLoading = false,
                    readingReminderEnabled = settings.readingReminderEnabled,
                    readingReminderHour = settings.readingReminderHour,
                    readingReminderMinute = settings.readingReminderMinute,
                    dailyReadingGoal = goal,
                    isOnboardingCompleted = onboardingCompleted,
                    customFontPath = fontPath
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun checkOnboardingStatus() {
        viewModelScope.launch {
            settingsRepository.isOnboardingCompleted().collect { completed ->
                _uiState.update { it.copy(isOnboardingCompleted = completed) }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted()
        }
    }

    fun exportData() {
        _uiState.update { it.copy(isExporting = true, exportResult = null) }
    }

    fun onExportReady(uri: Uri) {
        viewModelScope.launch {
            backupRepository.exportData(uri)
                .onSuccess {
                    _uiState.update { it.copy(isExporting = false, exportResult = "备份成功") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isExporting = false, exportResult = "备份失败: ${e.message}") }
                }
        }
    }

    fun importData() {
        _uiState.update { it.copy(isImporting = true, importResult = null) }
    }

    fun onImportReady(uri: Uri) {
        viewModelScope.launch {
            backupRepository.importData(uri)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importResult = "导入成功: ${result.booksImported}本书, ${result.bookmarksImported}个书签"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isImporting = false, importResult = "导入失败: ${e.message}") }
                }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }

    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }

    fun updateColorSource(source: ColorSource) {
        viewModelScope.launch {
            settingsRepository.updateColorSource(source)
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.updateLanguage(language)
        }
    }

    fun updateKeepScreenOn(keepOn: Boolean) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            settingsRepository.updateReadingSettings(currentSettings.copy(keepScreenOn = keepOn))
        }
    }

    fun updateReadingReminder(enabled: Boolean, hour: Int = 20, minute: Int = 0) {
        viewModelScope.launch {
            settingsRepository.updateReadingReminder(enabled, hour, minute)
        }
    }

    fun updateDailyReadingGoal(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateDailyReadingGoal(minutes)
        }
    }

    fun updateGestureSettings(gestureSettings: GestureSettings) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            settingsRepository.updateReadingSettings(currentSettings.copy(gestureSettings = gestureSettings))
        }
    }

    fun onCustomFontSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@launch
                val fontDir = java.io.File(context.filesDir, "fonts")
                if (!fontDir.exists()) fontDir.mkdirs()
                val fileName = "custom_font_${System.currentTimeMillis()}.ttf"
                val targetFile = java.io.File(fontDir, fileName)
                java.io.FileOutputStream(targetFile).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()
                settingsRepository.updateCustomFontPath(targetFile.absolutePath)
                val currentSettings = _uiState.value.readingSettings
                settingsRepository.updateReadingSettings(
                    currentSettings.copy(customFontPath = targetFile.absolutePath)
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(exportResult = "导入字体失败: ${e.message}") }
            }
        }
    }

    fun clearCustomFont() {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            settingsRepository.updateCustomFontPath(null)
            settingsRepository.updateReadingSettings(
                currentSettings.copy(customFontPath = null)
            )
        }
    }
}
