package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun getBookmarksByBookId(bookId: Long): Flow<List<Bookmark>>
    suspend fun getBookmarksListByBookId(bookId: Long): List<Bookmark>
    suspend fun getBookmarkById(id: Long): Bookmark?
    suspend fun insertBookmark(bookmark: Bookmark): Long
    suspend fun deleteBookmark(bookmark: Bookmark)
    suspend fun deleteBookmarkById(id: Long)
    suspend fun deleteBookmarksByBookId(bookId: Long)
}
