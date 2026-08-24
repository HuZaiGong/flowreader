package com.flowreader.app.ui.screens.search

import com.flowreader.app.domain.model.GlobalSearchResult
import com.flowreader.app.domain.model.SearchIndexProgress
import com.flowreader.app.domain.repository.SearchRepository
import com.flowreader.app.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers what made the search screen feel broken even once the index worked.
 *
 * Three separate defects live here: a two-character minimum that silently swallowed every
 * single-character Chinese query, a `catch (Exception)` that turned the debounce's own cancellation
 * into 「搜索失败」 while the user was still typing, and results that stayed partial forever because
 * nothing re-ran the query when indexing finished.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val searchRepository = mockk<SearchRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val progress = MutableStateFlow(SearchIndexProgress.Idle)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { searchRepository.indexProgress } returns progress
        every { settingsRepository.getSearchHistory() } returns flowOf(emptyList())
        coEvery { searchRepository.searchBooks(any()) } returns emptyList()
        coEvery { searchRepository.searchChapters(any(), any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SearchViewModel(searchRepository, settingsRepository)

    // ---- what counts as a query ----------------------------------------------------------------

    /**
     * The headline bug: 「爱」 is an ordinary Chinese word, and the old `length < 2` guard meant
     * typing it did nothing at all — no request, no results, no message.
     */
    @Test
    fun aSingleChineseCharacterIsSearched() = runTest(dispatcher) {
        val vm = viewModel()

        vm.updateQuery("爱")
        advanceUntilIdle()

        coVerify(exactly = 1) { searchRepository.searchChapters("爱", any(), 0) }
        coVerify(exactly = 1) { searchRepository.searchBooks("爱") }
    }

    /** A lone Latin letter is a prefix query that matches most of the library — not worth running. */
    @Test
    fun aSingleLatinCharacterIsNotSearched() = runTest(dispatcher) {
        val vm = viewModel()

        vm.updateQuery("a")
        advanceUntilIdle()

        coVerify(exactly = 0) { searchRepository.searchChapters(any(), any(), any()) }
    }

    @Test
    fun aPunctuationOnlyQueryIsNotSearched() = runTest(dispatcher) {
        val vm = viewModel()

        vm.updateQuery("、。！")
        advanceUntilIdle()

        coVerify(exactly = 0) { searchRepository.searchChapters(any(), any(), any()) }
    }

    @Test
    fun clearingTheQueryClearsTheResults() = runTest(dispatcher) {
        coEvery { searchRepository.searchChapters(any(), any(), any()) } returns listOf(hit(1))
        val vm = viewModel()

        vm.updateQuery("心流")
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.chapterResults.size)

        vm.updateQuery("")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.chapterResults.isEmpty())
        assertFalse(vm.uiState.value.isSearching)
    }

    // ---- debounce ------------------------------------------------------------------------------

    @Test
    fun onlyTheLastKeystrokeOfABurstIsSearched() = runTest(dispatcher) {
        val vm = viewModel()

        vm.updateQuery("心")
        advanceTimeBy(100)
        vm.updateQuery("心流")
        advanceTimeBy(100)
        vm.updateQuery("心流阅")
        advanceUntilIdle()

        coVerify(exactly = 1) { searchRepository.searchChapters(any(), any(), any()) }
        coVerify(exactly = 1) { searchRepository.searchChapters("心流阅", any(), 0) }
    }

    /**
     * Cancelling the debounced job must not surface as a failure. `CancellationException` extends
     * `Exception`, so the old blanket catch put 「搜索失败」 on screen mid-typing.
     */
    @Test
    fun aCancelledSearchDoesNotReportAnError() = runTest(dispatcher) {
        val vm = viewModel()

        vm.updateQuery("心")
        advanceTimeBy(400)
        vm.updateQuery("心流")
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
    }

    @Test
    fun aRealFailureIsReported() = runTest(dispatcher) {
        coEvery { searchRepository.searchChapters(any(), any(), any()) } throws
            IllegalStateException("index closed")
        val vm = viewModel()

        vm.updateQuery("心流")
        advanceUntilIdle()

        val error = vm.uiState.value.error
        assertTrue("expected a failure message, got $error", error?.contains("搜索失败") == true)
        assertFalse(vm.uiState.value.isSearching)
    }

    // ---- index progress ------------------------------------------------------------------------

    /** Opening the screen is the signal the index is about to be needed. */
    @Test
    fun openingTheScreenRequestsIndexMaintenance() = runTest(dispatcher) {
        viewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { searchRepository.requestIndexMaintenance() }
    }

    @Test
    fun indexProgressReachesTheUiState() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        progress.value = SearchIndexProgress(isIndexing = true, booksIndexed = 2, booksTotal = 7)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.indexProgress.booksIndexed)
        assertEquals(7, vm.uiState.value.indexProgress.booksTotal)
        assertTrue(vm.uiState.value.chapterResultsArePartial)
    }

    /**
     * Without the re-run, a query issued mid-build kept its partial hit list until the user retyped
     * it — which reads as search missing text that is plainly in the book.
     */
    @Test
    fun finishingTheIndexRerunsTheCurrentQuery() = runTest(dispatcher) {
        val vm = viewModel()
        progress.value = SearchIndexProgress(isIndexing = true, booksIndexed = 0, booksTotal = 3)
        advanceUntilIdle()

        vm.updateQuery("心流")
        advanceUntilIdle()
        coVerify(exactly = 1) { searchRepository.searchChapters("心流", any(), 0) }

        progress.value = SearchIndexProgress.Idle
        advanceUntilIdle()

        coVerify(exactly = 2) { searchRepository.searchChapters("心流", any(), 0) }
        assertFalse(vm.uiState.value.chapterResultsArePartial)
    }

    @Test
    fun finishingTheIndexWithNoQueryRunsNoSearch() = runTest(dispatcher) {
        viewModel()
        progress.value = SearchIndexProgress(isIndexing = true, booksIndexed = 0, booksTotal = 3)
        advanceUntilIdle()
        progress.value = SearchIndexProgress.Idle
        advanceUntilIdle()

        coVerify(exactly = 0) { searchRepository.searchChapters(any(), any(), any()) }
    }

    // ---- paging --------------------------------------------------------------------------------

    @Test
    fun afullPageOffersMore() = runTest(dispatcher) {
        val page = List(SearchViewModel.CHAPTER_PAGE_SIZE) { hit(it) }
        coEvery { searchRepository.searchChapters(any(), any(), 0) } returns page
        val vm = viewModel()

        vm.updateQuery("心流")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.hasMoreChapters)
    }

    @Test
    fun loadMoreAppendsAtTheNextOffset() = runTest(dispatcher) {
        val page = List(SearchViewModel.CHAPTER_PAGE_SIZE) { hit(it) }
        coEvery { searchRepository.searchChapters(any(), any(), 0) } returns page
        coEvery { searchRepository.searchChapters(any(), any(), SearchViewModel.CHAPTER_PAGE_SIZE) } returns
            listOf(hit(999))
        val vm = viewModel()

        vm.updateQuery("心流")
        advanceUntilIdle()
        vm.loadMoreChapters()
        advanceUntilIdle()

        assertEquals(page.size + 1, vm.uiState.value.chapterResults.size)
        assertFalse("a short page ends the list", vm.uiState.value.hasMoreChapters)
    }

    @Test
    fun loadMoreDoesNothingWithoutASearchableQuery() = runTest(dispatcher) {
        val vm = viewModel()

        vm.updateQuery("a")
        advanceUntilIdle()
        vm.loadMoreChapters()
        advanceUntilIdle()

        coVerify(exactly = 0) { searchRepository.searchChapters(any(), any(), any()) }
    }

    // ---- history -------------------------------------------------------------------------------

    @Test
    fun aSuccessfulSearchIsRecordedInHistory() = runTest(dispatcher) {
        val vm = viewModel()

        vm.updateQuery("  心流  ")
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.addSearchHistory("心流") }
    }

    @Test
    fun aFailedSearchIsNotRecordedInHistory() = runTest(dispatcher) {
        coEvery { searchRepository.searchChapters(any(), any(), any()) } throws
            IllegalStateException("index closed")
        val vm = viewModel()

        vm.updateQuery("心流")
        advanceUntilIdle()

        coVerify(exactly = 0) { settingsRepository.addSearchHistory(any()) }
    }

    @Test
    fun usingAHistoryEntrySearchesIt() = runTest(dispatcher) {
        val vm = viewModel()

        vm.useHistory("心流")
        advanceUntilIdle()

        assertEquals("心流", vm.uiState.value.query)
        coVerify(exactly = 1) { searchRepository.searchChapters("心流", any(), 0) }
    }

    // ---- fixtures ------------------------------------------------------------------------------

    private fun hit(index: Int) = GlobalSearchResult(
        bookId = 1L,
        bookTitle = "书",
        chapterIndex = index,
        chapterTitle = "第${index + 1}章",
        matchedText = "命中片段",
        matchStart = 0,
        matchLength = 2
    )
}
