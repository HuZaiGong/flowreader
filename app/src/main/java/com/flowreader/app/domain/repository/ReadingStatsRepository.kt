package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.BookStats
import com.flowreader.app.domain.model.DailyStats
import com.flowreader.app.domain.model.ReadingStats
import kotlinx.coroutines.flow.Flow

interface ReadingStatsRepository {
    suspend fun insertStats(stats: ReadingStats): Long
    fun getStatsByBookId(bookId: Long, limit: Int = 30): Flow<List<ReadingStats>>
    fun getStatsByDateRange(startDate: Long): Flow<List<ReadingStats>>
    fun getAllStats(limit: Int = 100): Flow<List<ReadingStats>>
    suspend fun getTotalReadTimeByBookId(bookId: Long): Int
    suspend fun getTotalPagesByBookId(bookId: Long): Int
    fun getDailyStats(startDate: Long): Flow<List<DailyStats>>
    suspend fun getBookStats(bookId: Long): BookStats?
    fun getAllBookStats(): Flow<List<BookStats>>
    suspend fun deleteStatsByBookId(bookId: Long)
}