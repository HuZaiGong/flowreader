package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.dao.ReadingStatsDao
import com.flowreader.app.data.local.entity.ReadingStatsEntity
import com.flowreader.app.domain.model.DailyStats
import com.flowreader.app.domain.model.ReadingStats
import com.flowreader.app.domain.model.ReadingSummary
import com.flowreader.app.domain.repository.ReadingStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingStatsRepositoryImpl @Inject constructor(
    private val readingStatsDao: ReadingStatsDao,
    private val bookDao: BookDao
) : ReadingStatsRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun getStatsByBookId(bookId: Long): Flow<List<ReadingStats>> {
        return readingStatsDao.getStatsByBookId(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTodayStats(): ReadingStats? {
        val today = dateFormat.format(Date())
        return readingStatsDao.getStatsByDate(today)?.toDomain()
    }

    override suspend fun getTodayReadTime(): Long {
        val today = dateFormat.format(Date())
        return readingStatsDao.getTodayReadTime(today) ?: 0L
    }

    override suspend fun getTodayReadPages(): Int {
        val today = dateFormat.format(Date())
        return readingStatsDao.getTodayReadPages(today) ?: 0
    }

    override fun getRecentStats(limit: Int): Flow<List<ReadingStats>> {
        return readingStatsDao.getRecentStats(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateTodayStats(bookId: Long, readPages: Int, readTimeSeconds: Long) {
        val today = dateFormat.format(Date())
        val existing = readingStatsDao.getStatsByDate(today)

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

    /**
     * 计算当前连续阅读天数
     */
    private suspend fun calculateCurrentStreak(): Int {
        val allStats = readingStatsDao.getAllStats()
        if (allStats.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var streak = 0
        var checkDate = calendar.time

        // 从今天开始往前检查
        for (i in 0 until 365) {
            val dateStr = dateFormat.format(checkDate)
            val hasReadOnThisDay = allStats.any { it.date == dateStr && it.readTimeSeconds > 0 }

            if (hasReadOnThisDay) {
                streak++
            } else if (i > 0) {
                // 如果今天没读但之前连续，说明断了
                break
            } else if (i == 0 && !hasReadOnThisDay) {
                // 今天还没读，检查昨天
                continue
            } else {
                break
            }

            calendar.add(Calendar.DAY_OF_YEAR, -1)
            checkDate = calendar.time
        }

        return streak
    }

    /**
     * 计算最长连续阅读天数
     */
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
            val currentDate = dateFormat.parse(sortedDates[i])
            val previousDate = dateFormat.parse(sortedDates[i - 1])

            if (currentDate != null && previousDate != null) {
                val diffDays = ((currentDate.time - previousDate.time) / (1000 * 60 * 60 * 24)).toInt()
                if (diffDays == 1) {
                    currentStreak++
                    maxStreak = maxOf(maxStreak, currentStreak)
                } else {
                    currentStreak = 1
                }
            }
        }

        return maxStreak
    }

    override fun getRecentDailyStats(limit: Int): Flow<List<DailyStats>> {
        return readingStatsDao.getRecentStats(limit).map { entities ->
            entities.map { entity ->
                DailyStats(
                    date = entity.date,
                    totalReadTime = entity.readTimeSeconds,
                    totalReadPages = entity.readPages,
                    booksRead = 1
                )
            }
        }
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
