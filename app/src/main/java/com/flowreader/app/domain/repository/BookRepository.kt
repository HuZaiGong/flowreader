package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(): Flow<List<Book>>
    fun getBooksByCategory(categoryId: Long): Flow<List<Book>>
    fun getRecentlyReadBooks(limit: Int = 10): Flow<List<Book>>
    fun searchBooks(query: String): Flow<List<Book>>
    suspend fun getBooksPaged(offset: Int, limit: Int): List<Book>
    suspend fun getBookCount(): Int
    suspend fun getBookById(id: Long): Book?
    suspend fun getBookByPath(filePath: String): Book?
    suspend fun insertBook(book: Book): Long
    suspend fun updateBook(book: Book)
    suspend fun deleteBook(book: Book)
    suspend fun deleteBookById(id: Long)
    suspend fun updateReadingProgress(bookId: Long, chapter: Int, position: Int, progress: Float)
}
