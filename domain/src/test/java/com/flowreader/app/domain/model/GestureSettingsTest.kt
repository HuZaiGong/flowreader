package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureSettingsTest {
    @Test
    fun defaultGesturesMapReaderTapZones() {
        val gestures = GestureSettings()

        assertEquals(GestureAction.PREVIOUS_PAGE, gestures.leftTapAction)
        assertEquals(GestureAction.TOGGLE_CONTROLS, gestures.middleTapAction)
        assertEquals(GestureAction.NEXT_PAGE, gestures.rightTapAction)
        assertEquals(GestureAction.ADD_BOOKMARK, gestures.longPressAction)
        assertTrue(gestures.edgeGestureEnabled)
    }
}
