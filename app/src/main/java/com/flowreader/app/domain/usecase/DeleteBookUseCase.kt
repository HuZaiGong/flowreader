package com.flowreader.app.domain.usecase

import android.content.Context
import android.util.Log
import com.flowreader.app.domain.repository.AnnotationRepository
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.BookmarkRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.domain.repository.ReadingStatsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 删除书籍用例
 * 负责删除书籍及其关联的所有数据（章节、书签、批注、阅读统计）和本地文件
 */
class DeleteBookUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val annotationRepository: AnnotationRepository,
    private val readingStatsRepository: ReadingStatsRepository
) {
    companion object {
        private const val TAG = "DeleteBookUseCase"
    }

    /**
     * 删除书籍及其关联数据
     * @param bookId 书籍ID
     * @return Result<Unit> 成功返回 Result.success，失败返回包含异常的 Result.failure
     */
    suspend operator fun invoke(bookId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始删除书籍: bookId=$bookId")
            
            // 获取书籍信息以删除本地文件
            val book = bookRepository.getBookById(bookId)
            if (book == null) {
                Log.w(TAG, "书籍不存在: bookId=$bookId")
                return@withContext Result.failure(IllegalArgumentException("书籍不存在"))
            }

            // 删除关联数据
            chapterRepository.deleteChaptersByBookId(bookId)
            bookmarkRepository.deleteBookmarksByBookId(bookId)
            annotationRepository.deleteAnnotationsByBookId(bookId)
            readingStatsRepository.deleteStatsByBookId(bookId)
            
            // 删除书籍记录
            bookRepository.deleteBookById(bookId)
            
            // 删除本地文件
            deleteBookFiles(book.filePath, book.coverPath)
            
            Log.d(TAG, "书籍删除成功: bookId=$bookId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "删除书籍失败: bookId=$bookId", e)
            Result.failure(e)
        }
    }

    /**
     * 删除书籍的本地文件
     */
    private fun deleteBookFiles(filePath: String?, coverPath: String?) {
        try {
            filePath?.let {
                val bookFile = File(context.filesDir, it)
                if (bookFile.exists()) {
                    val deleted = bookFile.delete()
                    Log.d(TAG, "删除书籍文件: path=$it, success=$deleted")
                }
            }
            
            coverPath?.let {
                val coverFile = File(context.filesDir, it)
                if (coverFile.exists()) {
                    val deleted = coverFile.delete()
                    Log.d(TAG, "删除封面文件: path=$it, success=$deleted")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "删除本地文件时发生错误", e)
            // 文件删除失败不影响数据库记录的删除
        }
    }
}
