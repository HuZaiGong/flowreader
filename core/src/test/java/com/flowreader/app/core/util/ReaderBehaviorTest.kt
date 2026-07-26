package com.flowreader.app.core.util

import com.flowreader.app.domain.model.GestureAction
import com.flowreader.app.domain.model.GestureSettings
import com.flowreader.app.domain.model.ReadingSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderBehaviorTest {

    private val defaults = ReadingSettings()

    @Test
    fun nightWindowCoversEveningThroughEarlyMorning() {
        assertTrue(ReaderBehavior.isNightHour(19))
        assertTrue(ReaderBehavior.isNightHour(23))
        assertTrue(ReaderBehavior.isNightHour(0))
        assertTrue(ReaderBehavior.isNightHour(6))
        assertFalse(ReaderBehavior.isNightHour(7))
        assertFalse(ReaderBehavior.isNightHour(18))
    }

    @Test
    fun edgeZonesDriveTheConfiguredTapActions() {
        val action = ReaderBehavior.tapAction(offsetX = 50f, width = 1000f, settings = defaults)
        assertEquals(GestureAction.PREVIOUS_PAGE, action)
        assertEquals(GestureAction.NEXT_PAGE, ReaderBehavior.tapAction(950f, 1000f, defaults))
        assertEquals(GestureAction.TOGGLE_CONTROLS, ReaderBehavior.tapAction(500f, 1000f, defaults))
    }

    @Test
    fun customEdgeWidthsAreHonoured() {
        val settings = defaults.copy(
            gestureSettings = GestureSettings(leftEdgeWidth = 40, rightEdgeWidth = 5)
        )
        assertEquals(GestureAction.PREVIOUS_PAGE, ReaderBehavior.tapAction(350f, 1000f, settings))
        assertEquals(GestureAction.TOGGLE_CONTROLS, ReaderBehavior.tapAction(450f, 1000f, settings))
        assertEquals(GestureAction.NEXT_PAGE, ReaderBehavior.tapAction(970f, 1000f, settings))
    }

    @Test
    fun tapZoneRatioTakesOverWhenEdgeGesturesAreOff() {
        val settings = defaults.copy(
            tapZoneRatio = 0.1f,
            gestureSettings = GestureSettings(edgeGestureEnabled = false)
        )
        // Zones are symmetric around the middle: 0.4*width and 0.6*width.
        assertEquals(GestureAction.PREVIOUS_PAGE, ReaderBehavior.tapAction(300f, 1000f, settings))
        assertEquals(GestureAction.TOGGLE_CONTROLS, ReaderBehavior.tapAction(500f, 1000f, settings))
        assertEquals(GestureAction.NEXT_PAGE, ReaderBehavior.tapAction(700f, 1000f, settings))
    }

    @Test
    fun customTapActionsAreActuallyConsulted() {
        val settings = defaults.copy(
            gestureSettings = GestureSettings(
                leftTapAction = GestureAction.SHOW_TOC,
                middleTapAction = GestureAction.NONE,
                rightTapAction = GestureAction.ADD_BOOKMARK
            )
        )
        assertEquals(GestureAction.SHOW_TOC, ReaderBehavior.tapAction(10f, 1000f, settings))
        assertEquals(GestureAction.NONE, ReaderBehavior.tapAction(500f, 1000f, settings))
        assertEquals(GestureAction.ADD_BOOKMARK, ReaderBehavior.tapAction(990f, 1000f, settings))
    }

    @Test
    fun zeroWidthFallsBackToTheMiddleAction() {
        assertEquals(GestureAction.TOGGLE_CONTROLS, ReaderBehavior.tapAction(0f, 0f, defaults))
    }

    @Test
    fun swipesOnlyFireBeyondTheThreshold() {
        assertEquals(GestureAction.NEXT_PAGE, ReaderBehavior.swipeAction(-200f, defaults))
        assertEquals(GestureAction.PREVIOUS_PAGE, ReaderBehavior.swipeAction(200f, defaults))
        assertEquals(GestureAction.NONE, ReaderBehavior.swipeAction(10f, defaults))
        assertEquals(GestureAction.NONE, ReaderBehavior.swipeAction(-10f, defaults))
    }
}
