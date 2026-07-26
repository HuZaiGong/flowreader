package com.flowreader.app.core.util

import kotlin.math.roundToInt

/**
 * Shared display formatting. These used to be copy-pasted per screen — `StatsScreen` even carried
 * two byte-identical `formatDate` helpers, one of which was never called.
 */
object FlowFormatters {

    /** `95` -> `"1分钟"`, `3720` -> `"1小时2分钟"`. */
    fun duration(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0)
        return when {
            safe < 60 -> "${safe}秒"
            safe < 3600 -> "${safe / 60}分钟"
            safe % 3600 < 60 -> "${safe / 3600}小时"
            else -> "${safe / 3600}小时${(safe % 3600) / 60}分钟"
        }
    }

    /** Minutes-only variant used by goal copy. */
    fun minutes(seconds: Long): Int = (seconds.coerceAtLeast(0) / 60).toInt()

    /** `0.4237f` -> `"42%"`. */
    fun percent(fraction: Float): String {
        val clamped = fraction.coerceIn(0f, 1f)
        return "${(clamped * 100).roundToInt()}%"
    }

    /** `0.4237f` -> `"42.4%"` for the reader progress readout. */
    fun percentPrecise(fraction: Float): String {
        val clamped = fraction.coerceIn(0f, 1f)
        val tenths = (clamped * 1000).roundToInt()
        return "${tenths / 10}.${tenths % 10}%"
    }

    /** `"2026-07-20"` -> `"07/20"`. Returns the input unchanged when it is not an ISO date. */
    fun shortDate(isoDate: String): String {
        val parts = isoDate.split("-")
        if (parts.size != 3) return isoDate
        val month = parts[1]
        val day = parts[2]
        if (month.length != 2 || day.length != 2) return isoDate
        if (!month.all { it.isDigit() } || !day.all { it.isDigit() }) return isoDate
        return "$month/$day"
    }

    /** `"2026-07-20"` -> `"7 月 20 日"`, used for chart accessibility labels. */
    fun spokenDate(isoDate: String): String {
        val parts = isoDate.split("-")
        if (parts.size != 3) return isoDate
        val month = parts[1].toIntOrNull() ?: return isoDate
        val day = parts[2].toIntOrNull() ?: return isoDate
        return "$month 月 $day 日"
    }
}
