package com.flowreader.app.domain.model

import java.util.Date

enum class BookFormat {
    EPUB,
    TXT,
    PDF,
    UNKNOWN
}

enum class BookStatus {
    READING,
    WANT_TO_READ,
    FINISHED,
    ABANDONED,
    COLLECTION
}

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String,
    val filePath: String,
    val coverPath: String? = null,
    val description: String = "",
    val fileSize: Long = 0,
    val format: BookFormat = BookFormat.EPUB,
    val totalChapters: Int = 0,
    val currentChapter: Int = 0,
    val currentPosition: Int = 0,
    val readingProgress: Float = 0f,
    val lastReadTime: Date? = null,
    val addedTime: Date = Date(),
    val categoryId: Long? = null,
    val status: BookStatus = BookStatus.READING,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val totalWordCount: Int = 0,
    val readWordCount: Int = 0,
    val estimatedReadTimeMinutes: Int = 0
)

data class Chapter(
    val id: Long = 0,
    val bookId: Long,
    val index: Int,
    val title: String,
    val content: String = "",
    val startPosition: Int = 0,
    val endPosition: Int = 0
)

data class Bookmark(
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val position: Int,
    val text: String,
    val note: String = "",
    val createdTime: Date = Date()
)

data class Category(
    val id: Long = 0,
    val name: String,
    val bookCount: Int = 0,
    val color: String = "#1976D2"
)
