package com.flowreader.app.data.repository

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
    private val readingStatsDao: ReadingStatsDao
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
        
        return ReadingSummary(
            totalReadTime = totalTime,
            totalReadPages = totalPages,
            totalBooks = 0,
            currentStreak = 0,
            longestStreak = 0
        )
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
