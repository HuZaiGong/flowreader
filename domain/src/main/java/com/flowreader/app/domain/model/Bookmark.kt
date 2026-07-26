package com.flowreader.app.domain.model

import java.util.Date

data class Bookmark(
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val position: Int,
    val text: String,
    val createdTime: Date = Date()
)
