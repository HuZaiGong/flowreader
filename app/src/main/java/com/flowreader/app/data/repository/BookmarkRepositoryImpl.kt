package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.BookmarkDao
import com.flowreader.app.data.local.entity.BookmarkEntity
import com.flowreader.app.domain.model.Bookmark
import com.flowreader.app.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {

    override fun getBookmarksByBookId(bookId: Long): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksByBookId(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBookmarksListByBookId(bookId: Long): List<Bookmark> {
        return bookmarkDao.getBookmarksListByBookId(bookId).map { it.toDomain() }
    }

    override suspend fun getBookmarkById(id: Long): Bookmark? {
        return bookmarkDao.getBookmarkById(id)?.toDomain()
    }

    override suspend fun insertBookmark(bookmark: Bookmark): Long {
        return bookmarkDao.insertBookmark(BookmarkEntity.fromDomain(bookmark))
    }

    override suspend fun deleteBookmark(bookmark: Bookmark) {
        bookmarkDao.deleteBookmark(BookmarkEntity.fromDomain(bookmark))
    }

    override suspend fun deleteBookmarkById(id: Long) {
        bookmarkDao.deleteBookmarkById(id)
    }

    override suspend fun deleteBookmarksByBookId(bookId: Long) {
        bookmarkDao.deleteBookmarksByBookId(bookId)
    }

    override fun getAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun clearAllBookmarks() {
        bookmarkDao.deleteAllBookmarks()
    }
}
