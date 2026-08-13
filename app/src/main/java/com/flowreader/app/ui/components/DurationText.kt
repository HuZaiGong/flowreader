package com.flowreader.app.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.flowreader.app.R
import com.flowreader.app.core.util.FlowFormatters

/**
 * Localized rendering of a duration in seconds.
 *
 * `FlowFormatters` lives in `:core`, which is deliberately Compose- and Context-free, so it returns
 * [FlowFormatters.DurationParts] and the unit words come from resources here. Before v56.5.1 the
 * formatter returned `"1小时2分钟"` itself, so every localized string that embedded a duration still
 * read Chinese units inside a German or Russian sentence.
 *
 * These are `Context` extensions rather than composables because one caller — the stats chart's
 * TalkBack description — builds its text inside `remember`, where `stringResource` is unavailable.
 * Keeping a single implementation is what stops the four unit branches from being written twice.
 */
fun Context.durationString(seconds: Long): String =
    when (val parts = FlowFormatters.durationParts(seconds)) {
        is FlowFormatters.DurationParts.Seconds -> getString(R.string.duration_seconds, parts.seconds)
        is FlowFormatters.DurationParts.Minutes -> getString(R.string.duration_minutes, parts.minutes)
        is FlowFormatters.DurationParts.Hours -> getString(R.string.duration_hours, parts.hours)
        is FlowFormatters.DurationParts.HoursMinutes ->
            getString(R.string.duration_hours_minutes, parts.hours, parts.minutes)
    }

/**
 * Localized month-and-day phrasing for chart accessibility labels, falling back to the raw ISO
 * string when it cannot be parsed — the same contract the removed `FlowFormatters.spokenDate` had.
 */
fun Context.spokenDateString(isoDate: String): String {
    val parts = FlowFormatters.spokenDateParts(isoDate) ?: return isoDate
    return getString(R.string.spoken_date, parts.month, parts.day)
}

/** Composable convenience wrapper around [durationString]. */
@Composable
fun durationText(seconds: Long): String = LocalContext.current.durationString(seconds)
