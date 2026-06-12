package com.flowreader.app.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.util.SafeFileNames
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * 更新书籍用例
 * 负责更新书籍元数据和封面
 */
class UpdateBookUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository
) {
    companion object {
        private const val TAG = "UpdateBookUseCase"
        private const val COVER_QUALITY = 85
    }

    /**
     * 更新书籍元数据
     * @param book 要更新的书籍对象
     * @return Result<Unit> 成功返回 Result.success，失败返回包含异常的 Result.failure
     */
    suspend operator fun invoke(book: Book): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "更新书籍元数据: bookId=${book.id}, title=${book.title}")
            bookRepository.updateBook(book)
            Log.d(TAG, "书籍元数据更新成功: bookId=${book.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "更新书籍元数据失败: bookId=${book.id}", e)
            Result.failure(e)
        }
    }

    /**
     * 更新书籍封面
     * @param bookId 书籍ID
     * @param coverUri 新封面的URI
     * @return Result<String> 成功返回新封面的内部路径，失败返回包含异常的 Result.failure
     */
    suspend fun updateCover(bookId: Long, coverUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "更新书籍封面: bookId=$bookId")
            
            val book = bookRepository.getBookById(bookId)
                ?: return@withContext Result.failure(IllegalArgumentException("书籍不存在"))

            // 删除旧封面
            book.coverPath?.let { oldCoverPath ->
                val oldCoverFile = File(context.filesDir, oldCoverPath)
                if (oldCoverFile.exists()) {
                    oldCoverFile.delete()
                    Log.d(TAG, "已删除旧封面: $oldCoverPath")
                }
            }

            // 保存新封面
            val newCoverPath = saveCoverToInternal(coverUri, book.title)
            
            // 更新数据库
            bookRepository.updateBook(book.copy(coverPath = newCoverPath))
            
            Log.d(TAG, "封面更新成功: bookId=$bookId, newPath=$newCoverPath")
            Result.success(newCoverPath)
        } catch (e: Exception) {
            Log.e(TAG, "更新封面失败: bookId=$bookId", e)
            Result.failure(e)
        }
    }

    /**
     * 更新书籍封面（使用 Bitmap）
     * @param bookId 书籍ID
     * @param coverBitmap 新封面的 Bitmap
     * @return Result<String> 成功返回新封面的内部路径，失败返回包含异常的 Result.failure
     */
    suspend fun updateCover(bookId: Long, coverBitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "更新书籍封面(Bitmap): bookId=$bookId")
            
            val book = bookRepository.getBookById(bookId)
                ?: return@withContext Result.failure(IllegalArgumentException("书籍不存在"))

            // 删除旧封面
            book.coverPath?.let { oldCoverPath ->
                val oldCoverFile = File(context.filesDir, oldCoverPath)
                if (oldCoverFile.exists()) {
                    oldCoverFile.delete()
                    Log.d(TAG, "已删除旧封面: $oldCoverPath")
                }
            }

            // 保存新封面
            val newCoverPath = saveCoverToInternal(coverBitmap, book.title)
            
            // 更新数据库
            bookRepository.updateBook(book.copy(coverPath = newCoverPath))
            
            Log.d(TAG, "封面更新成功: bookId=$bookId, newPath=$newCoverPath")
            Result.success(newCoverPath)
        } catch (e: Exception) {
            Log.e(TAG, "更新封面失败: bookId=$bookId", e)
            Result.failure(e)
        }
    }

    private fun saveCoverToInternal(coverUri: Uri, bookTitle: String): String {
        val fileName = SafeFileNames.forCover(bookTitle)
        val outputFile = File(context.filesDir, fileName)
        
        context.contentResolver.openInputStream(coverUri)?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("无法读取封面文件")
        
        return fileName
    }

    private fun saveCoverToInternal(bitmap: Bitmap, bookTitle: String): String {
        val fileName = SafeFileNames.forCover(bookTitle)
        val outputFile = File(context.filesDir, fileName)
        
        FileOutputStream(outputFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, COVER_QUALITY, output)
        }
        
        return fileName
    }
}
