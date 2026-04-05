package com.flowreader.app.data.local

import android.content.Context
import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.dao.ChapterDao
import com.flowreader.app.data.local.entity.BookEntity
import com.flowreader.app.data.local.entity.ChapterEntity
import com.flowreader.app.util.BookParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val bookParser: BookParser
) {
    private val defaultBooks = listOf(
        "神秘复苏.txt" to "我会睡觉",
        "诡秘之主.txt" to "爱潜水的乌贼"
    )

    private var initialized = false

    suspend fun initializeDefaultBooks() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        try {
            val books = bookDao.getAllBooks().first()
            if (books.isEmpty()) {
                for ((fileName, author) in defaultBooks) {
                    initializeBook(fileName, author)
                }
            }
            initialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun initializeBook(fileName: String, author: String) {
        try {
            val booksDir = File(context.filesDir, "books")
            if (!booksDir.exists()) {
                booksDir.mkdirs()
            }

            val destFile = File(booksDir, fileName)
            
            if (!destFile.exists()) {
                context.assets.open("books/$fileName").use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val uri = android.net.Uri.fromFile(destFile)
            val parseResult = bookParser.parseBook(uri)
            
            parseResult.onSuccess { result ->
                val book = result.book.copy(
                    author = author,
                    filePath = destFile.absolutePath
                )
                
                val bookId = bookDao.insertBook(BookEntity.fromDomain(book))
                
                val chaptersWithBookId = result.chapters.map { chapter ->
                    ChapterEntity(
                        bookId = bookId,
                        index = chapter.index,
                        title = chapter.title,
                        content = chapter.content,
                        startPosition = chapter.startPosition,
                        endPosition = chapter.endPosition
                    )
                }
                chapterDao.insertChapters(chaptersWithBookId)
            }.onFailure { e ->
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}