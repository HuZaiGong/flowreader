package com.flowreader.app.domain.model

import java.util.Date

data class Annotation(
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val startPosition: Int,
    val endPosition: Int,
    val selectedText: String,
    val note: String = "",
    val color: AnnotationColor = AnnotationColor.YELLOW,
    val type: AnnotationType = AnnotationType.HIGHLIGHT,
    val createdTime: Date = Date(),
    val modifiedTime: Date = Date()
)

enum class AnnotationType {
    HIGHLIGHT,
    NOTE,
    UNDERLINE
}

enum class AnnotationColor(val colorValue: Long) {
    YELLOW(0xFFFFFF00),
    GREEN(0xFF90EE90),
    BLUE(0xFFADD8E6),
    PINK(0xFFFFB6C1),
    ORANGE(0xFFFFA500)
}

data class SearchResult(
    val bookId: Long,
    val chapterIndex: Int,
    val chapterTitle: String,
    val matchedText: String,
    val position: Int,
    val contextBefore: String = "",
    val contextAfter: String = ""
)

data class SearchQuery(
    val bookId: Long,
    val query: String,
    val caseSensitive: Boolean = false
)
