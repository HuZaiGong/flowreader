package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.DailyStats
import com.flowreader.app.domain.model.ReadingStats
import com.flowreader.app.domain.model.ReadingSummary
import kotlinx.coroutines.flow.Flow

interface ReadingStatsRepository {
    fun getStatsByBookId(bookId: Long): Flow<List<ReadingStats>>
    suspend fun getTodayStats(): ReadingStats?
    suspend fun getTodayReadTime(): Long
    suspend fun getTodayReadPages(): Int
    fun getRecentStats(limit: Int = 30): Flow<List<ReadingStats>>
    suspend fun updateTodayStats(bookId: Long, readPages: Int, readTimeSeconds: Long)
    suspend fun deleteStatsByBookId(bookId: Long)
    suspend fun getTotalReadTime(): Long
    suspend fun getTotalReadPages(): Int
    suspend fun getReadingSummary(): ReadingSummary
    fun getRecentDailyStats(limit: Int = 7): Flow<List<DailyStats>>
}
