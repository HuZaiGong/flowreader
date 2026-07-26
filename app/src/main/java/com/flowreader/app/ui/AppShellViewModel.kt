package com.flowreader.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.AppSettings
import com.flowreader.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Supplies the app shell (theme mode + color source) to [FlowReaderNavHost].
 *
 * Before v52 `Navigation.kt` reached into the settings DataStore directly and parsed the raw
 * `theme` key itself, which made that key load-bearing in two unrelated places.
 */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = settingsRepository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
}
