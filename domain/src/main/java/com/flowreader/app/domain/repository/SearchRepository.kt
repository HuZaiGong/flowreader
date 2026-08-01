package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.GlobalSearchResult

interface SearchRepository {
    suspend fun rebuildIndex()
    suspend fun searchAll(query: String): List<GlobalSearchResult>

    /** Paged cross-book chapter hits; callers page through with [offset]. */
    suspend fun searchChapters(query: String, limit: Int, offset: Int): List<GlobalSearchResult>

    /** Books whose title or author contains [query]. */
    suspend fun searchBooks(query: String): List<com.flowreader.app.domain.model.Book>
}
