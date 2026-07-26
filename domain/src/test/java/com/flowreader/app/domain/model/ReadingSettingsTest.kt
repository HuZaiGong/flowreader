package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingSettingsTest {
    @Test
    fun defaultReadingSettingsMatchReaderDefaults() {
        val settings = ReadingSettings()

        assertEquals(18, settings.fontSize)
        assertEquals(1.5f, settings.lineSpacing)
        assertEquals(ReaderTheme.LIGHT, settings.theme)
        assertEquals(PageMode.SLIDE, settings.pageMode)
        assertTrue(settings.keepScreenOn)
        assertEquals(20, settings.eyeProtectionIntervalMinutes)
    }
}
