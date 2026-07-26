package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorSourceTest {

    @Test
    fun bothSourcesAreRealChoices() {
        // Deliberately two values, not three: a "follow system" option would be
        // indistinguishable from DYNAMIC at runtime, i.e. another fake switch.
        assertEquals(listOf(ColorSource.BRAND, ColorSource.DYNAMIC), ColorSource.entries.toList())
    }

    @Test
    fun brandIsTheDefaultSoTheAppKeepsItsIdentity() {
        // v51 forced wallpaper dynamic color on every Android 12+ device with no way out.
        assertEquals(ColorSource.BRAND, ColorSource.fromStoredName(null))
        assertEquals(ColorSource.BRAND, ColorSource.fromStoredName("SOMETHING_ELSE"))
    }

    @Test
    fun storedNamesRoundTrip() {
        ColorSource.entries.forEach { source ->
            assertEquals(source, ColorSource.fromStoredName(source.name))
        }
    }

    @Test
    fun everySourceHasADisplayName() {
        assertTrue(ColorSource.entries.all { it.displayName.isNotBlank() })
    }
}
