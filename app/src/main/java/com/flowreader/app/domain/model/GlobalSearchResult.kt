package com.flowreader.app.domain.model

data class GlobalSearchResult(
    val bookId: Long,
    val bookTitle: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val matchedText: String
)
