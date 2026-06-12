package com.flowreader.app.domain.usecase

import android.util.Log
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.repository.AnnotationRepository
import com.flowreader.app.domain.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 搜索书籍用例
 * 负责书籍和批注的搜索功能
 */
class SearchBooksUseCase @Inject constructor(
    private val bookRepository: BookRepository,
    private val annotationRepository: AnnotationRepository
) {
    companion object {
        private const val TAG = "SearchBooksUseCase"
    }

    /**
     * 搜索书籍
     * @param query 搜索关键词
     * @return Flow<List<Book>> 匹配的书籍列表流
     */
    operator fun invoke(query: String): Flow<List<Book>> {
        Log.d(TAG, "搜索书籍: query=$query")
        return bookRepository.searchBooks(query)
            .catch { e ->
                Log.e(TAG, "搜索书籍失败: query=$query", e)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * 在指定书籍中搜索批注
     * @param bookId 书籍ID
     * @param query 搜索关键词
     * @return Result<List<Annotation>> 成功返回匹配的批注列表，失败返回包含异常的 Result.failure
     */
    suspend fun searchAnnotations(bookId: Long, query: String): Result<List<Annotation>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "搜索批注: bookId=$bookId, query=$query")
            val annotations = annotationRepository.searchAnnotations(bookId, query)
            Log.d(TAG, "搜索到 ${annotations.size} 条批注")
            Result.success(annotations)
        } catch (e: Exception) {
            Log.e(TAG, "搜索批注失败: bookId=$bookId, query=$query", e)
            Result.failure(e)
        }
    }

    /**
     * 获取最近阅读的书籍
     * @param limit 返回数量限制
     * @return Flow<List<Book>> 最近阅读的书籍列表流
     */
    fun getRecentlyReadBooks(limit: Int = 10): Flow<List<Book>> {
        Log.d(TAG, "获取最近阅读书籍: limit=$limit")
        return bookRepository.getRecentlyReadBooks(limit)
            .catch { e ->
                Log.e(TAG, "获取最近阅读书籍失败", e)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * 按分类获取书籍
     * @param categoryId 分类ID
     * @return Flow<List<Book>> 该分类下的书籍列表流
     */
    fun getBooksByCategory(categoryId: Long): Flow<List<Book>> {
        Log.d(TAG, "获取分类书籍: categoryId=$categoryId")
        return bookRepository.getBooksByCategory(categoryId)
            .catch { e ->
                Log.e(TAG, "获取分类书籍失败: categoryId=$categoryId", e)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }
}
