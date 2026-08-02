package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.dao.ReadingStatsDao
import com.flowreader.app.data.local.entity.BookEntity
import com.flowreader.app.data.local.entity.ReadingStatsEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingStatsRepositoryImplTest {
    private val statsDao = mockk<ReadingStatsDao>()
    private val bookDao = mockk<BookDao>()
    private val repository = ReadingStatsRepositoryImpl(statsDao, bookDao)

    @Test
    fun getRecentDailyStats_aggregatesRowsByDate() = runTest {
        coEvery { statsDao.getRecentStats(7) } returns flowOf(
            listOf(
                ReadingStatsEntity(bookId = 1L, date = "2026-07-26", readPages = 2, readTimeSeconds = 60),
                ReadingStatsEntity(bookId = 2L, date = "2026-07-26", readPages = 3, readTimeSeconds = 120)
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
