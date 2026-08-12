package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.dao.ReadingStatsDao
import com.flowreader.app.data.local.entity.BookEntity
import com.flowreader.app.data.local.entity.ReadingStatsEntity
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReadingStatsRepositoryImplTest {
    private val statsDao = mockk<ReadingStatsDao>()
    private val bookDao = mockk<BookDao>()
    private val repository = ReadingStatsRepositoryImpl(statsDao, bookDao)

    @Test
    fun getRecentDailyStats_aggregatesRowsByDate() = runTest {
        val today = LocalDate.now().toString()
        coEvery { statsDao.getStatsSince(any()) } returns flowOf(
            listOf(
                ReadingStatsEntity(bookId = 1L, date = today, readPages = 2, readTimeSeconds = 60),
                ReadingStatsEntity(bookId = 2L, date = today, readPages = 3, readTimeSeconds = 120)
            )
        )

        val daily = repository.getRecentDailyStats(7)
        daily.collect { rows ->
            assertEquals(1, rows.size)
            assertEquals(5, rows.first().totalReadPages)
            assertEquals(180L, rows.first().totalReadTime)
            assertEquals(2, rows.first().booksRead)
        }
    }

    /**
     * One row per book per day means a row `LIMIT` is not a day limit. Three books read every day
     * used to collapse a 7-day request into the two or three most recent days.
     */
    @Test
    fun getRecentDailyStats_countsDaysNotRows() = runTest {
        val today = LocalDate.now()
        val rows = (0 until 7).flatMap { dayOffset ->
            val date = today.minusDays(dayOffset.toLong()).toString()
            (1L..3L).map { bookId ->
                ReadingStatsEntity(bookId = bookId, date = date, readPages = 1, readTimeSeconds = 60)
            }
        }
        coEvery { statsDao.getStatsSince(any()) } returns flowOf(rows)

        repository.getRecentDailyStats(7).collect { daily ->
            assertEquals(7, daily.size)
            assertEquals(3, daily.first().booksRead)
        }
    }

    /** The DAO must be asked for a date range covering exactly [limit] days, today inclusive. */
    @Test
    fun getRecentDailyStats_asksForAnInclusiveSevenDayWindow() = runTest {
        val requested = slot<String>()
        coEvery { statsDao.getStatsSince(capture(requested)) } returns flowOf(emptyList())

        repository.getRecentDailyStats(7).collect { }

        assertEquals(LocalDate.now().minusDays(6).toString(), requested.captured)
    }

    /** A nonsensical limit must not build a future start date that filters everything out. */
    @Test
    fun getRecentDailyStats_clampsANonPositiveLimit() = runTest {
        val requested = slot<String>()
        coEvery { statsDao.getStatsSince(capture(requested)) } returns flowOf(
            listOf(ReadingStatsEntity(bookId = 1L, date = LocalDate.now().toString(), readPages = 1, readTimeSeconds = 60))
        )

        repository.getRecentDailyStats(0).collect { daily ->
            assertEquals(1, daily.size)
        }

        assertEquals(LocalDate.now().toString(), requested.captured)
    }

    @Test
    fun getReadingReport_findsMostReadBook() = runTest {
        // Date must sit inside the 7-day window regardless of when CI runs.
        val recentDate = java.time.LocalDate.now().minusDays(1).toString()
        coEvery { statsDao.getAllStats() } returns listOf(
            ReadingStatsEntity(bookId = 9L, date = recentDate, readPages = 8, readTimeSeconds = 300)
        )
        coEvery { bookDao.getBookById(9L) } returns BookEntity(id = 9L, title = "Book", author = "Author", filePath = "")

        val report = repository.getReadingReport(7)

        assertEquals("Book", report.mostReadBookTitle)
        assertEquals(8, report.totalReadPages)
    }
}
