package com.flowreader.app.ui.screens.reader.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.flowreader.app.core.designsystem.token.FlowSpacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.flowreader.app.R
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.util.FtsSearchResult

@Composable
fun AnnotationsDialog(
    annotations: List<Annotation>,
    onAnnotationSelect: (Annotation) -> Unit,
    onAnnotationDelete: (Annotation) -> Unit,
    onAnnotationNoteUpdate: (Long, String) -> Unit,
    onDismiss: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    backgroundColor: androidx.compose.ui.graphics.Color
) {
    var editingNoteId by remember { mutableStateOf<Long?>(null) }
    var editingNoteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_menu_annotations)) },
        text = {
            if (annotations.isEmpty()) {
                Text(stringResource(R.string.reader_annotations_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(annotations.size, key = { annotations[it].id }) { index ->
                        val annotation = annotations[index]
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = annotation.selectedText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                if (annotation.note.isNotBlank()) {
                                    Text(
                                        text = stringResource(R.string.reader_annotation_note, annotation.note),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            Color(annotation.color.colorValue),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        editingNoteId = annotation.id
                                        editingNoteText = annotation.note
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.reader_edit_note))
                                    }
                                    IconButton(onClick = { onAnnotationDelete(annotation) }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onAnnotationSelect(annotation) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )

    if (editingNoteId != null) {
        AlertDialog(
            onDismissRequest = { editingNoteId = null },
            title = { Text(stringResource(R.string.reader_edit_note)) },
            text = {
                OutlinedTextField(
                    value = editingNoteText,
                    onValueChange = { editingNoteText = it },
                    label = { Text(stringResource(R.string.reader_note_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editingNoteId?.let { id ->
                        onAnnotationNoteUpdate(id, editingNoteText)
                    }
                    editingNoteId = null
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNoteId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun ShareProgressDialog(
    shareText: String,
    onDismiss: () -> Unit,
    onShare: (Intent) -> Unit,
    onShareCard: () -> Unit
) {
    val shareLabel = stringResource(R.string.reader_share_chooser)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_share_progress_title)) },
        text = {
            Column {
                Text(
                    text = shareText,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onShareCard) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(FlowSpacing.sm))
                    Text(stringResource(R.string.reader_share_card))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, shareLabel)
                onShare(shareIntent)
            }) {
                Text(stringResource(R.string.action_share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun SearchDialog(
    query: String,
    results: List<FtsSearchResult>,
    isSearching: Boolean,
    hasSearched: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onResultClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_search_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.reader_search_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() })
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSearch,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = query.isNotBlank() && !isSearching
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(stringResource(R.string.action_search))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (results.isNotEmpty()) {
                    Text(
                        stringResource(R.string.reader_search_results, results.size),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        // book_content is UNIQUE(book_id, chapter_index), so in-book search returns
                        // at most one row per chapter and chapterIndex is a unique key.
                        items(results.size, key = { results[it].chapterIndex }) { index ->
                            val result = results[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onResultClick(result.chapterIndex) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = result.chapterTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = result.matchedText,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else if (!isSearching && hasSearched && query.isNotEmpty()) {
                    Text(
                        stringResource(R.string.reader_search_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}
