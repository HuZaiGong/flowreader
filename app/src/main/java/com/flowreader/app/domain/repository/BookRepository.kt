package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.Bookmark
import com.flowreader.app.domain.model.Category
import com.flowreader.app.domain.model.Chapter
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(): Flow<List<Book>>
    fun getBooksByCategory(categoryId: Long): Flow<List<Book>>
    fun getRecentlyReadBooks(limit: Int = 10): Flow<List<Book>>
    fun searchBooks(query: String): Flow<List<Book>>
    suspend fun getBookById(id: Long): Book?
    suspend fun getBookByPath(filePath: String): Book?
    suspend fun insertBook(book: Book): Long
    suspend fun updateBook(book: Book)
    suspend fun deleteBook(book: Book)
    suspend fun deleteBookById(id: Long)
    suspend fun updateReadingProgress(bookId: Long, chapter: Int, position: Int, progress: Float)
}

interface ChapterRepository {
    fun getChaptersByBookId(bookId: Long): Flow<List<Chapter>>
    suspend fun getChaptersListByBookId(bookId: Long): List<Chapter>
    suspend fun getChapter(bookId: Long, index: Int): Chapter?
    suspend fun getChapterById(id: Long): Chapter?
    suspend fun insertChapter(chapter: Chapter): Long
    suspend fun insertChapters(chapters: List<Chapter>)
    suspend fun updateChapter(chapter: Chapter)
    suspend fun deleteChaptersByBookId(bookId: Long)
    suspend fun getChapterCount(bookId: Long): Int
}

interface BookmarkRepository {
    fun getAllBookmarks(): Flow<List<Bookmark>>
    fun getBookmarksByBookId(bookId: Long): Flow<List<Bookmark>>
    suspend fun getBookmarksListByBookId(bookId: Long): List<Bookmark>
    suspend fun getBookmarkById(id: Long): Bookmark?
    suspend fun insertBookmark(bookmark: Bookmark): Long
    suspend fun deleteBookmark(bookmark: Bookmark)
    suspend fun deleteBookmarkById(id: Long)
    suspend fun deleteBookmarksByBookId(bookId: Long)
    suspend fun clearAllBookmarks()
}

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun deleteCategoryById(id: Long)
}
