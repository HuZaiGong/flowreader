package com.flowreader.app.core.designsystem.theme

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

/**
 * Applies the in-app language, without recreating the Activity.
 *
 * `stringResource` resolves against `LocalContext.current.resources` and invalidates on
 * `LocalConfiguration`, so overriding both is enough to re-render the whole tree in the new
 * language while the navigation back stack survives.
 *
 * Two things this deliberately does *not* do:
 * - it never swaps `LocalContext` for a bare `createConfigurationContext()` result. That context
 *   does not wrap the Activity, and `hiltViewModel()` walks the `ContextWrapper` chain looking for
 *   one — losing it makes every screen crash on injection. [LocalizedContext] keeps the Activity
 *   as its base and overrides `getResources()` only.
 * - it does not localize anything outside composition (notifications, the home-screen widget).
 *   Those still follow the system language.
 */
@Composable
fun FlowLocaleProvider(languageTag: String?, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val localizedContext = remember(context, configuration, languageTag) {
        if (languageTag.isNullOrBlank()) {
            context
        } else {
            val locale = Locale.forLanguageTag(languageTag)
            val overrides = Configuration(configuration).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            LocalizedContext(context, overrides)
        }
    }

    val localizedConfiguration = localizedContext.resources.configuration
    val layoutDirection = if (localizedConfiguration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        LocalLayoutDirection provides layoutDirection,
        content = content
    )
}

private class LocalizedContext(base: Context, overrides: Configuration) : ContextWrapper(base) {
    private val localizedResources: Resources = base.createConfigurationContext(overrides).resources

    override fun getResources(): Resources = localizedResources
}
