package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingStatsModelTest {
    @Test
    fun readingReportKeepsAggregateFields() {
        val daily = DailyStats(date = "2026-07-26", totalReadTime = 1800L, totalReadPages = 24, booksRead = 2)
        val report = ReadingReport(
            rangeLabel = "weekly",
            totalReadTime = 1800L,
            totalReadPages = 24,
            fastestReadingDay = daily,
            mostReadBookTitle = null,
            dailyStats = listOf(daily)
        )

        assertEquals("weekly", report.rangeLabel)
        assertEquals(daily, report.fastestReadingDay)
        assertNull(report.mostReadBookTitle)
        assertEquals(listOf(daily), report.dailyStats)
    }
}
