package com.flowreader.app.data.local.dao

import androidx.room.*
import com.flowreader.app.data.local.entity.ReadingStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingStatsDao {
    @Query("SELECT * FROM reading_stats WHERE bookId = :bookId ORDER BY date DESC")
    fun getStatsByBookId(bookId: Long): Flow<List<ReadingStatsEntity>>

    @Query("SELECT * FROM reading_stats WHERE date = :date")
    suspend fun getStatsByDate(date: String): ReadingStatsEntity?

    @Query("SELECT SUM(readTimeSeconds) FROM reading_stats WHERE date = :date")
    suspend fun getTodayReadTime(date: String): Long?

    @Query("SELECT SUM(readPages) FROM reading_stats WHERE date = :date")
    suspend fun getTodayReadPages(date: String): Int?

    @Query("SELECT * FROM reading_stats ORDER BY date DESC LIMIT :limit")
    fun getRecentStats(limit: Int = 30): Flow<List<ReadingStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: ReadingStatsEntity): Long

    @Update
    suspend fun updateStats(stats: ReadingStatsEntity)

    @Query("DELETE FROM reading_stats WHERE bookId = :bookId")
    suspend fun deleteStatsByBookId(bookId: Long)

    @Query("SELECT SUM(readTimeSeconds) FROM reading_stats")
    suspend fun getTotalReadTime(): Long?

    @Query("SELECT SUM(readPages) FROM reading_stats")
    suspend fun getTotalReadPages(): Int?
}
