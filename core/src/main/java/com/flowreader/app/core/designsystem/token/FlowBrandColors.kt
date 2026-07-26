package com.flowreader.app.core.designsystem.token

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * FlowReader's brand palette. Before v52 these 50 values were dead on every Android 12+ device
 * because the theme unconditionally forced wallpaper dynamic color; they are now the default and
 * dynamic color is an explicit opt-in (`ColorSource.DYNAMIC`).
 */
object FlowBrandColors {
    val LightPrimary = Color(0xFF6750A4)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFEADDFF)
    val LightOnPrimaryContainer = Color(0xFF21005D)
    val LightSecondary = Color(0xFF625B71)
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightSecondaryContainer = Color(0xFFE8DEF8)
    val LightOnSecondaryContainer = Color(0xFF1D192B)
    val LightTertiary = Color(0xFF7D5260)
    val LightOnTertiary = Color(0xFFFFFFFF)
    val LightTertiaryContainer = Color(0xFFFFD8E4)
    val LightOnTertiaryContainer = Color(0xFF31111D)
    val LightError = Color(0xFFB3261E)
    val LightOnError = Color(0xFFFFFFFF)
    val LightErrorContainer = Color(0xFFF9DEDC)
    val LightOnErrorContainer = Color(0xFF410E0B)
    val LightBackground = Color(0xFFFFFBFE)
    val LightOnBackground = Color(0xFF1C1B1F)
    val LightSurface = Color(0xFFFFFBFE)
    val LightOnSurface = Color(0xFF1C1B1F)
    val LightSurfaceVariant = Color(0xFFE7E0EC)
    val LightOnSurfaceVariant = Color(0xFF49454F)
    val LightOutline = Color(0xFF79747E)
    val LightOutlineVariant = Color(0xFFCAC4D0)

    val DarkPrimary = Color(0xFFD0BCFF)
    val DarkOnPrimary = Color(0xFF381E72)
    val DarkPrimaryContainer = Color(0xFF4F378B)
    val DarkOnPrimaryContainer = Color(0xFFEADDFF)
    val DarkSecondary = Color(0xFFCCC2DC)
    val DarkOnSecondary = Color(0xFF332D41)
    val DarkSecondaryContainer = Color(0xFF4A4458)
    val DarkOnSecondaryContainer = Color(0xFFE8DEF8)
    val DarkTertiary = Color(0xFFEFB8C8)
    val DarkOnTertiary = Color(0xFF492532)
    val DarkTertiaryContainer = Color(0xFF633B48)
    val DarkOnTertiaryContainer = Color(0xFFFFD8E4)
    val DarkError = Color(0xFFF2B8B5)
    val DarkOnError = Color(0xFF601410)
    val DarkErrorContainer = Color(0xFF8C1D18)
    val DarkOnErrorContainer = Color(0xFFF9DEDC)
    val DarkBackground = Color(0xFF1C1B1F)
    val DarkOnBackground = Color(0xFFE6E1E5)
    val DarkSurface = Color(0xFF1C1B1F)
    val DarkOnSurface = Color(0xFFE6E1E5)
    val DarkSurfaceVariant = Color(0xFF49454F)
    val DarkOnSurfaceVariant = Color(0xFFCAC4D0)
    val DarkOutline = Color(0xFF938F99)
    val DarkOutlineVariant = Color(0xFF49454F)
}

val FlowLightColorScheme = lightColorScheme(
    primary = FlowBrandColors.LightPrimary,
    onPrimary = FlowBrandColors.LightOnPrimary,
    primaryContainer = FlowBrandColors.LightPrimaryContainer,
    onPrimaryContainer = FlowBrandColors.LightOnPrimaryContainer,
    secondary = FlowBrandColors.LightSecondary,
    onSecondary = FlowBrandColors.LightOnSecondary,
    secondaryContainer = FlowBrandColors.LightSecondaryContainer,
    onSecondaryContainer = FlowBrandColors.LightOnSecondaryContainer,
    tertiary = FlowBrandColors.LightTertiary,
    onTertiary = FlowBrandColors.LightOnTertiary,
    tertiaryContainer = FlowBrandColors.LightTertiaryContainer,
    onTertiaryContainer = FlowBrandColors.LightOnTertiaryContainer,
    error = FlowBrandColors.LightError,
    onError = FlowBrandColors.LightOnError,
    errorContainer = FlowBrandColors.LightErrorContainer,
    onErrorContainer = FlowBrandColors.LightOnErrorContainer,
    background = FlowBrandColors.LightBackground,
    onBackground = FlowBrandColors.LightOnBackground,
    surface = FlowBrandColors.LightSurface,
    onSurface = FlowBrandColors.LightOnSurface,
    surfaceVariant = FlowBrandColors.LightSurfaceVariant,
    onSurfaceVariant = FlowBrandColors.LightOnSurfaceVariant,
    outline = FlowBrandColors.LightOutline,
    outlineVariant = FlowBrandColors.LightOutlineVariant
)

val FlowDarkColorScheme = darkColorScheme(
    primary = FlowBrandColors.DarkPrimary,
    onPrimary = FlowBrandColors.DarkOnPrimary,
    primaryContainer = FlowBrandColors.DarkPrimaryContainer,
    onPrimaryContainer = FlowBrandColors.DarkOnPrimaryContainer,
    secondary = FlowBrandColors.DarkSecondary,
    onSecondary = FlowBrandColors.DarkOnSecondary,
    secondaryContainer = FlowBrandColors.DarkSecondaryContainer,
    onSecondaryContainer = FlowBrandColors.DarkOnSecondaryContainer,
    tertiary = FlowBrandColors.DarkTertiary,
    onTertiary = FlowBrandColors.DarkOnTertiary,
    tertiaryContainer = FlowBrandColors.DarkTertiaryContainer,
    onTertiaryContainer = FlowBrandColors.DarkOnTertiaryContainer,
    error = FlowBrandColors.DarkError,
    onError = FlowBrandColors.DarkOnError,
    errorContainer = FlowBrandColors.DarkErrorContainer,
    onErrorContainer = FlowBrandColors.DarkOnErrorContainer,
    background = FlowBrandColors.DarkBackground,
    onBackground = FlowBrandColors.DarkOnBackground,
    surface = FlowBrandColors.DarkSurface,
    onSurface = FlowBrandColors.DarkOnSurface,
    surfaceVariant = FlowBrandColors.DarkSurfaceVariant,
    onSurfaceVariant = FlowBrandColors.DarkOnSurfaceVariant,
    outline = FlowBrandColors.DarkOutline,
    outlineVariant = FlowBrandColors.DarkOutlineVariant
)
