package com.flowreader.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.core.util.CjkTokenizer
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.GlobalSearchResult
import com.flowreader.app.domain.model.SearchIndexProgress
import com.flowreader.app.domain.repository.SearchRepository
import com.flowreader.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val history: List<String> = emptyList(),
    val bookResults: List<Book> = emptyList(),
    val chapterResults: List<GlobalSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val hasMoreChapters: Boolean = false,
    val indexProgress: SearchIndexProgress = SearchIndexProgress.Idle,
    val error: String? = null
) {
    /** Chapter hits can only be partial while the index is still filling in. */
    val chapterResultsArePartial: Boolean get() = indexProgress.isIndexing
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var chapterOffset = 0

    init {
        viewModelScope.launch {
            settingsRepository.getSearchHistory().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
        // Opening the search screen is the signal that the index is about to be needed. The work runs
        // in the application scope, so leaving the screen no longer abandons a half-built index.
        searchRepository.requestIndexMaintenance()
        observeIndexProgress()
    }

    /**
     * Mirrors index progress into the UI state and re-runs the query once indexing finishes.
     *
     * Without the re-run, a search issued while the index was still building would show its partial
     * hit list forever — the user's only recovery was to retype the query, which looked like search
     * being broken rather than merely late.
     */
    private fun observeIndexProgress() {
        viewModelScope.launch {
            var wasIndexing = false
            searchRepository.indexProgress.collect { progress ->
                _uiState.update { it.copy(indexProgress = progress) }
                if (wasIndexing && !progress.isIndexing) {
                    val current = _uiState.value.query
                    if (CjkTokenizer.isSearchableQuery(current)) search(current)
                }
                wasIndexing = progress.isIndexing
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            search(query)
        }
    }

    private suspend fun search(query: String) {
        val trimmed = query.trim()
        // Not a length check: a single Chinese character is a real query, a single Latin letter is not.
        if (!CjkTokenizer.isSearchableQuery(trimmed)) {
            _uiState.update { it.copy(bookResults = emptyList(), chapterResults = emptyList(), isSearching = false) }
            return
        }
        _uiState.update { it.copy(isSearching = true, error = null) }
        try {
            chapterOffset = 0
            val books = searchRepository.searchBooks(trimmed)
            val chapters = searchRepository.searchChapters(trimmed, limit = CHAPTER_PAGE_SIZE, offset = 0)
            _uiState.update {
                it.copy(
                    bookResults = books,
                    chapterResults = chapters,
                    isSearching = false,
                    hasMoreChapters = chapters.size >= CHAPTER_PAGE_SIZE
                )
            }
            settingsRepository.addSearchHistory(trimmed)
        } catch (e: CancellationException) {
            // Every keystroke cancels the previous debounced job. Reporting that as a failure put
            // 「搜索失败」 on screen while the user was still typing.
            throw e
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isSearching = false, error = "搜索失败: ${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    fun loadMoreChapters() {
        val query = _uiState.value.query.trim()
        if (!CjkTokenizer.isSearchableQuery(query) || _uiState.value.isSearching) return
        viewModelScope.launch {
            try {
                chapterOffset += CHAPTER_PAGE_SIZE
                val more = searchRepository.searchChapters(query, limit = CHAPTER_PAGE_SIZE, offset = chapterOffset)
                _uiState.update {
                    it.copy(
                        chapterResults = it.chapterResults + more,
                        hasMoreChapters = more.size >= CHAPTER_PAGE_SIZE
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "加载更多失败: ${e.localizedMessage ?: "未知错误"}") }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            settingsRepository.clearSearchHistory()
        }
    }

    fun useHistory(query: String) {
        updateQuery(query)
    }

    companion object {
        const val CHAPTER_PAGE_SIZE = 20
    }
}
