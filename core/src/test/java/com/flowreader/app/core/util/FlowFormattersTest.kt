package com.flowreader.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlowFormattersTest {

    @Test
    fun durationPicksTheCoarsestUsefulUnit() {
        assertEquals(FlowFormatters.DurationParts.Seconds(0), FlowFormatters.durationParts(0))
        assertEquals(FlowFormatters.DurationParts.Seconds(45), FlowFormatters.durationParts(45))
        assertEquals(FlowFormatters.DurationParts.Minutes(1), FlowFormatters.durationParts(95))
        assertEquals(FlowFormatters.DurationParts.Hours(1), FlowFormatters.durationParts(3600))
        assertEquals(FlowFormatters.DurationParts.HoursMinutes(1, 2), FlowFormatters.durationParts(3_720))
    }

    @Test
    fun durationNeverReportsNegativeTime() {
        assertEquals(FlowFormatters.DurationParts.Seconds(0), FlowFormatters.durationParts(-90))
    }

    @Test
    fun durationDropsAZeroMinuteRemainderRatherThanSayingZeroMinutes() {
        // 2h exactly, and 2h00m05s: both are Hours, not HoursMinutes(2, 0).
        assertEquals(FlowFormatters.DurationParts.Hours(2), FlowFormatters.durationParts(7_200))
        assertEquals(FlowFormatters.DurationParts.Hours(2), FlowFormatters.durationParts(7_205))
    }

    @Test
    fun durationPartsCarryNoUnitWords() {
        // The whole point of the v56.5.1 split: :core must not emit a localizable word. If someone
        // reintroduces a String return here, the units stop following the app language again.
        val parts = FlowFormatters.durationParts(3_720)
        assertEquals(FlowFormatters.DurationParts.HoursMinutes(1, 2), parts)
    }

    @Test
    fun minutesTruncatesTowardsZero() {
        assertEquals(0, FlowFormatters.minutes(59))
        assertEquals(1, FlowFormatters.minutes(60))
        assertEquals(2, FlowFormatters.minutes(179))
    }

    @Test
    fun percentRoundsAndClamps() {
        assertEquals("0%", FlowFormatters.percent(-1f))
        assertEquals("42%", FlowFormatters.percent(0.4237f))
        assertEquals("100%", FlowFormatters.percent(4f))
    }

    @Test
    fun precisePercentKeepsOneDecimal() {
        assertEquals("42.4%", FlowFormatters.percentPrecise(0.4237f))
        assertEquals("0.0%", FlowFormatters.percentPrecise(0f))
        assertEquals("100.0%", FlowFormatters.percentPrecise(1f))
    }

    @Test
    fun shortDateFormatsIsoDates() {
        assertEquals("07/20", FlowFormatters.shortDate("2026-07-20"))
    }

    @Test
    fun shortDatePassesThroughUnparseableInput() {
        assertEquals("not-a-date", FlowFormatters.shortDate("not-a-date"))
        assertEquals("2026/07/20", FlowFormatters.shortDate("2026/07/20"))
        assertEquals("2026-7-2", FlowFormatters.shortDate("2026-7-2"))
    }

    @Test
    fun spokenDatePartsSplitsMonthAndDay() {
        assertEquals(FlowFormatters.SpokenDate(7, 20), FlowFormatters.spokenDateParts("2026-07-20"))
        // Single-digit forms parse too; only the phrasing is the caller's problem.
        assertEquals(FlowFormatters.SpokenDate(7, 2), FlowFormatters.spokenDateParts("2026-7-2"))
    }

    @Test
    fun spokenDatePartsReturnsNullForUnparseableInput() {
        // null rather than an exception: the caller falls back to showing the raw string, which is
        // what the removed spokenDate() did by returning its input unchanged.
        assertNull(FlowFormatters.spokenDateParts("oops"))
        assertNull(FlowFormatters.spokenDateParts("2026-07"))
        assertNull(FlowFormatters.spokenDateParts("2026-ab-20"))
    }
}
