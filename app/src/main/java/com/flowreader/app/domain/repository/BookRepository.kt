package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.Annotation
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

interface ChapterRepository {
    fun getChaptersByBookId(bookId: Long): Flow<List<Chapter>>
    suspend fun getChaptersListByBookId(bookId: Long): List<Chapter>
    suspend fun getChapterMetadataList(bookId: Long): List<Chapter>
    suspend fun getChapter(bookId: Long, index: Int): Chapter?
    suspend fun getChapterContent(bookId: Long, index: Int): String?
    suspend fun getChapterById(id: Long): Chapter?
    suspend fun insertChapter(chapter: Chapter): Long
    suspend fun insertChapters(chapters: List<Chapter>)
    suspend fun updateChapter(chapter: Chapter)
    suspend fun deleteChaptersByBookId(bookId: Long)
    suspend fun getChapterCount(bookId: Long): Int
}

interface BookmarkRepository {
    fun getBookmarksByBookId(bookId: Long): Flow<List<Bookmark>>
    suspend fun getBookmarksListByBookId(bookId: Long): List<Bookmark>
    suspend fun getBookmarkById(id: Long): Bookmark?
    suspend fun insertBookmark(bookmark: Bookmark): Long
    suspend fun deleteBookmark(bookmark: Bookmark)
    suspend fun deleteBookmarkById(id: Long)
    suspend fun deleteBookmarksByBookId(bookId: Long)
}

interface AnnotationRepository {
    fun getAnnotationsByBookId(bookId: Long): Flow<List<Annotation>>
    suspend fun getAnnotationsListByBookId(bookId: Long): List<Annotation>
    fun getAnnotationsByChapter(bookId: Long, chapterIndex: Int): Flow<List<Annotation>>
    suspend fun getAnnotationsListByChapter(bookId: Long, chapterIndex: Int): List<Annotation>
    suspend fun getAnnotationById(id: Long): Annotation?
    suspend fun insertAnnotation(annotation: Annotation): Long
    suspend fun updateAnnotation(annotation: Annotation)
    suspend fun deleteAnnotation(annotation: Annotation)
    suspend fun deleteAnnotationById(id: Long)
    suspend fun deleteAnnotationsByBookId(bookId: Long)
    suspend fun searchAnnotations(bookId: Long, query: String): List<Annotation>
}

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun deleteCategoryById(id: Long)
}
