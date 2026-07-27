package com.flowreader.app.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.core.util.AnnotationExporter
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.repository.AnnotationExportFormat
import com.flowreader.app.domain.repository.AnnotationRepository
import com.flowreader.app.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** An annotation resolved against the book it came from. */
data class NoteItem(
    val annotation: Annotation,
    val bookTitle: String
)

data class NotesUiState(
    val notes: List<NoteItem> = emptyList(),
    val books: List<Book> = emptyList(),
    val selectedBookId: Long? = null,
    val query: String = "",
    val isLoading: Boolean = true,
    val exportText: String? = null,
    val exportedCount: Int = 0,
    val error: String? = null
)

/**
 * Cross-book notes (v53).
 *
 * The join to book titles happens here rather than in SQL: annotations already stream from Room as
 * a Flow and the shelf is small, so a second reactive query keeps the screen live when either side
 * changes — renaming a book updates its notes without a reload.
 */
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val annotationRepository: AnnotationRepository,
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private val query = MutableStateFlow("")
    private val bookFilter = MutableStateFlow<Long?>(null)

    init {
        observeNotes()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            combine(
                annotationRepository.getAllAnnotations(),
                bookRepository.getAllBooks(),
                query,
                bookFilter
            ) { annotations, books, text, filter ->
                val titles = books.associate { it.id to it.title }
                val trimmed = text.trim()
                val visible = annotations
                    .filter { filter == null || it.bookId == filter }
                    .filter { annotation ->
                        trimmed.isEmpty() ||
                            annotation.selectedText.contains(trimmed, ignoreCase = true) ||
                            annotation.note.contains(trimmed, ignoreCase = true)
                    }
                    .map { NoteItem(it, titles[it.bookId].orEmpty()) }

                // Only books that actually carry annotations become filter chips; the full shelf
                // would bury the three books the user has ever highlighted.
                val annotated = books.filter { book -> annotations.any { it.bookId == book.id } }
                Triple(visible, annotated, trimmed)
            }.collect { (visible, annotated, trimmed) ->
                _uiState.update {
                    it.copy(
                        notes = visible,
                        books = annotated,
                        query = trimmed,
                        selectedBookId = bookFilter.value,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateQuery(text: String) {
        query.value = text
    }

    fun selectBook(bookId: Long?) {
        bookFilter.value = bookId
    }

    fun deleteNote(annotationId: Long) {
        viewModelScope.launch {
            runCatching { annotationRepository.deleteAnnotationById(annotationId) }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
        }
    }

    /** Exports exactly what is on screen, so a book filter or a search narrows the export too. */
    fun exportVisible(format: AnnotationExportFormat) {
        val visible = _uiState.value.notes
        val titles = visible.associate { it.annotation.bookId to it.bookTitle }
        val text = AnnotationExporter.export(
            annotations = visible.map { it.annotation },
            format = format,
            titleOf = { bookId -> titles[bookId]?.takeIf { it.isNotBlank() } }
        )
        _uiState.update { it.copy(exportText = text, exportedCount = visible.size) }
    }

    fun clearExport() {
        _uiState.update { it.copy(exportText = null, exportedCount = 0) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
