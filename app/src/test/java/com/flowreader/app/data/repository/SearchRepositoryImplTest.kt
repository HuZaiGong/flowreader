package com.flowreader.app.data.repository

import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.SearchIndexProgress
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.util.FullTextSearch
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Tests the index *lifecycle*, not the queries — `FullTextSearchEngineTest` covers those against a
 * real FTS5 build.
 *
 * The behaviour under test is what made global search unusable: `searchChapters()` compared the
 * library's id set against an in-memory flag that did not survive process death, so the first search
 * of every launch re-parsed and re-indexed the entire library — inline, on a job the search screen
 * cancelled on the very next keystroke.
 */
class SearchRepositoryImplTest {

    private val bookRepository = mockk<BookRepository>(relaxed = true)
    private val chapterRepository = mockk<ChapterRepository>(relaxed = true)
    private val fullTextSearch = mockk<FullTextSearch>(relaxed = true) {
        // withIndexLock takes a suspending block, which a relaxed mock would never invoke — every
        // assertion about what maintenance writes depends on that block actually running.
        coEvery { withIndexLock<Unit>(any()) } coAnswers { firstArg<suspend () -> Unit>().invoke() }
    }

    @Test
    fun maintenanceOnlyIndexesBooksTheIndexIsMissing() = runTest {
        givenLibrary(book(1), book(2), book(3))
        givenIndexed(1L, 2L)
        val repository = newRepository(backgroundScope)

        repository.awaitIndexReady()

        coVerify(exactly = 1) { fullTextSearch.markBookIndexed(3L, 2) }
        coVerify(exactly = 0) { fullTextSearch.markBookIndexed(1L, any()) }
        coVerify(exactly = 0) { fullTextSearch.markBookIndexed(2L, any()) }
    }

    /** An empty marker set is the fresh-install and post-migration case: index everything. */
    @Test
    fun anEmptyIndexIndexesTheWholeLibrary() = runTest {
        givenLibrary(book(1), book(2))
        givenIndexed()
        val repository = newRepository(backgroundScope)

        repository.awaitIndexReady()

        coVerify(exactly = 1) { fullTextSearch.markBookIndexed(1L, 2) }
        coVerify(exactly = 1) { fullTextSearch.markBookIndexed(2L, 2) }
    }

    /** Books the library no longer has must go, or their text keeps producing hits that open nothing. */
    @Test
    fun maintenanceDropsBooksTheLibraryNoLongerHas() = runTest {
        givenLibrary(book(1))
        givenIndexed(1L, 99L)
        val repository = newRepository(backgroundScope)

        repository.awaitIndexReady()

        coVerify(exactly = 1) { fullTextSearch.deleteBookContent(99L) }
        coVerify(exactly = 0) { fullTextSearch.markBookIndexed(any(), any()) }
    }

    @Test
    fun nothingToDoCostsNoIndexing() = runTest {
        givenLibrary(book(1))
        givenIndexed(1L)
        val repository = newRepository(backgroundScope)

        repository.awaitIndexReady()

        coVerify(exactly = 0) { fullTextSearch.withIndexLock<Unit>(any()) }
        coVerify(exactly = 0) { fullTextSearch.indexChapter(any(), any(), any(), any()) }
        assertFalse(repository.indexProgress.value.isIndexing)
    }

    /**
     * Comics hold no text. Indexing them decoded every archive to store nothing, which on a library
     * of scanned volumes was the bulk of the rebuild.
     */
    @Test
    fun comicsAreNeverIndexed() = runTest {
        givenLibrary(book(1), book(2, BookFormat.COMIC))
        givenIndexed()
        val repository = newRepository(backgroundScope)

        repository.awaitIndexReady()

        coVerify(exactly = 1) { fullTextSearch.markBookIndexed(1L, 2) }
        coVerify(exactly = 0) { fullTextSearch.markBookIndexed(2L, any()) }

        repository.indexBook(2L, force = true)
        coVerify(exactly = 0) { fullTextSearch.indexChapter(2L, any(), any(), any()) }
    }

    /** The completion marker must be written last, so an interrupted run reads as unfinished. */
    @Test
    fun aBookIsClearedBeforeIndexingAndMarkedAfterwards() = runTest {
        givenLibrary(book(1), chaptersPerBook = 3)
        givenIndexed()
        val repository = newRepository(backgroundScope)

        repository.awaitIndexReady()

        coVerify(ordering = Ordering.ORDERED) {
            fullTextSearch.deleteBookContent(1L)
            fullTextSearch.indexChapter(1L, 0, any(), any())
            fullTextSearch.indexChapter(1L, 1, any(), any())
            fullTextSearch.indexChapter(1L, 2, any(), any())
            fullTextSearch.markBookIndexed(1L, 3)
        }
    }

    @Test
    fun blankChaptersAreSkippedButStillCounted() = runTest {
        givenLibrary(book(1), chaptersPerBook = 2)
        givenIndexed()
        coEvery { chapterRepository.getChapterContent(1L, 0) } returns "   "
        coEvery { chapterRepository.getChapterContent(1L, 1) } returns "有内容"
        val repository = newRepository(backgroundScope)

        repository.awaitIndexReady()

        coVerify(exactly = 0) { fullTextSearch.indexChapter(1L, 0, any(), any()) }
        coVerify(exactly = 1) { fullTextSearch.indexChapter(1L, 1, any(), any()) }
        // The marker records the book's chapter count, not how many chapters held text.
        coVerify(exactly = 1) { fullTextSearch.markBookIndexed(1L, 2) }
    }

