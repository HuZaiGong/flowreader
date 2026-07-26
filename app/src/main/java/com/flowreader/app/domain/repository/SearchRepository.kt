package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.GlobalSearchResult

interface SearchRepository {
    suspend fun rebuildIndex()
    suspend fun searchAll(query: String): List<GlobalSearchResult>
}
