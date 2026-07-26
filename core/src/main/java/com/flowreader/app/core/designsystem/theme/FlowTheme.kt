package com.flowreader.app.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.flowreader.app.core.designsystem.token.FlowDarkColorScheme
import com.flowreader.app.core.designsystem.token.FlowLightColorScheme
import com.flowreader.app.core.designsystem.token.FlowShapes
import com.flowreader.app.core.designsystem.token.FlowTypography
import com.flowreader.app.domain.model.AppThemeMode
import com.flowreader.app.domain.model.ColorSource

/** Resolves [AppThemeMode] against the platform setting. */
@Composable
@ReadOnlyComposable
fun AppThemeMode.isDark(): Boolean = when (this) {
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
    AppThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
}

/**
 * The one and only theme wrapper. Applied once at the navigation root — never per screen.
 *
 * [colorSource] replaces the pre-v52 behaviour where Android 12+ silently forced wallpaper
 * dynamic color and the brand palette was unreachable. The default is now [ColorSource.BRAND].
 */
@Composable
fun FlowTheme(
    themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    colorSource: ColorSource = ColorSource.BRAND,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode.isDark()
    val context = LocalContext.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        colorSource == ColorSource.DYNAMIC && dynamicAvailable && darkTheme -> dynamicDarkColorScheme(context)
        colorSource == ColorSource.DYNAMIC && dynamicAvailable -> dynamicLightColorScheme(context)
        darkTheme -> FlowDarkColorScheme
        else -> FlowLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FlowTypography,
        shapes = FlowShapes,
        content = content
    )
}
