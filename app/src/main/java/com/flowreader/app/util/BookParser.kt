package com.flowreader.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.model.Chapter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.*
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class ParseProgress {
    object Starting : ParseProgress()
    data class Reading(val bytesRead: Long, val totalBytes: Long) : ParseProgress()
    object Parsing : ParseProgress()
    object Saving : ParseProgress()
    object Complete : ParseProgress()
    data class Error(val message: String) : ParseProgress()
}

@Singleton
class BookParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bufferSize = 8192

    suspend fun parseBook(uri: Uri): Result<BookParseResult> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("无法打开文件"))

            val fileName = getFileName(uri)
            val format = detectFormat(fileName)
            val fileSize = getFileSize(uri)

            when (format) {
                BookFormat.EPUB -> parseEpubStream(inputStream, fileName, fileSize)
                BookFormat.TXT -> parseTxtStream(inputStream, fileName, fileSize)
                else -> Result.failure(Exception("不支持的格式: $format"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseBookWithProgress(uri: Uri) = flow {
        emit(ParseProgress.Starting)
        
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("无法打开文件")

            val fileName = getFileName(uri)
            val format = detectFormat(fileName)
            val fileSize = getFileSize(uri)

            when (format) {
                BookFormat.EPUB -> {
                    emit(ParseProgress.Reading(0, fileSize))
                    val result = parseEpubStream(inputStream, fileName, fileSize)
                    emit(ParseProgress.Parsing)
                    result
                }
                BookFormat.TXT -> {
                    emit(ParseProgress.Reading(0, fileSize))
                    val result = parseTxtStream(inputStream, fileName, fileSize)
                    emit(ParseProgress.Parsing)
                    result
                }
                else -> Result.failure(Exception("不支持的格式: $format"))
            }.onSuccess {
                emit(ParseProgress.Complete)
            }.onFailure {
                emit(ParseProgress.Error(it.message ?: "解析失败"))
            }
        } catch (e: Exception) {
            emit(ParseProgress.Error(e.message ?: "解析失败"))
        }
    }.flowOn(Dispatchers.IO)

    private fun detectFormat(fileName: String): BookFormat {
        return when {
            fileName.endsWith(".epub", ignoreCase = true) -> BookFormat.EPUB
            fileName.endsWith(".txt", ignoreCase = true) -> BookFormat.TXT
            fileName.endsWith(".pdf", ignoreCase = true) -> BookFormat.PDF
            else -> BookFormat.UNKNOWN
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "未知书籍"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex >= 0) {
                    name = cursor.getString(displayNameIndex) ?: name
                }
            }
        }
        return name
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        return size
    }

    private fun parseEpubStream(inputStream: InputStream, fileName: String, fileSize: Long): Result<BookParseResult> {
        // 暂时使用简化版的EPUB解析
        // 完整的EPUB解析需要添加epublib依赖
        return try {
            // 简化处理：尝试用JSoup直接解析
            val text = inputStream.bufferedReader(Charsets.UTF_8).readText()
            val doc = Jsoup.parse(text)
            val bodyText = doc.body().text()

            val title = fileName.removeSuffix(".epub")

            val chapters = mutableListOf<Chapter>()
            if (bodyText.isNotBlank()) {
                // 简单的章节分割
                val contentLength = bodyText.length
                val chapterCount = maxOf(1, contentLength / 5000)
                val chunkSize = contentLength / chapterCount

                for (i in 0 until chapterCount) {
                    val start = i * chunkSize
                    val end = minOf(start + chunkSize, contentLength)
                    val chapterContent = bodyText.substring(start, end)

                    chapters.add(
                        Chapter(
                            bookId = 0,
                            index = i,
                            title = "第 ${i + 1} 章",
                            content = chapterContent,
                            startPosition = start,
                            endPosition = end
                        )
                    )
                }
            }

            Result.success(
                BookParseResult(
                    book = Book(
                        title = title,
                        author = "未知作者",
                        filePath = "",
                        coverPath = null,
                        description = "",
                        fileSize = fileSize,
                        format = BookFormat.EPUB,
                        totalChapters = chapters.size
                    ),
                    chapters = chapters
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTxtStream(inputStream: InputStream, fileName: String, fileSize: Long): Result<BookParseResult> {
        return try {
            val text = inputStream.bufferedReader(Charsets.UTF_8).readText()

            val title = fileName.removeSuffix(".txt")
            val chapters = mutableListOf<Chapter>()

            val lines = text.lines()
            val chapterPattern = Regex("^(第[一二三四五六七八九十\\d]+[章节卷部分篇]|#|Chapter|CHAPTER)\\s*.*")

            var currentChapterTitle = "前言"
            var currentContent = StringBuilder()
            var chapterIndex = 0

            for (line in lines) {
                val trimmedLine = line.trim()
                if (chapterPattern.containsMatchIn(trimmedLine) && trimmedLine.length < 100) {
                    if (currentContent.isNotBlank()) {
                        chapters.add(
                            Chapter(
                                bookId = 0,
                                index = chapterIndex,
                                title = currentChapterTitle,
                                content = currentContent.toString().trim(),
                                startPosition = 0,
                                endPosition = currentContent.length
                            )
                        )
                        chapterIndex++
                    }
                    currentChapterTitle = trimmedLine
                    currentContent = StringBuilder()
                } else {
                    currentContent.appendLine(line)
                }
            }

            if (currentContent.isNotBlank()) {
                chapters.add(
                    Chapter(
                        bookId = 0,
                        index = chapterIndex,
                        title = currentChapterTitle,
                        content = currentContent.toString().trim(),
                        startPosition = 0,
                        endPosition = currentContent.length
                    )
                )
            }

            if (chapters.isEmpty()) {
                chapters.add(
                    Chapter(
                        bookId = 0,
                        index = 0,
                        title = "全部内容",
                        content = text,
                        startPosition = 0,
                        endPosition = text.length
                    )
                )
            }

            Result.success(
                BookParseResult(
                    book = Book(
                        title = title,
                        author = "未知作者",
                        filePath = "",
                        fileSize = fileSize,
                        format = BookFormat.TXT,
                        totalChapters = chapters.size
                    ),
                    chapters = chapters
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveCoverImage(imageData: ByteArray, bookTitle: String): String? {
        return try {
            val coversDir = File(context.filesDir, "covers")
            if (!coversDir.exists()) {
                coversDir.mkdirs()
            }

            val fileName = bookTitle.replace(Regex("[^a-zA-Z0-9]"), "_") + ".jpg"
            val file = File(coversDir, fileName)

            FileOutputStream(file).use { fos ->
                fos.write(imageData)
            }

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun copyFileToInternal(uri: Uri): String? {
        return try {
            val booksDir = File(context.filesDir, "books")
            if (!booksDir.exists()) {
                booksDir.mkdirs()
            }

            val fileName = getFileName(uri)
            val file = File(booksDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}

data class BookParseResult(
    val book: Book,
    val chapters: List<Chapter>
)
