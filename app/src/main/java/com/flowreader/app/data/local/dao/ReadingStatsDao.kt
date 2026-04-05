package com.flowreader.app.data.local.dao

import androidx.room.*
import com.flowreader.app.data.local.entity.ReadingStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: ReadingStatsEntity): Long

    @Query("SELECT * FROM reading_stats WHERE bookId = :bookId ORDER BY date DESC LIMIT :limit")
    fun getStatsByBookId(bookId: Long, limit: Int = 30): Flow<List<ReadingStatsEntity>>

    @Query("SELECT * FROM reading_stats WHERE date >= :startDate ORDER BY date DESC")
    fun getStatsByDateRange(startDate: Long): Flow<List<ReadingStatsEntity>>

    @Query("SELECT * FROM reading_stats ORDER BY date DESC LIMIT :limit")
    fun getAllStats(limit: Int = 100): Flow<List<ReadingStatsEntity>>

    @Query("SELECT SUM(readDurationMinutes) FROM reading_stats WHERE bookId = :bookId")
    suspend fun getTotalReadTimeByBookId(bookId: Long): Int?

    @Query("SELECT SUM(pagesRead) FROM reading_stats WHERE bookId = :bookId")
    suspend fun getTotalPagesByBookId(bookId: Long): Int?

    @Query("""
        SELECT date / 86400000 * 86400000 as day, 
               SUM(readDurationMinutes) as totalTime, 
               SUM(pagesRead) as totalPages 
        FROM reading_stats 
        WHERE date >= :startDate 
        GROUP BY day 
        ORDER BY day DESC
    """)
    fun getDailyStats(startDate: Long): Flow<List<DailyStatsResult>>

    @Query("DELETE FROM reading_stats WHERE bookId = :bookId")
    suspend fun deleteStatsByBookId(bookId: Long)
}

data class DailyStatsResult(
    val day: Long,
    val totalTime: Int,
    val totalPages: Int
)