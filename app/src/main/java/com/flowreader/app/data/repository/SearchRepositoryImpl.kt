package com.flowreader.app.data.repository

import com.flowreader.app.domain.model.GlobalSearchResult
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.domain.repository.SearchRepository
import com.flowreader.app.util.FullTextSearch
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val fullTextSearch: FullTextSearch
) : SearchRepository {
    override suspend fun rebuildIndex() {
        fullTextSearch.initialize()
        bookRepository.getAllBooks().first().forEach { book ->
            val chapters = chapterRepository.getChapterMetadataList(book.id)
            fullTextSearch.deleteBookContent(book.id)
            chapters.forEach { chapter ->
                val content = chapterRepository.getChapterContent(book.id, chapter.index).orEmpty()
                if (content.isNotBlank()) {
                    fullTextSearch.indexChapter(book.id, chapter.index, chapter.title, content)
                }
            }
        }
    }

    override suspend fun searchAll(query: String): List<GlobalSearchResult> {
        rebuildIndex()
        val booksById = bookRepository.getAllBooks().first().associateBy { it.id }
        return fullTextSearch.searchAll(query).map { result ->
            GlobalSearchResult(
                bookId = result.bookId,
                bookTitle = booksById[result.bookId]?.title.orEmpty(),
                chapterIndex = result.chapterIndex,
                chapterTitle = result.chapterTitle,
                matchedText = result.matchedText
            )
        }
    }
}
