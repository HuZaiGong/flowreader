package com.flowreader.app.domain.model

import java.util.Date

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
    val tags: List<String> = emptyList()
)

/**
 * [FB2] and [MOBI] are read-only imports added in v53: both are converted to chapters at import
 * time like EPUB, and neither involves DRM — an encrypted MOBI is rejected, not decrypted.
 * [COMIC] covers single image files and image-only ZIP/CBZ packs, added in v54.
 */
enum class BookFormat {
    EPUB,
    TXT,
    PDF,
    MARKDOWN,
    FB2,
    MOBI,
    COMIC,
    UNKNOWN
}

data class Chapter(
    val id: Long = 0,
    val bookId: Long,
    val index: Int,
    val title: String,
    val content: String = "",
    val startPosition: Int = 0,
    val endPosition: Int = 0
)

data class Category(
    val id: Long = 0,
    val name: String,
    val bookCount: Int = 0
)
