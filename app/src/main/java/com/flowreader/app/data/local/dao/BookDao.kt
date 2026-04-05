package com.flowreader.app.data.local.dao

import androidx.room.*
import com.flowreader.app.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTime DESC, addedTime DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE categoryId = :categoryId ORDER BY addedTime DESC")
    fun getBooksByCategory(categoryId: Long): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE lastReadTime IS NOT NULL ORDER BY lastReadTime DESC LIMIT :limit")
    fun getRecentlyReadBooks(limit: Int = 10): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE filePath = :filePath")
    suspend fun getBookByPath(filePath: String): BookEntity?

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("UPDATE books SET currentChapter = :chapter, currentPosition = :position, readingProgress = :progress, lastReadTime = :time WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: Long, chapter: Int, position: Int, progress: Float, time: Long = System.currentTimeMillis())
}
