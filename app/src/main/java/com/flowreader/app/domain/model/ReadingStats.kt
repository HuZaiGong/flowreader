package com.flowreader.app.domain.model

import java.util.Date

data class ReadingStats(
    val id: Long = 0,
    val bookId: Long,
    val date: Date,
    val readDurationMinutes: Int = 0,
    val pagesRead: Int = 0,
    val chaptersRead: Int = 0
)

data class DailyStats(
    val date: String,
    val totalReadTime: Int,
    val totalPages: Int
)

data class BookStats(
    val bookId: Long,
    val bookTitle: String,
    val totalReadTime: Int,
    val totalChapters: Int,
    val totalPages: Int,
    val lastReadDate: Date?
)