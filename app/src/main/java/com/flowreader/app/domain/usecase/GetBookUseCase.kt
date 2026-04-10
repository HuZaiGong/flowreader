package com.flowreader.app.domain.usecase

import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.ChapterRepository
import javax.inject.Inject

class GetBookUseCase @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(bookId: Long): BookWithChapters? {
        val book = bookRepository.getBookById(bookId) ?: return null
        val chapterMeta = chapterRepository.getChapterMetadataList(bookId)
        return BookWithChapters(book, chapterMeta)
    }
}

data class BookWithChapters(
    val book: Book,
    val chapterMetadata: List<Chapter>
)