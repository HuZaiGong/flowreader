package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(): Flow<List<Book>>
    fun getBooksByCategory(categoryId: Long): Flow<List<Book>>
    fun getRecentlyReadBooks(limit: Int = 10): Flow<List<Book>>
    fun searchBooks(query: String): Flow<List<Book>>
    fun getBooksByTag(tag: String): Flow<List<Book>>
    suspend fun getBooksPaged(offset: Int, limit: Int): List<Book>
    suspend fun getBookCount(): Int
    suspend fun getBookById(id: Long): Book?
    suspend fun getBookByPath(filePath: String): Book?
    suspend fun insertBook(book: Book): Long
    suspend fun updateBook(book: Book)
    suspend fun deleteBook(book: Book)
    suspend fun deleteBookById(id: Long)
    suspend fun updateReadingProgress(bookId: Long, chapter: Int, position: Int, progress: Float)

    /**
     * Batch shelf edits (v53). These run as a single statement per call rather than a loop of
     * per-row updates so selecting 200 books and moving them does not emit 200 Room invalidations.
     */
    suspend fun deleteBooksByIds(ids: List<Long>)
    suspend fun moveBooksToCategory(ids: List<Long>, categoryId: Long?)

    /** Passing `null` leaves that field untouched, so "set author only" does not wipe tags. */
    suspend fun updateBooksMetadata(ids: List<Long>, author: String?, tags: List<String>?)
}
