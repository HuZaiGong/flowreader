package com.flowreader.app.data.repository

import android.util.Log
import com.flowreader.app.core.util.SqlLike
import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.entity.BookEntity
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.util.FullTextSearch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val fullTextSearch: FullTextSearch
) : BookRepository {

    private companion object {
        const val TAG = "BookRepositoryImpl"
    }

    override fun getAllBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBooksPaged(offset: Int, limit: Int): List<Book> {
        return bookDao.getBooksPaged(offset, limit).map { it.toDomain() }
    }

    override suspend fun getBookCount(): Int {
        return bookDao.getBookCount()
    }

    override fun getBooksByCategory(categoryId: Long): Flow<List<Book>> {
        return bookDao.getBooksByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentlyReadBooks(limit: Int): Flow<List<Book>> {
        return bookDao.getRecentlyReadBooks(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Title/author search. The query is escaped, so `%` and `_` match themselves.
     *
     * They used to be wildcards in the bound argument: a lone `%` returned every book in the library
     * and `a_c` matched "abc" — surprising enough to read as search being broken.
     */
    override fun searchBooks(query: String): Flow<List<Book>> {
        return bookDao.searchBooks(SqlLike.escape(query)).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBooksByTag(tag: String): Flow<List<Book>> {
        return bookDao.getBooksByTag(SqlLike.escape(tag)).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBookById(id: Long): Book? {
        return bookDao.getBookById(id)?.toDomain()
    }

    override suspend fun getBookByPath(filePath: String): Book? {
        return bookDao.getBookByPath(filePath)?.toDomain()
    }

    override suspend fun insertBook(book: Book): Long {
        return bookDao.insertBook(BookEntity.fromDomain(book))
    }

    override suspend fun updateBook(book: Book) {
        bookDao.updateBook(BookEntity.fromDomain(book))
    }

    override suspend fun deleteBook(book: Book) {
        bookDao.deleteBook(BookEntity.fromDomain(book))
        dropFromSearchIndex(listOf(book.id))
    }

    override suspend fun deleteBookById(id: Long) {
        bookDao.deleteBookById(id)
        dropFromSearchIndex(listOf(id))
    }

    override suspend fun updateReadingProgress(bookId: Long, chapter: Int, position: Int, progress: Float) {
        bookDao.updateReadingProgress(bookId, chapter, position, progress)
    }

    override suspend fun deleteBooksByIds(ids: List<Long>) {
        val valid = ids.filter { it > 0L }.distinct()
        if (valid.isEmpty()) return
        bookDao.deleteBooksByIds(valid)
        dropFromSearchIndex(valid)
    }

    /**
     * Drops deleted books from the full-text index.
     *
     * Without this the index kept every deleted book's whole text — so global search returned hits
     * that opened a book that no longer existed, and the file grew monotonically no matter how much
     * the user deleted. Best-effort: index maintenance also sweeps ids the library no longer has, so a
     * failure here degrades to a stale entry until the next search rather than blocking the delete the
     * user asked for.
     */
    private suspend fun dropFromSearchIndex(ids: List<Long>) {
        try {
            fullTextSearch.deleteBooks(ids)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove deleted books from the search index", e)
        }
    }

    override suspend fun moveBooksToCategory(ids: List<Long>, categoryId: Long?) {
        val valid = ids.filter { it > 0L }.distinct()
        if (valid.isEmpty()) return
        bookDao.updateCategoryForBooks(valid, categoryId)
    }

    override suspend fun updateBooksMetadata(ids: List<Long>, author: String?, tags: List<String>?) {
        val valid = ids.filter { it > 0L }.distinct()
        if (valid.isEmpty()) return
        // A null field means "leave alone"; a blank author would otherwise silently erase every
        // author in the selection when the user only meant to retag.
        author?.takeIf { it.isNotBlank() }?.let { bookDao.updateAuthorForBooks(valid, it.trim()) }
        tags?.let { bookDao.updateTagsForBooks(valid, it.map { tag -> tag.trim() }.filter { tag -> tag.isNotEmpty() }.joinToString(",")) }
    }
}
