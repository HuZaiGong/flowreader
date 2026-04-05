package com.flowreader.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_stats")
data class ReadingStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val date: String,
    val readPages: Int = 0,
    val readChapters: Int = 0,
    val readTimeSeconds: Long = 0,
    val lastReadTime: Long = System.currentTimeMillis()
)
