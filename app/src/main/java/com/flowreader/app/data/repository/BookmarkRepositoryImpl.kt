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

    private val maxBookmarkTextLength = 500

    override fun getBookmarksByBookId(bookId: Long): Flow<List<Bookmark>> {
        require(bookId > 0L) { "Book id must be positive" }
        return bookmarkDao.getBookmarksByBookId(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBookmarksListByBookId(bookId: Long): List<Bookmark> {
        require(bookId > 0L) { "Book id must be positive" }
        return bookmarkDao.getBookmarksListByBookId(bookId).map { it.toDomain() }
    }

    override suspend fun getBookmarkById(id: Long): Bookmark? {
        require(id > 0L) { "Bookmark id must be positive" }
        return bookmarkDao.getBookmarkById(id)?.toDomain()
    }

    override suspend fun addBookmark(bookmark: Bookmark): Bookmark {
        val normalized = bookmark.copy(
            id = 0L,
            bookId = requirePositive(bookmark.bookId, "Book id"),
            chapterIndex = bookmark.chapterIndex.coerceAtLeast(0),
            position = bookmark.position.coerceAtLeast(0),
            text = normalizeText(bookmark.text)
        )
        val id = bookmarkDao.insertBookmark(BookmarkEntity.fromDomain(normalized))
        return normalized.copy(id = id)
    }

    override suspend fun deleteBookmarkById(id: Long) {
        require(id > 0L) { "Bookmark id must be positive" }
        bookmarkDao.deleteBookmarkById(id)
    }

    override suspend fun deleteBookmarksByBookId(bookId: Long) {
        require(bookId > 0L) { "Book id must be positive" }
        bookmarkDao.deleteBookmarksByBookId(bookId)
    }

    private fun requirePositive(value: Long, name: String): Long {
        require(value > 0L) { "$name must be positive" }
        return value
    }

    private fun normalizeText(text: String): String {
        val normalized = text.trim().ifBlank { "书签" }
        return normalized.take(maxBookmarkTextLength)
    }
}
