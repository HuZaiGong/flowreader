package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorSourceTest {

    @Test
    fun everySourceIsARealChoice() {
        // Three values as of v56.6, and each reads a different AppSettings field: BRAND takes
        // colorPreset, CUSTOM takes customSeedArgb, DYNAMIC takes neither. A "follow system" option
        // is still deliberately absent — it would be indistinguishable from DYNAMIC at runtime,
        // i.e. another fake switch.
        assertEquals(
            listOf(ColorSource.BRAND, ColorSource.DYNAMIC, ColorSource.CUSTOM),
            ColorSource.entries.toList()
        )
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
