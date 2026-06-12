package com.flowreader.app.domain.usecase

import android.util.Log
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.repository.AnnotationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 管理批注用例
 * 负责批注的增删改查操作
 */
class ManageAnnotationUseCase @Inject constructor(
    private val annotationRepository: AnnotationRepository
) {
    companion object {
        private const val TAG = "ManageAnnotationUseCase"
    }

    /**
     * 获取书籍的所有批注
     * @param bookId 书籍ID
     * @return Flow<List<Annotation>> 批注列表流
     */
    fun getAnnotationsByBookId(bookId: Long): Flow<List<Annotation>> {
        Log.d(TAG, "获取书籍批注: bookId=$bookId")
        return annotationRepository.getAnnotationsByBookId(bookId)
            .catch { e ->
                Log.e(TAG, "获取书籍批注失败: bookId=$bookId", e)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * 获取指定章节的批注
     * @param bookId 书籍ID
     * @param chapterIndex 章节索引
     * @return Flow<List<Annotation>> 批注列表流
     */
    fun getAnnotationsByChapter(bookId: Long, chapterIndex: Int): Flow<List<Annotation>> {
        Log.d(TAG, "获取章节批注: bookId=$bookId, chapterIndex=$chapterIndex")
        return annotationRepository.getAnnotationsByChapter(bookId, chapterIndex)
            .catch { e ->
                Log.e(TAG, "获取章节批注失败: bookId=$bookId, chapterIndex=$chapterIndex", e)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * 添加批注
     * @param annotation 批注对象
     * @return Result<Long> 成功返回批注ID，失败返回包含异常的 Result.failure
     */
    suspend fun addAnnotation(annotation: Annotation): Result<Long> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "添加批注: bookId=${annotation.bookId}, chapterIndex=${annotation.chapterIndex}")
            val id = annotationRepository.insertAnnotation(annotation)
            Log.d(TAG, "批注添加成功: annotationId=$id")
            Result.success(id)
        } catch (e: Exception) {
            Log.e(TAG, "添加批注失败", e)
            Result.failure(e)
        }
    }

    /**
     * 更新批注
     * @param annotation 要更新的批注对象
     * @return Result<Unit> 成功返回 Result.success，失败返回包含异常的 Result.failure
     */
    suspend fun updateAnnotation(annotation: Annotation): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "更新批注: annotationId=${annotation.id}")
            annotationRepository.updateAnnotation(annotation)
            Log.d(TAG, "批注更新成功: annotationId=${annotation.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "更新批注失败: annotationId=${annotation.id}", e)
            Result.failure(e)
        }
    }

    /**
     * 删除批注
     * @param annotationId 批注ID
     * @return Result<Unit> 成功返回 Result.success，失败返回包含异常的 Result.failure
     */
    suspend fun deleteAnnotation(annotationId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "删除批注: annotationId=$annotationId")
            annotationRepository.deleteAnnotationById(annotationId)
            Log.d(TAG, "批注删除成功: annotationId=$annotationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "删除批注失败: annotationId=$annotationId", e)
            Result.failure(e)
        }
    }

    /**
     * 删除书籍的所有批注
     * @param bookId 书籍ID
     * @return Result<Unit> 成功返回 Result.success，失败返回包含异常的 Result.failure
     */
    suspend fun deleteAllAnnotations(bookId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "删除书籍的所有批注: bookId=$bookId")
            annotationRepository.deleteAnnotationsByBookId(bookId)
            Log.d(TAG, "书籍批注删除成功: bookId=$bookId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "删除书籍批注失败: bookId=$bookId", e)
            Result.failure(e)
        }
    }

    /**
     * 导出批注为文本
     * @param bookId 书籍ID
     * @return Result<String> 成功返回批注文本，失败返回包含异常的 Result.failure
     */
    suspend fun exportAnnotations(bookId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "导出批注: bookId=$bookId")
            val annotations = annotationRepository.getAnnotationsListByBookId(bookId)
            
            if (annotations.isEmpty()) {
                return@withContext Result.success("暂无批注")
            }

            val text = buildString {
                annotations.groupBy { it.chapterIndex }.forEach { (chapterIndex, chapterAnnotations) ->
                    appendLine("=== 章节 ${chapterIndex + 1} ===\n")
                    chapterAnnotations.forEachIndexed { index, annotation ->
                        appendLine("【批注 ${index + 1}】")
                        appendLine("高亮文本: ${annotation.highlightText}")
                        if (annotation.note.isNotEmpty()) {
                            appendLine("笔记: ${annotation.note}")
                        }
                        appendLine("颜色: ${annotation.color}")
                        appendLine("时间: ${annotation.createdAt}")
                        appendLine()
                    }
                    appendLine()
                }
            }
            
            Log.d(TAG, "批注导出成功: bookId=$bookId, 共 ${annotations.size} 条")
            Result.success(text)
        } catch (e: Exception) {
            Log.e(TAG, "导出批注失败: bookId=$bookId", e)
            Result.failure(e)
        }
    }
}
