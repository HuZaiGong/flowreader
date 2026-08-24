package com.flowreader.app.domain.model

/**
 * How far the full-text index has got through the library.
 *
 * Global search can only find text that is already indexed, and building the index means parsing
 * every chapter of every book — slow enough on a large library to look like "search is broken" if
 * the UI says nothing. Exposing progress lets the search screen distinguish 「还没索引完」 from
 * 「确实没有匹配」.
 */
data class SearchIndexProgress(
    val isIndexing: Boolean = false,
    val booksIndexed: Int = 0,
    val booksTotal: Int = 0
) {
    /** True while indexing has books left to process, so partial results need explaining. */
    val isIncomplete: Boolean get() = isIndexing && booksIndexed < booksTotal

    companion object {
        val Idle = SearchIndexProgress()
    }
}
