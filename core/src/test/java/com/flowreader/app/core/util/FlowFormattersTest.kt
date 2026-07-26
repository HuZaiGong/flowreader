package com.flowreader.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FlowFormattersTest {

    @Test
    fun durationPicksTheCoarsestUsefulUnit() {
        assertEquals("0秒", FlowFormatters.duration(0))
        assertEquals("45秒", FlowFormatters.duration(45))
        assertEquals("1分钟", FlowFormatters.duration(95))
        assertEquals("1小时", FlowFormatters.duration(3600))
        assertEquals("1小时2分钟", FlowFormatters.duration(3_720))
    }

    @Test
    fun durationNeverReportsNegativeTime() {
        assertEquals("0秒", FlowFormatters.duration(-90))
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
    fun spokenDateReadsNaturally() {
        assertEquals("7 月 20 日", FlowFormatters.spokenDate("2026-07-20"))
        assertEquals("oops", FlowFormatters.spokenDate("oops"))
    }
}
