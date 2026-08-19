package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppColorPresetTest {

    @Test
    fun twelvePresetsAreOffered() {
        assertEquals(12, AppColorPreset.entries.size)
    }

    @Test
    fun violetStaysTheDefaultSoExistingInstallsDoNotShift() {
        // The default must remain the historical brand seed: :core maps VIOLET to the hand-tuned
        // FlowLight/DarkColorScheme verbatim, so changing this default would recolor every install
        // that never touched the setting.
        assertEquals(AppColorPreset.VIOLET, AppColorPreset.DEFAULT)
        assertEquals(AppColorPreset.VIOLET, AppColorPreset.fromStoredName(null))
        assertEquals(AppColorPreset.VIOLET, AppColorPreset.fromStoredName("NOT_A_PRESET"))
    }

    @Test
    fun storedNamesRoundTrip() {
        AppColorPreset.entries.forEach { preset ->
            assertEquals(preset, AppColorPreset.fromStoredName(preset.name))
        }
    }

    @Test
    fun everyPresetHasADisplayName() {
        assertTrue(AppColorPreset.entries.all { it.displayName.isNotBlank() })
    }

    @Test
    fun displayNamesAreDistinct() {
        // A duplicate label would make two swatches in the settings grid indistinguishable to
        // TalkBack, which reads the label as the swatch's only content description.
        val names = AppColorPreset.entries.map { it.displayName }
        assertEquals(names.size, names.distinct().size)
    }
}
