package com.flowreader.app.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.*
import com.flowreader.app.data.repository.SettingsRepository
import com.flowreader.app.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val appTheme: ReaderTheme = ReaderTheme.SYSTEM,
    val readingSettings: ReadingSettings = ReadingSettings(),
    val isLoading: Boolean = true,
    val autoTimeTheme: Boolean = false,
    val readingReminderEnabled: Boolean = false,
    val readingReminderHour: Int = 20,
    val readingReminderMinute: Int = 0,
    val dailyReadingGoal: Int = 30,
    val exportResult: String? = null,
    val importResult: String? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isOnboardingCompleted: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
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
                settingsRepository.isOnboardingCompleted()
            ) { settings, goal, onboardingCompleted ->
                SettingsUiState(
                    appTheme = settings.theme,
                    readingSettings = settings.defaultReadingSettings,
                    isLoading = false,
                    autoTimeTheme = settings.autoTimeTheme,
                    readingReminderEnabled = settings.readingReminderEnabled,
                    readingReminderHour = settings.readingReminderHour,
                    readingReminderMinute = settings.readingReminderMinute,
                    dailyReadingGoal = goal,
                    isOnboardingCompleted = onboardingCompleted
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

    fun updateAppTheme(theme: ReaderTheme) {
        viewModelScope.launch {
            settingsRepository.updateTheme(theme)
        }
    }

    fun updateReaderTheme(theme: ReaderTheme) {
        viewModelScope.launch {
            settingsRepository.updateReaderTheme(theme)
        }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            settingsRepository.updateReadingSettings(currentSettings.copy(fontSize = size))
        }
    }

    fun updateLineSpacing(spacing: Float) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            settingsRepository.updateReadingSettings(currentSettings.copy(lineSpacing = spacing))
        }
    }

    fun updatePageMode(mode: PageMode) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            settingsRepository.updateReadingSettings(currentSettings.copy(pageMode = mode))
        }
    }

    fun updateKeepScreenOn(keepOn: Boolean) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            settingsRepository.updateReadingSettings(currentSettings.copy(keepScreenOn = keepOn))
        }
    }

    fun updateScreenTimeout(minutes: Int) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            settingsRepository.updateReadingSettings(currentSettings.copy(screenTimeoutMinutes = minutes))
        }
    }

    fun updateAutoTimeTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoTimeTheme(enabled)
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
}
