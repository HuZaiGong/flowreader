package com.flowreader.app.core.util

import kotlin.math.roundToInt

/**
 * Shared display formatting. These used to be copy-pasted per screen — `StatsScreen` even carried
 * two byte-identical `formatDate` helpers, one of which was never called.
 */
object FlowFormatters {

    /**
     * A duration broken into the coarsest useful unit, without any words attached.
     *
     * `:core` is deliberately Compose- and Context-free, so it cannot reach for a localized string.
     * Until v56.5.1 `duration()` returned `"1小时2分钟"` directly, which meant every localized string
     * that embedded it still read Chinese units inside a German or Russian sentence. The unit words
     * now come from the caller's `stringResource`; this function only decides *which* unit applies.
     */
    sealed interface DurationParts {
        data class Seconds(val seconds: Long) : DurationParts
        data class Minutes(val minutes: Long) : DurationParts
        data class Hours(val hours: Long) : DurationParts
        data class HoursMinutes(val hours: Long, val minutes: Long) : DurationParts
    }

    /** `95` -> `Minutes(1)`, `3720` -> `HoursMinutes(1, 2)`. Negative input is treated as zero. */
    fun durationParts(seconds: Long): DurationParts {
        val safe = seconds.coerceAtLeast(0)
        return when {
            safe < 60 -> DurationParts.Seconds(safe)
            safe < 3600 -> DurationParts.Minutes(safe / 60)
            safe % 3600 < 60 -> DurationParts.Hours(safe / 3600)
            else -> DurationParts.HoursMinutes(safe / 3600, (safe % 3600) / 60)
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

    /** Month and day of an ISO date, for callers that need to phrase it themselves. */
    data class SpokenDate(val month: Int, val day: Int)

    /**
     * `"2026-07-20"` -> `SpokenDate(7, 20)`, or null when the input is not an ISO date.
     *
     * Used for chart accessibility labels. Same reason as [durationParts] for returning parts
     * instead of text: the phrasing ("7 月 20 日" vs "20 July") belongs to a localized resource.
     */
    fun spokenDateParts(isoDate: String): SpokenDate? {
        val parts = isoDate.split("-")
        if (parts.size != 3) return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        return SpokenDate(month, day)
    }
}
