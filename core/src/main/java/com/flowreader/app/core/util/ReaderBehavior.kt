package com.flowreader.app.core.util

import com.flowreader.app.domain.model.GestureAction
import com.flowreader.app.domain.model.ReadingSettings

/**
 * Pure reader behaviour rules.
 *
 * Both helpers here back settings that were persisted but never consulted before v52: the tap
 * zones ignored everything except `tapZoneRatio`, and auto night mode was sampled once during
 * composition so 19:00 never actually arrived while the reader was open.
 */
object ReaderBehavior {

    const val NIGHT_START_HOUR = 19
    const val NIGHT_END_HOUR = 7

    /** True inside the 19:00–07:00 window that auto night mode switches on. */
    fun isNightHour(hourOfDay: Int): Boolean {
        val hour = hourOfDay.coerceIn(0, 23)
        return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR
    }

    /**
     * Which configured action a tap at [offsetX] maps to.
     *
     * With edge gestures on, the left/right hot zones are the configured edge widths (percent of
     * the screen). With them off we fall back to symmetric zones sized by `tapZoneRatio`.
     */
    fun tapAction(offsetX: Float, width: Float, settings: ReadingSettings): GestureAction {
        if (width <= 0f) return settings.gestureSettings.middleTapAction
        val gestures = settings.gestureSettings
        val leftBound: Float
        val rightBound: Float
        if (gestures.edgeGestureEnabled) {
            leftBound = width * (gestures.leftEdgeWidth.coerceIn(0, 45) / 100f)
            rightBound = width * (1f - gestures.rightEdgeWidth.coerceIn(0, 45) / 100f)
        } else {
            val halfZone = width * settings.tapZoneRatio.coerceIn(0.1f, 0.45f)
            leftBound = width / 2f - halfZone
            rightBound = width / 2f + halfZone
        }
        return when {
            offsetX < leftBound -> gestures.leftTapAction
            offsetX > rightBound -> gestures.rightTapAction
            else -> gestures.middleTapAction
        }
    }

    /** Horizontal fling to action. Negative [dragAmount] means a leftward swipe. */
    fun swipeAction(dragAmount: Float, settings: ReadingSettings, threshold: Float = 80f): GestureAction {
        val gestures = settings.gestureSettings
        return when {
            dragAmount <= -threshold -> gestures.swipeLeftAction
            dragAmount >= threshold -> gestures.swipeRightAction
            else -> GestureAction.NONE
        }
    }
}
