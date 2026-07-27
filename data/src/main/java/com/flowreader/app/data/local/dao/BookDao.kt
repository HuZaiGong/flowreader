package com.flowreader.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flowreader.app.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTime DESC, addedTime DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY lastReadTime DESC, addedTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getBooksPaged(offset: Int, limit: Int): List<BookEntity>

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

    @Query("SELECT * FROM books WHERE tags LIKE '%' || :tag || '%' ORDER BY addedTime DESC")
    fun getBooksByTag(tag: String): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query(
        "UPDATE books SET currentChapter = :chapter, currentPosition = :position, " +
            "readingProgress = :progress, lastReadTime = :time WHERE id = :bookId"
    )
    suspend fun updateReadingProgress(bookId: Long, chapter: Int, position: Int, progress: Float, time: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    // v53 batch shelf edits. One statement per action instead of a per-row loop, so a 200-book
    // selection emits a single Room invalidation rather than 200.

    @Query("DELETE FROM books WHERE id IN (:ids)")
    suspend fun deleteBooksByIds(ids: List<Long>)

    @Query("UPDATE books SET categoryId = :categoryId WHERE id IN (:ids)")
    suspend fun updateCategoryForBooks(ids: List<Long>, categoryId: Long?)

    @Query("UPDATE books SET author = :author WHERE id IN (:ids)")
    suspend fun updateAuthorForBooks(ids: List<Long>, author: String)

    @Query("UPDATE books SET tags = :tags WHERE id IN (:ids)")
    suspend fun updateTagsForBooks(ids: List<Long>, tags: String)
}
