package com.flowreader.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "reading_stats")
data class ReadingStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val date: Long,
    val readDurationMinutes: Int = 0,
    val pagesRead: Int = 0,
    val chaptersRead: Int = 0
)

fun ReadingStatsEntity.toDomain() = com.flowreader.app.domain.model.ReadingStats(
    id = id,
    bookId = bookId,
    date = Date(date),
    readDurationMinutes = readDurationMinutes,
    pagesRead = pagesRead,
    chaptersRead = chaptersRead
)

fun com.flowreader.app.domain.model.ReadingStats.toEntity() = ReadingStatsEntity(
    id = id,
    bookId = bookId,
    date = date.time,
    readDurationMinutes = readDurationMinutes,
    pagesRead = pagesRead,
    chaptersRead = chaptersRead
)