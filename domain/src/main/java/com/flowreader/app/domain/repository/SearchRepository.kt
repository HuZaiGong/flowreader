package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.GlobalSearchResult
import com.flowreader.app.domain.model.SearchIndexProgress
import kotlinx.coroutines.flow.StateFlow

interface SearchRepository {

    /** How far index maintenance has got; drives the search screen's 「正在建立索引」 state. */
    val indexProgress: StateFlow<SearchIndexProgress>

    /**
     * Brings the index in line with the library by indexing books that are missing and dropping ones
     * that are gone — it does **not** rebuild what is already there.
     *
     * Safe and cheap to call on every visit to the search screen: with nothing to do it costs two
     * queries. Maintenance runs in an application-scoped coroutine, so a caller that gets cancelled
     * (the search screen's debounce cancels on every keystroke) leaves the work running instead of
     * abandoning a half-built index.
     */
    fun requestIndexMaintenance()

    /**
     * Suspends until index maintenance has finished, starting it if it is not already running.
     *
     * Cancelling the caller does not cancel the maintenance.
     */
    suspend fun awaitIndexReady()

    /** Discards the whole index and rebuilds it. For explicit user action and backup restore. */
    suspend fun rebuildIndex()

    /**
     * Indexes one book.
     *
     * By default this is a no-op for a book the index already holds in full, so the reader can call it
     * on every open — reopening a long novel used to re-parse and re-index every chapter. Pass
     * [force] to replace what is indexed, which is what a re-import or a repaired file needs.
     */
    suspend fun indexBook(bookId: Long, force: Boolean = false)

    /** Removes one book from the index. Call when a book is deleted. */
    suspend fun removeBookFromIndex(bookId: Long)

    suspend fun searchAll(query: String): List<GlobalSearchResult>

    /** Paged cross-book chapter hits; callers page through with [offset]. */
    suspend fun searchChapters(query: String, limit: Int, offset: Int): List<GlobalSearchResult>

    /** Books whose title or author contains [query]. */
    suspend fun searchBooks(query: String): List<Book>
}
