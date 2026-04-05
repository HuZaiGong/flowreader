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
    val importResult: String? = null
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
                settingsRepository.getDailyReadingGoal()
            ) { settings, goal ->
                SettingsUiState(
                    appTheme = settings.theme,
                    readingSettings = settings.defaultReadingSettings,
                    isLoading = false,
                    autoTimeTheme = settings.autoTimeTheme,
                    readingReminderEnabled = settings.readingReminderEnabled,
                    readingReminderHour = settings.readingReminderHour,
                    readingReminderMinute = settings.readingReminderMinute,
                    dailyReadingGoal = goal
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(exportResult = "备份功能正在开发中") }
        }
    }

    fun importData() {
        viewModelScope.launch {
            _uiState.update { it.copy(importResult = "恢复功能正在开发中") }
        }
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
}
