package com.flowreader.app.data.repository

import com.flowreader.app.domain.model.Book
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
    private var indexedBookIds: Set<Long> = emptySet()
    private var hasBuiltIndex = false

    override suspend fun rebuildIndex() {
        fullTextSearch.initialize()
        val books = bookRepository.getAllBooks().first()
        fullTextSearch.deleteAllContent()
        books.forEach { book ->
            val chapters = chapterRepository.getChapterMetadataList(book.id)
            chapters.forEach { chapter ->
                val content = chapterRepository.getChapterContent(book.id, chapter.index).orEmpty()
                if (content.isNotBlank()) {
                    fullTextSearch.indexChapter(book.id, chapter.index, chapter.title, content)
                }
            }
        }
        indexedBookIds = books.map { it.id }.toSet()
        hasBuiltIndex = true
    }

    override suspend fun searchAll(query: String): List<GlobalSearchResult> =
        searchChapters(query, limit = 100, offset = 0)

    override suspend fun searchChapters(query: String, limit: Int, offset: Int): List<GlobalSearchResult> {
        val books = bookRepository.getAllBooks().first()
        val bookIds = books.map { it.id }.toSet()
        fullTextSearch.initialize()
        if (!hasBuiltIndex || bookIds != indexedBookIds) {
            rebuildIndex()
        }
        val booksById = books.associateBy { it.id }
        return fullTextSearch.searchAll(query, maxResults = limit, offset = offset).map { result ->
            GlobalSearchResult(
                bookId = result.bookId,
                bookTitle = booksById[result.bookId]?.title.orEmpty(),
                chapterIndex = result.chapterIndex,
                chapterTitle = result.chapterTitle,
                matchedText = result.matchedText
            )
        }
    }

    override suspend fun searchBooks(query: String): List<Book> =
        bookRepository.searchBooks(query).first()
}
