package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.Chapter
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {
    fun getChaptersByBookId(bookId: Long): Flow<List<Chapter>>
    suspend fun getChaptersListByBookId(bookId: Long): List<Chapter>
    suspend fun getChapterMetadataList(bookId: Long): List<Chapter>
    suspend fun getChapter(bookId: Long, index: Int): Chapter?
    suspend fun getChapterContent(bookId: Long, index: Int): String?
    suspend fun getChapterById(id: Long): Chapter?
    suspend fun insertChapter(chapter: Chapter): Long
    suspend fun insertChapters(chapters: List<Chapter>)
    suspend fun updateChapter(chapter: Chapter)
    suspend fun deleteChaptersByBookId(bookId: Long)
    suspend fun getChapterCount(bookId: Long): Int
}
