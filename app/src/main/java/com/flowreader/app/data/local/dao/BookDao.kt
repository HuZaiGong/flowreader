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

    @Query("SELECT * FROM books WHERE status = :status ORDER BY lastReadTime DESC, addedTime DESC")
    fun getBooksByStatus(status: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY addedTime DESC")
    fun getFavoriteBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE status = 'READING' ORDER BY lastReadTime DESC LIMIT 5")
    fun getCurrentlyReading(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE filePath = :filePath")
    suspend fun getBookByPath(filePath: String): BookEntity?

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE tags LIKE '%' || :tag || '%'")
    fun getBooksByTag(tag: String): Flow<List<BookEntity>>

    @Query("SELECT COUNT(*) FROM books WHERE status = :status")
    suspend fun getBookCountByStatus(status: String): Int

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

    @Query("UPDATE books SET status = :status WHERE id = :bookId")
    suspend fun updateBookStatus(bookId: Long, status: String)

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun updateFavoriteStatus(bookId: Long, isFavorite: Boolean)

    @Query("UPDATE books SET tags = :tags WHERE id = :bookId")
    suspend fun updateTags(bookId: Long, tags: String)

    @Query("UPDATE books SET categoryId = :categoryId WHERE id = :bookId")
    suspend fun updateCategory(bookId: Long, categoryId: Long?)

    @Query("UPDATE books SET totalWordCount = :wordCount, estimatedReadTimeMinutes = :estimatedMinutes WHERE id = :bookId")
    suspend fun updateBookStats(bookId: Long, wordCount: Int, estimatedMinutes: Int)
}
