package com.flowreader.app.data.repository

import android.util.Log
import com.flowreader.app.di.ApplicationScope
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.model.GlobalSearchResult
import com.flowreader.app.domain.model.SearchIndexProgress
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.domain.repository.SearchRepository
import com.flowreader.app.util.FullTextSearch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val fullTextSearch: FullTextSearch,
    @ApplicationScope private val appScope: CoroutineScope
) : SearchRepository {

    private val _indexProgress = MutableStateFlow(SearchIndexProgress.Idle)
    override val indexProgress: StateFlow<SearchIndexProgress> = _indexProgress.asStateFlow()

    /** The single in-flight maintenance job, so concurrent callers join one run instead of racing. */
    private var maintenanceJob: Job? = null
    private val jobMutex = Mutex()

    override fun requestIndexMaintenance() {
        appScope.launch { startOrJoinMaintenance() }
    }

    override suspend fun awaitIndexReady() {
        // join() rather than running the work here: if the caller is cancelled — the search screen
        // cancels its debounced job on every keystroke — join() throws but the maintenance carries on
        // in the application scope. Doing the work inline is what made a half-built index the norm.
        startOrJoinMaintenance().join()
    }

    private suspend fun startOrJoinMaintenance(): Job = jobMutex.withLock {
        maintenanceJob?.takeIf { it.isActive } ?: appScope.launch { runMaintenance() }.also { maintenanceJob = it }
    }

    /**
     * Indexes books the index is missing and drops ones the library no longer has.
     *
     * Deliberately not a rebuild: `searchChapters()` used to trigger a full re-parse of every book
     * whenever the library's id set differed from an in-memory snapshot, which meant the first search
     * of every launch (the snapshot did not survive process death) re-read the whole library on the
     * UI's debounce job. Diffing against the index's own completion markers makes the steady state
     * two queries and makes an interrupted run resume instead of restart.
     *
     * Never throws: it runs detached in [appScope], where an escaping exception would reach the
     * default handler and crash the process.
     */
    private suspend fun runMaintenance() {
        try {
            val books = searchableBooks()
            val indexed = fullTextSearch.indexedBookIds()
            val missing = books.filter { it.id !in indexed }
            val stale = indexed - books.map { it.id }.toSet()

            if (missing.isEmpty() && stale.isEmpty()) {
                _indexProgress.value = SearchIndexProgress.Idle
                return
            }

            _indexProgress.value = SearchIndexProgress(
                isIndexing = true,
                booksIndexed = 0,
                booksTotal = missing.size
            )

            fullTextSearch.withIndexLock {
                stale.forEach { fullTextSearch.deleteBookContent(it) }
                missing.forEachIndexed { done, book ->
                    indexBookLocked(book)
                    _indexProgress.value = _indexProgress.value.copy(booksIndexed = done + 1)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Leaving the index short of a book degrades search; it must not take the app with it.
            Log.e(TAG, "Search index maintenance failed", e)
        } finally {
            _indexProgress.value = SearchIndexProgress.Idle
        }
    }

    /**
     * The index lock lives in [FullTextSearch] so the reader's per-book re-index cannot interleave
     * with maintenance either. [FullTextSearch.withIndexLock] is not re-entrant, so every entry point
     * takes it exactly once and delegates to a `*Locked` helper.
     */
    override suspend fun rebuildIndex() {
        fullTextSearch.withIndexLock {
            fullTextSearch.deleteAllContent()
            searchableBooks().forEach { indexBookLocked(it) }
        }
    }

    override suspend fun indexBook(bookId: Long, force: Boolean) {
        val book = bookRepository.getBookById(bookId) ?: return
        if (book.format == BookFormat.COMIC) return
        if (!force && bookId in fullTextSearch.indexedBookIds()) return
        fullTextSearch.withIndexLock { indexBookLocked(book) }
    }

    override suspend fun removeBookFromIndex(bookId: Long) {
        fullTextSearch.deleteBooks(listOf(bookId))
    }

    private suspend fun indexBookLocked(book: Book) {
        // Clear first: re-indexing a book whose chapter count shrank would otherwise leave the
        // dropped chapters searchable.
        fullTextSearch.deleteBookContent(book.id)
        val chapters = chapterRepository.getChapterMetadataList(book.id)
        chapters.forEach { chapter ->
            val content = chapterRepository.getChapterContent(book.id, chapter.index).orEmpty()
            if (content.isNotBlank()) {
                fullTextSearch.indexChapter(book.id, chapter.index, chapter.title, content)
            }
        }
        // Only now is the book complete. Marking it earlier would let an interrupted run look done.
        fullTextSearch.markBookIndexed(book.id, chapters.size)
    }

    /**
     * Comics hold no text, so indexing them is pure cost: every page is an image, every chapter reads
     * back blank, and on a library of scanned volumes the rebuild spent its time decoding archives to
     * index nothing.
     */
    private suspend fun searchableBooks(): List<Book> =
        bookRepository.getAllBooks().first().filter { it.format != BookFormat.COMIC }

    override suspend fun searchAll(query: String): List<GlobalSearchResult> =
        searchChapters(query, limit = 100, offset = 0)

    /**
     * Searches what is currently indexed. Maintenance is requested but **not** waited for — a query
     * must return the hits it can immediately, and [indexProgress] tells the UI when those hits are
     * still partial.
     */
    override suspend fun searchChapters(query: String, limit: Int, offset: Int): List<GlobalSearchResult> {
        requestIndexMaintenance()
        val booksById = bookRepository.getAllBooks().first().associateBy { it.id }
        return fullTextSearch.searchAll(query, maxResults = limit, offset = offset).map { result ->
            GlobalSearchResult(
                bookId = result.bookId,
                bookTitle = booksById[result.bookId]?.title.orEmpty(),
                chapterIndex = result.chapterIndex,
                chapterTitle = result.chapterTitle,
                matchedText = result.matchedText,
                matchStart = result.matchStart,
                matchLength = result.matchLength
            )
        }
    }

    override suspend fun searchBooks(query: String): List<Book> =
        bookRepository.searchBooks(query).first()

    private companion object {
        const val TAG = "SearchRepositoryImpl"
    }
}
