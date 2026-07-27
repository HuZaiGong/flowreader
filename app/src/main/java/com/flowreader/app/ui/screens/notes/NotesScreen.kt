package com.flowreader.app.ui.screens.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.R
import com.flowreader.app.core.designsystem.component.FlowScaffold
import com.flowreader.app.core.designsystem.component.FlowStateHost
import com.flowreader.app.core.designsystem.component.FlowTopBar
import com.flowreader.app.core.designsystem.token.FlowRadius
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.domain.repository.AnnotationExportFormat

/**
 * Every highlight and note in the library, in one place.
 *
 * Before v53 an annotation only existed inside the book that produced it, so "what did I write
 * about focus last year" meant opening books one by one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onBackClick: () -> Unit,
    onOpenPassage: (Long, Int) -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExportMenu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    FlowScaffold(
        topBar = {
            FlowTopBar(
                title = stringResource(R.string.notes_title),
                onNavigateUp = onBackClick,
                actions = {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.IosShare, contentDescription = stringResource(R.string.notes_export))
                    }
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        AnnotationExportFormat.entries.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format.name) },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.exportVisible(format)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.updateQuery(it) },
                label = { Text(stringResource(R.string.notes_search_placeholder)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FlowSpacing.lg, vertical = FlowSpacing.sm)
            )

            if (uiState.books.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm),
                    contentPadding = PaddingValues(horizontal = FlowSpacing.lg)
                ) {
                    item(key = "all") {
                        FilterChip(
                            selected = uiState.selectedBookId == null,
                            onClick = { viewModel.selectBook(null) },
                            label = { Text(stringResource(R.string.notes_filter_all)) }
                        )
                    }
                    items(uiState.books, key = { it.id }) { book ->
                        FilterChip(
                            selected = uiState.selectedBookId == book.id,
                            onClick = { viewModel.selectBook(book.id) },
                            label = { Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }

            FlowStateHost(
                isLoading = uiState.isLoading,
                isEmpty = uiState.notes.isEmpty(),
                error = null,
                modifier = Modifier.fillMaxSize(),
                emptyTitle = stringResource(R.string.notes_empty_title),
                emptyMessage = stringResource(R.string.notes_empty_message),
                emptyIcon = Icons.AutoMirrored.Filled.MenuBook
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(FlowSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(FlowSpacing.md)
                ) {
                    items(uiState.notes, key = { it.annotation.id }) { item ->
                        NoteCard(
                            item = item,
                            onOpen = { onOpenPassage(item.annotation.bookId, item.annotation.chapterIndex) },
                            onDelete = { viewModel.deleteNote(item.annotation.id) }
                        )
                    }
                }
            }
        }
    }

    val exportText = uiState.exportText
    if (exportText != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExport() },
            title = { Text(stringResource(R.string.notes_export_result, uiState.exportedCount)) },
            text = {
                Text(
                    text = exportText.take(EXPORT_PREVIEW_CHARS),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(exportText))
                        viewModel.clearExport()
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearExport() }) { Text(stringResource(R.string.action_close)) }
            }
        )
    }
}

private const val EXPORT_PREVIEW_CHARS = 4000

@Composable
private fun NoteCard(item: NoteItem, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(FlowSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(item.annotation.color.colorValue),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                ) {}
                Text(
                    text = stringResource(
                        R.string.notes_source,
                        item.bookTitle,
                        item.annotation.chapterIndex + 1
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = FlowSpacing.sm)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }

            Text(
                text = item.annotation.selectedText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(FlowRadius.sm))
                    .clickable(onClick = onOpen)
                    .padding(vertical = FlowSpacing.xs)
            )

            if (item.annotation.note.isNotBlank()) {
                Text(
                    text = item.annotation.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = FlowSpacing.xs)
                )
            }

            TextButton(onClick = onOpen, modifier = Modifier.padding(top = FlowSpacing.xs)) {
                Text(stringResource(R.string.notes_open_book))
            }
        }
    }
}
