package com.flowreader.app.ui.screens.opds

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.util.BookParser
import com.flowreader.app.util.OpdsClient
import com.flowreader.app.util.OpdsEntry
import com.flowreader.app.util.OpdsFeed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpdsUiState(
    val url: String = "",
    val feed: OpdsFeed? = null,
    val breadcrumbs: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val downloadingTitle: String? = null,
    val importedTitle: String? = null,
    val error: String? = null
)

/**
 * LAN-only OPDS browsing and import.
 *
 * The address guard lives in `OpdsAddress` and is enforced on every hop by [OpdsClient]; this
 * ViewModel only sequences fetch → download → parse → insert. A downloaded file is deleted from
 * the cache as soon as the parser has copied it into internal storage.
 */
@HiltViewModel
class OpdsViewModel @Inject constructor(
    private val opdsClient: OpdsClient,
    private val bookParser: BookParser,
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpdsUiState())
    val uiState: StateFlow<OpdsUiState> = _uiState.asStateFlow()

    fun updateUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun connect() {
        load(_uiState.value.url, resetBreadcrumbs = true)
    }

    fun openEntry(entry: OpdsEntry) {
        val target = entry.navigationUrl ?: return
        load(target, resetBreadcrumbs = false)
    }

    fun loadNextPage() {
        val next = _uiState.value.feed?.nextUrl ?: return
        load(next, resetBreadcrumbs = false)
    }

    fun goBack() {
        val crumbs = _uiState.value.breadcrumbs
        if (crumbs.size < 2) return
        val previous = crumbs[crumbs.size - 2]
        _uiState.update { it.copy(breadcrumbs = crumbs.dropLast(1)) }
        load(previous, resetBreadcrumbs = false, pushCrumb = false)
    }

    private fun load(rawUrl: String, resetBreadcrumbs: Boolean, pushCrumb: Boolean = true) {
        if (rawUrl.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            opdsClient.loadCatalog(rawUrl)
                .onSuccess { feed ->
                    _uiState.update { state ->
                        val crumbs = when {
                            resetBreadcrumbs -> listOf(feed.url)
                            pushCrumb -> state.breadcrumbs + feed.url
                            else -> state.breadcrumbs
                        }
                        state.copy(feed = feed, breadcrumbs = crumbs, isLoading = false, url = feed.url)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "连接失败") }
                }
        }
    }

    fun download(entry: OpdsEntry) {
        val url = entry.acquisitionUrl ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingTitle = entry.title, error = null) }
            opdsClient.download(url, fileNameFor(entry, url))
                .mapCatching { file ->
                    val result = bookParser.parseBook(Uri.fromFile(file)).getOrThrow()
                    val internalPath = result.pdfFilePath ?: bookParser.copyFileToInternal(Uri.fromFile(file))
                    val bookId = bookRepository.insertBook(result.book.copy(filePath = internalPath ?: ""))
                    chapterRepository.insertChapters(result.chapters.map { it.copy(bookId = bookId) })
                    file.delete()
                    result.book.title
                }
                .onSuccess { title ->
                    _uiState.update { it.copy(downloadingTitle = null, importedTitle = title) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(downloadingTitle = null, error = error.message ?: "导入失败") }
                }
        }
    }

    fun clearImported() {
        _uiState.update { it.copy(importedTitle = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * OPDS acquisition links rarely end in a filename, so the extension is derived from the
     * declared media type — the parser dispatches on extension and would otherwise see nothing.
     */
    private fun fileNameFor(entry: OpdsEntry, url: String): String {
        val fromUrl = url.substringAfterLast('/').substringBefore('?')
        if (BookParser.detectFormatStatic(fromUrl) != BookFormat.UNKNOWN) return fromUrl
        val extension = when {
            entry.acquisitionType?.contains("epub") == true -> "epub"
            entry.acquisitionType?.contains("pdf") == true -> "pdf"
            entry.acquisitionType?.contains("mobipocket") == true -> "mobi"
            entry.acquisitionType?.contains("fictionbook") == true -> "fb2"
            entry.acquisitionType?.contains("plain") == true -> "txt"
            else -> "epub"
        }
        return "${entry.title}.$extension"
    }
}
