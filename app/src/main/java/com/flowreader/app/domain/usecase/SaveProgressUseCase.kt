package com.flowreader.app.domain.usecase

import com.flowreader.app.domain.repository.BookRepository
import javax.inject.Inject

class SaveProgressUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    companion object {
        const val DEBOUNCE_MS = 3000L
    }

    /**
     * 保存阅读进度
     * @param bookId 书籍ID
     * @param chapterIndex 当前章节索引
     * @param position 当前阅读位置
     * @param progress 阅读进度百分比
     */
    suspend operator fun invoke(
        bookId: Long,
        chapterIndex: Int,
        position: Int,
        progress: Float
    ) {
        bookRepository.updateReadingProgress(bookId, chapterIndex, position, progress)
    }
}