    /** The reader calls this on every open; re-indexing a long novel each time was the old cost. */
    @Test
    fun indexingAnAlreadyIndexedBookIsANoOp() = runTest {
        givenLibrary(book(1))
        givenIndexed(1L)
        val repository = newRepository(backgroundScope)

        repository.indexBook(1L)

        coVerify(exactly = 0) { fullTextSearch.indexChapter(any(), any(), any(), any()) }
    }

    @Test
    fun forcingReindexesAnAlreadyIndexedBook() = runTest {
        givenLibrary(book(1), chaptersPerBook = 1)
        givenIndexed(1L)
        val repository = newRepository(backgroundScope)

        repository.indexBook(1L, force = true)

        coVerify(exactly = 1) { fullTextSearch.indexChapter(1L, 0, any(), any()) }
        coVerify(exactly = 1) { fullTextSearch.markBookIndexed(1L, 1) }
    }

    /**
     * Concurrent callers must join one run rather than each starting a full pass.
     *
     * The wait is `awaitIndexReady()`, not `advanceUntilIdle()`: since coroutines-test 1.7 the
     * scheduler only advances *foreground* work, so a fire-and-forget `requestIndexMaintenance()`
     * would never run and every assertion below would pass for the wrong reason. Joining the shared
     * job is also the property under test — if coalescing broke, `awaitIndexReady()` would join a
     * sixth run and the counts would go up.
     */
    @Test
    fun concurrentRequestsShareASingleRun() = runTest {
        givenLibrary(book(1), book(2))
        givenIndexed()
        val repository = newRepository(backgroundScope)

        repeat(5) { repository.requestIndexMaintenance() }
        repository.awaitIndexReady()

        coVerify(exactly = 1) { fullTextSearch.markBookIndexed(1L, 2) }
        coVerify(exactly = 1) { fullTextSearch.markBookIndexed(2L, 2) }
        coVerify(exactly = 1) { fullTextSearch.withIndexLock<Unit>(any()) }
    }

    /**
     * A search must return what is indexed *now*. Waiting would put the whole first-launch index
     * build in front of the user's first query, and rebuilding inline is what the old code did.
     */
    @Test
    fun searchingNeitherWaitsForNorRebuildsTheIndex() = runTest {
        givenLibrary(book(1))
        givenIndexed()
        coEvery { fullTextSearch.searchAll(any(), any(), any()) } returns emptyList()
        val repository = newRepository(backgroundScope)

        repository.searchChapters("心流", limit = 20, offset = 0)

        coVerify(exactly = 1) { fullTextSearch.searchAll("心流", 20, 0) }
        coVerify(exactly = 0) { fullTextSearch.deleteAllContent() }
    }

    /** A failure must not escape the application scope, where it would reach the crash handler. */
    @Test
    fun aFailingIndexDoesNotPropagate() = runTest {
        givenLibrary(book(1))
        coEvery { fullTextSearch.indexedBookIds() } throws IllegalStateException("index closed")
        val repository = newRepository(backgroundScope)

        repository.awaitIndexReady()

        assertFalse(repository.indexProgress.value.isIndexing)
    }

    @Test
    fun progressCountsBooksAsTheyFinish() = runTest {
        givenLibrary(book(1), book(2), book(3))
        givenIndexed()
        // Sampled synchronously inside the run: StateFlow conflates, so collecting from another
        // coroutine would legitimately miss intermediate values and make this flaky.
        var repository: SearchRepositoryImpl? = null
        val samples = mutableListOf<Pair<Int, Int>>()
        coEvery { fullTextSearch.markBookIndexed(any(), any()) } coAnswers {
            repository?.indexProgress?.value?.let { samples += it.booksIndexed to it.booksTotal }
        }
        val created = newRepository(backgroundScope)
        repository = created

        created.awaitIndexReady()

        // Each sample is taken as a book's last write lands, so it counts books already finished.
        assertEquals(listOf(0 to 3, 1 to 3, 2 to 3), samples)
        assertEquals(SearchIndexProgress.Idle, created.indexProgress.value)
    }

    // ---- fixtures ------------------------------------------------------------------------------

    private fun book(id: Long, format: BookFormat = BookFormat.EPUB) =
        Book(id = id, title = "书 $id", author = "作者", filePath = "/tmp/$id", format = format)

    private fun givenLibrary(vararg books: Book, chaptersPerBook: Int = 2) {
        coEvery { bookRepository.getAllBooks() } returns flowOf(books.toList())
        books.forEach { b ->
            coEvery { bookRepository.getBookById(b.id) } returns b
            coEvery { chapterRepository.getChapterMetadataList(b.id) } returns
                (0 until chaptersPerBook).map { Chapter(bookId = b.id, index = it, title = "第${it + 1}章") }
        }
        coEvery { chapterRepository.getChapterContent(any(), any()) } returns "正文内容"
    }

    private fun givenIndexed(vararg ids: Long) {
        coEvery { fullTextSearch.indexedBookIds() } returns ids.toSet()
    }

    private fun newRepository(scope: CoroutineScope) =
        SearchRepositoryImpl(bookRepository, chapterRepository, fullTextSearch, scope)
}
