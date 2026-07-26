package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeModeTest {

    @Test
    fun followSystemIsAvailableAlongsideTheExplicitModes() {
        assertEquals(3, AppThemeMode.entries.size)
        assertTrue(AppThemeMode.entries.contains(AppThemeMode.FOLLOW_SYSTEM))
    }

    @Test
    fun preV52ThemeKeyValuesStillParse() {
        // The DataStore `theme` key held ReaderTheme.LIGHT / DARK before v52.
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStoredName("LIGHT"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromStoredName("DARK"))
    }

    @Test
    fun unknownValuesFollowTheSystem() {
        assertEquals(AppThemeMode.FOLLOW_SYSTEM, AppThemeMode.fromStoredName(null))
        assertEquals(AppThemeMode.FOLLOW_SYSTEM, AppThemeMode.fromStoredName("SEPIA"))
    }

    @Test
    fun everyModeHasADisplayName() {
        assertTrue(AppThemeMode.entries.all { it.displayName.isNotBlank() })
    }
}
