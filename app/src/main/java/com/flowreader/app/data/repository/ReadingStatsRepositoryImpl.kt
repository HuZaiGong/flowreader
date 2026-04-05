package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.DailyStatsResult
import com.flowreader.app.data.local.dao.ReadingStatsDao
import com.flowreader.app.data.local.entity.toEntity
import com.flowreader.app.data.local.entity.toDomain
import com.flowreader.app.domain.model.BookStats
import com.flowreader.app.domain.model.DailyStats
import com.flowreader.app.domain.model.ReadingStats
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.ReadingStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingStatsRepositoryImpl @Inject constructor(
    private val readingStatsDao: ReadingStatsDao,
    private val bookRepository: BookRepository
) : ReadingStatsRepository {

    override suspend fun insertStats(stats: ReadingStats): Long {
        return readingStatsDao.insertStats(stats.toEntity())
    }

    override fun getStatsByBookId(bookId: Long, limit: Int): Flow<List<ReadingStats>> {
        return readingStatsDao.getStatsByBookId(bookId, limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getStatsByDateRange(startDate: Long): Flow<List<ReadingStats>> {
        return readingStatsDao.getStatsByDateRange(startDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllStats(limit: Int): Flow<List<ReadingStats>> {
        return readingStatsDao.getAllStats(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getTotalReadTimeByBookId(bookId: Long): Int {
        return readingStatsDao.getTotalReadTimeByBookId(bookId) ?: 0
    }

    override suspend fun getTotalPagesByBookId(bookId: Long): Int {
        return readingStatsDao.getTotalPagesByBookId(bookId) ?: 0
    }

    override fun getDailyStats(startDate: Long): Flow<List<DailyStats>> {
        return readingStatsDao.getDailyStats(startDate).map { list ->
            list.map { result ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                DailyStats(
                    date = dateFormat.format(Date(result.day)),
                    totalReadTime = result.totalTime,
                    totalPages = result.totalPages
                )
            }
        }
    }

    override suspend fun getBookStats(bookId: Long): BookStats? {
        val book = bookRepository.getBookById(bookId) ?: return null
        val totalReadTime = getTotalReadTimeByBookId(bookId)
        val totalPages = getTotalPagesByBookId(bookId)
        
        return BookStats(
            bookId = bookId,
            bookTitle = book.title,
            totalReadTime = totalReadTime,
            totalChapters = book.currentChapter + 1,
            totalPages = totalPages,
            lastReadDate = book.lastReadTime
        )
    }

    override fun getAllBookStats(): Flow<List<BookStats>> {
        return bookRepository.getAllBooks().map { books ->
            books.map { book ->
                BookStats(
                    bookId = book.id,
                    bookTitle = book.title,
                    totalReadTime = 0,
                    totalChapters = book.totalChapters,
                    totalPages = (book.readingProgress * 100).toInt(),
                    lastReadDate = book.lastReadTime
                )
            }
        }
    }

    override suspend fun deleteStatsByBookId(bookId: Long) {
        readingStatsDao.deleteStatsByBookId(bookId)
    }
}