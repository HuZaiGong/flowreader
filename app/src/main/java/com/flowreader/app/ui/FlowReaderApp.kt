package com.flowreader.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flowreader.app.ui.screens.auth.AuthScreen
import com.flowreader.app.ui.screens.library.LibraryScreen
import com.flowreader.app.ui.screens.onboarding.OnboardingScreen
import com.flowreader.app.ui.screens.settings.SettingsViewModel

sealed class AppRoute(val route: String) {
    object Onboarding : AppRoute("onboarding")
    object Auth : AppRoute("auth")
    object Main : AppRoute("main")
}

@Composable
fun FlowReaderApp(
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    var showOnboarding by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsViewModel.checkOnboardingStatus()
    }

    LaunchedEffect(settingsState.isOnboardingCompleted) {
        showOnboarding = !settingsState.isOnboardingCompleted
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            showOnboarding -> {
                OnboardingScreen(
                    onComplete = {
                        settingsViewModel.completeOnboarding()
                        showOnboarding = false
                        showAuth = true
                    }
                )
            }
            showAuth -> {
                AuthScreen(
                    onAuthSuccess = {
                        showAuth = false
                    },
                    onSkip = {
                        showAuth = false
                    }
                )
            }
            else -> {
                FlowReaderNavHost()
            }
        }
    }
}
