package com.flowreader.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.dao.BookmarkDao
import com.flowreader.app.data.local.dao.CategoryDao
import com.flowreader.app.data.local.dao.ChapterDao
import com.flowreader.app.data.local.entity.BookEntity
import com.flowreader.app.data.local.entity.BookmarkEntity
import com.flowreader.app.data.local.entity.CategoryEntity
import com.flowreader.app.data.local.entity.ChapterEntity
import com.flowreader.app.domain.model.ImportResult
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
    private val categoryDao: CategoryDao
) : BackupRepository {

    companion object {
        private const val TAG = "BackupRepository"
    }
    override suspend fun exportData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始导出备份数据")
            val json = JSONObject()

            val books = bookDao.getAllBooks().first()
            Log.d(TAG, "导出书籍数量: ${books.size}")
            val booksArray = JSONArray()
            books.forEach { book ->
                val chapters = chapterDao.getChaptersByBookId(book.id).first()
                booksArray.put(book.toJson(chapters))
            }
            json.put("books", booksArray)

            val categories = categoryDao.getAllCategories().first()
            Log.d(TAG, "导出分类数量: ${categories.size}")
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
            Log.d(TAG, "导出书签数量: ${allBookmarks.size}")
            val bookmarksArray = JSONArray()
            allBookmarks.forEach { bookmark ->
                bookmarksArray.put(bookmark.toJson())
            }
            json.put("bookmarks", bookmarksArray)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json.toString(2))
                }
            } ?: throw IllegalStateException("无法打开输出流")

            Log.d(TAG, "备份数据导出成功")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "导出备份数据失败", e)
            Result.failure(e)
        }
    }

    override suspend fun importData(uri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始导入备份数据")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val json = JSONObject(reader.readText())

                    // 备份文件来自外部存储，恢复时只信任内容字段，不复用主键或内部文件路径。
                    val bookIdMap = mutableMapOf<Long, Long>()
                    val categoryIdMap = importCategories(json.optJSONArray("categories"))
                    Log.d(TAG, "导入分类映射: ${categoryIdMap.size} 个")

                    var booksImported = 0
                    var bookmarksImported = 0

                    val booksArray = json.getJSONArray("books")
                    Log.d(TAG, "准备导入 ${booksArray.length()} 本书籍")
                    for (i in 0 until booksArray.length()) {
                        val bookJson = booksArray.getJSONObject(i)
                        val originalBookId = bookJson.getLong("id")
                        val book = BookEntity.fromJson(bookJson).copy(
                            id = 0,
                            filePath = "",
                            coverPath = null,
                            categoryId = BookEntity.categoryIdFromJson(bookJson)?.let { categoryIdMap[it] }
                        )
                        val newBookId = bookDao.insertBook(book)
                        bookIdMap[originalBookId] = newBookId
                        booksImported++

                        val chaptersArray = bookJson.optJSONArray("chapters") ?: JSONArray()
                        Log.d(TAG, "导入书籍 '${book.title}' 的 ${chaptersArray.length()} 个章节")
                        for (j in 0 until chaptersArray.length()) {
                            val chapterJson = chaptersArray.getJSONObject(j)
                            val chapter = ChapterEntity.fromJson(chapterJson).copy(id = 0, bookId = newBookId)
                            chapterDao.insertChapter(chapter)
                        }
                    }

                    val bookmarksArray = json.getJSONArray("bookmarks")
                    Log.d(TAG, "准备导入 ${bookmarksArray.length()} 个书签")
                    for (i in 0 until bookmarksArray.length()) {
                        val bookmarkJson = bookmarksArray.getJSONObject(i)
                        val originalBookId = bookmarkJson.getLong("bookId")
                        val newBookId = bookIdMap[originalBookId]
                        if (newBookId == null) {
                            Log.w(TAG, "书签关联的书籍不存在，跳过: originalBookId=$originalBookId")
                            continue
                        }
                        val bookmark = BookmarkEntity.fromJson(bookmarkJson).copy(id = 0, bookId = newBookId)
                        bookmarkDao.insertBookmark(bookmark)
                        bookmarksImported++
                    }

                    Log.d(TAG, "备份数据导入成功: 书籍=$booksImported, 书签=$bookmarksImported")
                    Result.success(ImportResult(booksImported, bookmarksImported))
                }
            } ?: Result.failure(Exception("无法打开文件"))
        } catch (e: Exception) {
            Log.e(TAG, "导入备份数据失败", e)
            Result.failure(e)
        }
    }

    private suspend fun importCategories(categoriesArray: JSONArray?): Map<Long, Long> {
        if (categoriesArray == null) return emptyMap()

        val categoryIdMap = mutableMapOf<Long, Long>()
        for (i in 0 until categoriesArray.length()) {
            val categoryJson = categoriesArray.getJSONObject(i)
            val originalId = categoryJson.getLong("id")
            val category = CategoryEntity.fromJson(categoryJson).copy(id = 0)
            categoryIdMap[originalId] = categoryDao.insertCategory(category)
        }
        return categoryIdMap
    }

    override suspend fun exportReadingProgress(bookId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "导出阅读进度: bookId=$bookId")
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

            Log.d(TAG, "阅读进度导出成功: bookId=$bookId")
            Result.success(json.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "导出阅读进度失败: bookId=$bookId", e)
            Result.failure(e)
        }
    }
}


private fun BookEntity.toJson(chapters: List<ChapterEntity>): JSONObject = JSONObject().apply {
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
    put("chapters", JSONArray().apply {
        chapters.forEach { chapter -> put(chapter.toJson()) }
    })
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
    categoryId = BookEntity.categoryIdFromJson(json)
)

private fun BookEntity.Companion.categoryIdFromJson(json: JSONObject): Long? =
    if (json.has("categoryId") && !json.isNull("categoryId")) json.getLong("categoryId") else null

private fun CategoryEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
}

private fun CategoryEntity.Companion.fromJson(json: JSONObject): CategoryEntity = CategoryEntity(
    id = json.getLong("id"),
    name = json.getString("name")
)

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
