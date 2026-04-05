package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.entity.BookEntity
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao
) : BookRepository {

    override fun getAllBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities ->
            entities.map { it.toDomain() }
        }
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

    override fun searchBooks(query: String): Flow<List<Book>> {
        return bookDao.searchBooks(query).map { entities ->
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
    }

    override suspend fun deleteBookById(id: Long) {
        bookDao.deleteBookById(id)
    }

    override suspend fun updateReadingProgress(bookId: Long, chapter: Int, position: Int, progress: Float) {
        bookDao.updateReadingProgress(bookId, chapter, position, progress)
    }
}
