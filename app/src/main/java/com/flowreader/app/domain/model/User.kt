package com.flowreader.app.domain.model

import java.util.Date

data class User(
    val id: Long = 0,
    val username: String,
    val email: String? = null,
    val phone: String? = null,
    val createdAt: Date = Date(),
    val lastLoginAt: Date? = null,
    val syncEnabled: Boolean = false
)

data class SyncData(
    val books: List<Book> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val annotations: List<Annotation> = emptyList(),
    val readingProgress: Map<Long, ReadingProgress> = emptyMap(),
    val lastSyncTime: Long = System.currentTimeMillis()
)

data class ReadingProgress(
    val bookId: Long,
    val currentChapter: Int,
    val currentPosition: Int,
    val readingProgress: Float,
    val lastReadTime: Long
)
