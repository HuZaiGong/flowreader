package com.flowreader.app.data.repository

import android.content.Context
import android.net.Uri
import com.flowreader.app.data.local.AppDatabase
import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.dao.BookmarkDao
import com.flowreader.app.data.local.dao.CategoryDao
import com.flowreader.app.data.local.dao.ChapterDao
import com.flowreader.app.data.local.dao.ReadingStatsDao
import com.flowreader.app.data.local.entity.BookEntity
import com.flowreader.app.data.local.entity.BookmarkEntity
import com.flowreader.app.data.local.entity.CategoryEntity
import com.flowreader.app.data.local.entity.ChapterEntity
import com.flowreader.app.data.local.entity.ReadingStatsEntity
import com.flowreader.app.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val bookmarkDao: BookmarkDao,
    private val categoryDao: CategoryDao,
    private val readingStatsDao: ReadingStatsDao
) : BackupRepository {
    override suspend fun exportData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()

            val books = bookDao.getAllBooks().first()
            val booksArray = JSONArray()
            books.forEach { book ->
                booksArray.put(book.toJson())
            }
            json.put("books", booksArray)

            val categories = categoryDao.getAllCategories().first()
            val categoriesArray = JSONArray()
            categories.forEach { category ->
                categoriesArray.put(category.toJson())
            }
            json.put("categories", categoriesArray)

            val allBookmarks = mutableListOf<BookmarkEntity>()
            books.forEach { book ->
                val bookmarks = bookmarkDao.getBookmarksByBookId(book.id).first()
                allBookmarks.addAll(bookmarks)
            }
            val bookmarksArray = JSONArray()
            allBookmarks.forEach { bookmark ->
                bookmarksArray.put(bookmark.toJson())
            }
            json.put("bookmarks", bookmarksArray)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json.toString(2))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importData(uri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val json = JSONObject(reader.readText())

                    var booksImported = 0
                    var bookmarksImported = 0

                    val booksArray = json.getJSONArray("books")
                    for (i in 0 until booksArray.length()) {
                        val bookJson = booksArray.getJSONObject(i)
                        val book = BookEntity.fromJson(bookJson)
                        bookDao.insertBook(book)
                        booksImported++

                        val chaptersArray = bookJson.getJSONArray("chapters")
                        for (j in 0 until chaptersArray.length()) {
                            val chapterJson = chaptersArray.getJSONObject(j)
                            val chapter = ChapterEntity.fromJson(chapterJson)
                            chapterDao.insertChapter(chapter)
                        }
                    }

                    val bookmarksArray = json.getJSONArray("bookmarks")
                    for (i in 0 until bookmarksArray.length()) {
                        val bookmarkJson = bookmarksArray.getJSONObject(i)
                        val bookmark = BookmarkEntity.fromJson(bookmarkJson)
                        bookmarkDao.insertBookmark(bookmark)
                        bookmarksImported++
                    }

                    Result.success(ImportResult(booksImported, bookmarksImported))
                }
            } ?: Result.failure(Exception("无法打开文件"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportReadingProgress(bookId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            val book = bookDao.getBookById(bookId)
                ?: return@withContext Result.failure(Exception("书籍不存在"))

            val json = JSONObject().apply {
                put("title", book.title)
                put("author", book.author)
                put("currentChapter", book.currentChapter)
                put("currentPosition", book.currentPosition)
                put("readingProgress", book.readingProgress)
                put("lastReadTime", book.lastReadTime)
            }

            Result.success(json.toString(2))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ImportResult(
    val booksImported: Int,
    val bookmarksImported: Int
)

private fun BookEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("author", author)
    put("filePath", filePath)
    put("coverPath", coverPath)
    put("description", description)
    put("fileSize", fileSize)
    put("format", format)
    put("totalChapters", totalChapters)
    put("currentChapter", currentChapter)
    put("currentPosition", currentPosition)
    put("readingProgress", readingProgress)
    put("lastReadTime", lastReadTime)
    put("addedTime", addedTime)
    put("categoryId", categoryId)
}

private fun BookEntity.Companion.fromJson(json: JSONObject): BookEntity = BookEntity(
    id = json.getLong("id"),
    title = json.getString("title"),
    author = json.getString("author"),
    filePath = json.optString("filePath", ""),
    coverPath = json.optString("coverPath"),
    description = json.optString("description", ""),
    fileSize = json.optLong("fileSize", 0),
    format = json.optString("format", "TXT"),
    totalChapters = json.optInt("totalChapters", 0),
    currentChapter = json.optInt("currentChapter", 0),
    currentPosition = json.optInt("currentPosition", 0),
    readingProgress = json.optDouble("readingProgress", 0.0).toFloat(),
    lastReadTime = json.optLong("lastReadTime"),
    addedTime = json.optLong("addedTime", System.currentTimeMillis()),
    categoryId = if (json.has("categoryId") && !json.isNull("categoryId")) json.getLong("categoryId") else null
)

private fun CategoryEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
}

private fun BookmarkEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("bookId", bookId)
    put("chapterIndex", chapterIndex)
    put("position", position)
    put("text", text)
    put("createdTime", createdTime)
}

private fun BookmarkEntity.Companion.fromJson(json: JSONObject): BookmarkEntity = BookmarkEntity(
    id = json.getLong("id"),
    bookId = json.getLong("bookId"),
    chapterIndex = json.getInt("chapterIndex"),
    position = json.getInt("position"),
    text = json.getString("text"),
    createdTime = json.optLong("createdTime", System.currentTimeMillis())
)

private fun ChapterEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("bookId", bookId)
    put("index", index)
    put("title", title)
    put("content", content)
    put("startPosition", startPosition)
    put("endPosition", endPosition)
}

private fun ChapterEntity.Companion.fromJson(json: JSONObject): ChapterEntity = ChapterEntity(
    id = json.getLong("id"),
    bookId = json.getLong("bookId"),
    index = json.getInt("index"),
    title = json.getString("title"),
    content = json.optString("content", ""),
    startPosition = json.optInt("startPosition", 0),
    endPosition = json.optInt("endPosition", 0)
)
