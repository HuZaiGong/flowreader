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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.flowreader.app.core.designsystem.token.FlowColorPresets
import com.flowreader.app.core.designsystem.token.FlowShapes
import com.flowreader.app.core.designsystem.token.FlowTypography
import com.flowreader.app.domain.model.AppColorPreset
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
 * dynamic color and the brand palette was unreachable. The default is still [ColorSource.BRAND].
 *
 * v56.6 gave the two non-wallpaper sources something to vary: BRAND renders one of the 12
 * [AppColorPreset]s and CUSTOM generates a scheme from [customSeedArgb]. A CUSTOM source with a null
 * seed falls back to [colorPreset] rather than rendering nothing.
 */
@Composable
fun FlowTheme(
    themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    colorSource: ColorSource = ColorSource.BRAND,
    colorPreset: AppColorPreset = AppColorPreset.DEFAULT,
    customSeedArgb: Long? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode.isDark()
    val context = LocalContext.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Insurance, not a live bug fix — and worth keeping only because the failure mode is severe.
    //
    // Every branch below except the two brand singletons builds a **new** ColorScheme instance;
    // `ColorScheme` does not override `equals` (it declares only `toString`), and `MaterialTheme`
    // publishes it through a `staticCompositionLocalOf`, whose contract is to recompose the entire
    // subtree when the provided value changes. So if this composable's body ever re-runs with
    // unchanged colour inputs, the whole app UI rebuilds.
    //
    // Today it cannot: the compiler reports `FlowTheme` as `restartable skippable` with all five
    // parameters `stable` (`./gradlew :core:compileDebugKotlin -PcomposeReports=true`), so Compose
    // skips it outright when nothing changed. That guarantee is one careless parameter away from
    // gone — adding an unstable type or a non-memoized lambda would silently turn a skip into a
    // full-tree rebuild. `remember` makes the outcome hold either way, and `FlowThemeStabilityTest`
    // pins the outcome rather than the mechanism.
    val colorScheme = remember(colorSource, colorPreset, customSeedArgb, darkTheme, dynamicAvailable, context) {
        when {
            colorSource == ColorSource.DYNAMIC && dynamicAvailable && darkTheme -> dynamicDarkColorScheme(context)
            colorSource == ColorSource.DYNAMIC && dynamicAvailable -> dynamicLightColorScheme(context)
            colorSource == ColorSource.CUSTOM && customSeedArgb != null ->
                FlowColorPresets.schemeFromSeed(customSeedArgb, darkTheme)
            // DYNAMIC below Android 12, and CUSTOM with no seed yet, both land here on purpose.
            else -> FlowColorPresets.schemeOf(colorPreset, darkTheme)
        }
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
