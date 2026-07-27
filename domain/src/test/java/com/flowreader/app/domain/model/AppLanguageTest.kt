package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {

    @Test
    fun unknownAndMissingValuesFallBackToTheSystemLanguage() {
        assertEquals(AppLanguage.FOLLOW_SYSTEM, AppLanguage.fromStoredName(null))
        assertEquals(AppLanguage.FOLLOW_SYSTEM, AppLanguage.fromStoredName(""))
        assertEquals(AppLanguage.FOLLOW_SYSTEM, AppLanguage.fromStoredName("KLINGON"))
    }

    @Test
    fun storedNamesRoundTrip() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromStoredName(language.name))
        }
    }

    @Test
    fun onlyFollowSystemHasANullTag() {
        assertNull(AppLanguage.FOLLOW_SYSTEM.tag)
        AppLanguage.entries.filter { it != AppLanguage.FOLLOW_SYSTEM }.forEach { language ->
            assertNotNull("${language.name} must pin a locale", language.tag)
        }
    }

    @Test
    fun everyTagMatchesAShippedResourceQualifier() {
        // values/ (zh) plus values-en, values-ja, values-ko. Adding an entry here without the
        // matching folder gives the user a language that silently renders as Chinese.
        val shipped = setOf("zh-CN", "en", "ja", "ko")
        AppLanguage.entries.mapNotNull { it.tag }.forEach { tag ->
            assertTrue("no resource folder for $tag", tag in shipped)
        }
    }

    @Test
    fun displayNamesAreNeverBlank() {
        AppLanguage.entries.forEach { language ->
            assertTrue(language.displayName.isNotBlank())
        }
    }
}
