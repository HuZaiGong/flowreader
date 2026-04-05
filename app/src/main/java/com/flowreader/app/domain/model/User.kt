package com.flowreader.app.domain.model

import java.util.Date

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Date = Date(),
    val lastSyncTime: Date? = null,
    val syncEnabled: Boolean = false
)

data class SyncProgress(
    val bookId: Long,
    val currentChapter: Int,
    val currentPosition: Int,
    val readingProgress: Float,
    val lastReadTime: Long
)

data class SyncBookmark(
    val bookId: Long,
    val chapterIndex: Int,
    val position: Int,
    val text: String,
    val createdTime: Long
)

data class SyncSettings(
    val fontSize: Int = 18,
    val lineSpacing: Float = 1.5f,
    val theme: String = "LIGHT",
    val pageMode: String = "SLIDE",
    val keepScreenOn: Boolean = true
)
