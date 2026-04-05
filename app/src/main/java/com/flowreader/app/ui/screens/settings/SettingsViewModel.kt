package com.flowreader.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.*
import com.flowreader.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val appTheme: ReaderTheme = ReaderTheme.SYSTEM,
    val readingSettings: ReadingSettings = ReadingSettings(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.appSettings.collect { settings ->
                _uiState.update {
                    it.copy(
                        appTheme = settings.theme,
                        readingSettings = settings.defaultReadingSettings,
                        isLoading = false
                    )
                }
            }
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
}
