package com.flowreader.app.domain.usecase

import android.net.Uri
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.util.BookParser
import javax.inject.Inject

class ImportBookUseCase @Inject constructor(
    private val bookParser: BookParser,
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(uri: Uri): Result<Long> {
        return bookParser.parseBook(uri).mapCatching { result ->
            val internalPath = result.pdfFilePath ?: bookParser.copyFileToInternal(uri)
            val book = result.book.copy(filePath = internalPath.orEmpty())
            val bookId = bookRepository.insertBook(book)
            val chapters = result.chapters.map { it.copy(bookId = bookId) }
            chapterRepository.insertChapters(chapters)
            bookId
        }
    }
}
