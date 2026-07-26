package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.dao.ReadingStatsDao
import com.flowreader.app.data.local.entity.ReadingStatsEntity
import com.flowreader.app.domain.model.DailyStats
import com.flowreader.app.domain.model.ReadingStats
import com.flowreader.app.domain.model.ReadingSummary
import com.flowreader.app.domain.model.ReadingReport
import com.flowreader.app.domain.repository.ReadingStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingStatsRepositoryImpl @Inject constructor(
    private val readingStatsDao: ReadingStatsDao,
    private val bookDao: BookDao
) : ReadingStatsRepository {

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun getStatsByBookId(bookId: Long): Flow<List<ReadingStats>> {
        return readingStatsDao.getStatsByBookId(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTodayStats(): ReadingStats? {
        val today = LocalDate.now().format(dateFormat)
        return readingStatsDao.getStatsByDateOnly(today)?.toDomain()
    }

    override suspend fun getTodayReadTime(): Long {
        val today = LocalDate.now().format(dateFormat)
        return readingStatsDao.getTodayReadTime(today) ?: 0L
    }

    override suspend fun getTodayReadPages(): Int {
        val today = LocalDate.now().format(dateFormat)
        return readingStatsDao.getTodayReadPages(today) ?: 0
    }

    override fun getRecentStats(limit: Int): Flow<List<ReadingStats>> {
        return readingStatsDao.getRecentStats(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateTodayStats(bookId: Long, readPages: Int, readTimeSeconds: Long) {
        val today = LocalDate.now().format(dateFormat)
        val existing = readingStatsDao.getStatsByDate(bookId, today)

        if (existing != null) {
            readingStatsDao.updateStats(
                existing.copy(
                    readPages = existing.readPages + readPages,
                    readTimeSeconds = existing.readTimeSeconds + readTimeSeconds,
                    lastReadTime = System.currentTimeMillis()
                )
            )
        } else {
            readingStatsDao.insertStats(
                ReadingStatsEntity(
                    bookId = bookId,
                    date = today,
                    readPages = readPages,
                    readTimeSeconds = readTimeSeconds,
                    lastReadTime = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun deleteStatsByBookId(bookId: Long) {
        readingStatsDao.deleteStatsByBookId(bookId)
    }

    override suspend fun getTotalReadTime(): Long {
        return readingStatsDao.getTotalReadTime() ?: 0L
    }

    override suspend fun getTotalReadPages(): Int {
        return readingStatsDao.getTotalReadPages() ?: 0
    }

    override suspend fun getReadingSummary(): ReadingSummary {
        val totalTime = getTotalReadTime()
        val totalPages = getTotalReadPages()
        val totalBooks = bookDao.getBookCount()
        val currentStreak = calculateCurrentStreak()
        val longestStreak = calculateLongestStreak()

        return ReadingSummary(
            totalReadTime = totalTime,
            totalReadPages = totalPages,
            totalBooks = totalBooks,
            currentStreak = currentStreak,
            longestStreak = longestStreak
        )
    }

    private suspend fun calculateCurrentStreak(): Int {
        val allStats = readingStatsDao.getAllStats()
        if (allStats.isEmpty()) return 0

        val today = LocalDate.now()
        val hasReadToday = allStats.any {
            it.date == today.format(dateFormat) && it.readTimeSeconds > 0
        }

        var streak = 0
        var checkDate = if (hasReadToday) today else today.minusDays(1)

        for (i in 0 until 365) {
            val dateStr = checkDate.format(dateFormat)
            val hasReadOnThisDay = allStats.any { it.date == dateStr && it.readTimeSeconds > 0 }
            if (!hasReadOnThisDay) break
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }

    private suspend fun calculateLongestStreak(): Int {
        val allStats = readingStatsDao.getAllStats()
        if (allStats.isEmpty()) return 0

        val sortedDates = allStats
            .filter { it.readTimeSeconds > 0 }
            .map { it.date }
            .distinct()
            .sorted()

        if (sortedDates.isEmpty()) return 0

        var maxStreak = 1
        var currentStreak = 1

        for (i in 1 until sortedDates.size) {
            val currentDate = LocalDate.parse(sortedDates[i], dateFormat)
            val previousDate = LocalDate.parse(sortedDates[i - 1], dateFormat)
            val diffDays = ChronoUnit.DAYS.between(previousDate, currentDate)

            if (diffDays == 1L) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }

        return maxStreak
    }

    override fun getRecentDailyStats(limit: Int): Flow<List<DailyStats>> {
        return readingStatsDao.getRecentStats(limit).map { entities ->
            entities.groupBy { it.date }.map { (date, rows) ->
                DailyStats(
                    date = date,
                    totalReadTime = rows.sumOf { it.readTimeSeconds },
                    totalReadPages = rows.sumOf { it.readPages },
                    booksRead = rows.map { it.bookId }.distinct().size
                )
            }.sortedBy { it.date }.takeLast(limit)
        }
    }

    override suspend fun getReadingReport(days: Int): ReadingReport {
        val startDate = LocalDate.now().minusDays((days - 1).toLong())
        val allStats = readingStatsDao.getAllStats().filter {
            LocalDate.parse(it.date, dateFormat) >= startDate
        }
        val dailyStats = allStats.groupBy { it.date }.map { (date, rows) ->
            DailyStats(
                date = date,
                totalReadTime = rows.sumOf { it.readTimeSeconds },
                totalReadPages = rows.sumOf { it.readPages },
                booksRead = rows.map { it.bookId }.distinct().size
            )
        }.sortedBy { it.date }
        val mostReadBookId = allStats.groupBy { it.bookId }
            .maxByOrNull { entry -> entry.value.sumOf { it.readTimeSeconds } }
            ?.key
        val mostReadBookTitle = mostReadBookId?.let { bookDao.getBookById(it)?.title }

        return ReadingReport(
            rangeLabel = if (days <= 7) "本周" else "本月",
            totalReadTime = allStats.sumOf { it.readTimeSeconds },
            totalReadPages = allStats.sumOf { it.readPages },
            fastestReadingDay = dailyStats.maxByOrNull { it.totalReadPages },
            mostReadBookTitle = mostReadBookTitle,
            dailyStats = dailyStats
        )
    }

    private fun ReadingStatsEntity.toDomain(): ReadingStats {
        return ReadingStats(
            id = id,
            bookId = bookId,
            date = date,
            readPages = readPages,
            readChapters = readChapters,
            readTimeSeconds = readTimeSeconds,
            lastReadTime = Date(lastReadTime)
        )
    }
}
