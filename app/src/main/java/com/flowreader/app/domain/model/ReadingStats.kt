package com.flowreader.app.domain.model

import java.util.Date

data class ReadingStats(
    val id: Long = 0,
    val bookId: Long,
    val date: String,
    val readPages: Int = 0,
    val readChapters: Int = 0,
    val readTimeSeconds: Long = 0,
    val lastReadTime: Date = Date()
)

data class DailyStats(
    val date: String,
    val totalReadTime: Long,
    val totalReadPages: Int,
    val booksRead: Int
)

data class ReadingSummary(
    val totalReadTime: Long,
    val totalReadPages: Int,
    val totalBooks: Int,
    val currentStreak: Int,
    val longestStreak: Int
)
