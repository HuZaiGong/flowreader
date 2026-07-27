package com.flowreader.app.ui.screens.readinglist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.R
import com.flowreader.app.core.designsystem.component.BookCover
import com.flowreader.app.core.designsystem.component.FlowScaffold
import com.flowreader.app.core.designsystem.component.FlowStateHost
import com.flowreader.app.core.designsystem.component.FlowTopBar
import com.flowreader.app.core.designsystem.token.FlowElevation
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.ReadingList
import com.flowreader.app.domain.model.ReadingListBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListsScreen(
    onBackClick: () -> Unit,
    onOpenBook: (Long) -> Unit,
    viewModel: ReadingListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ReadingList?>(null) }
    var deleteTarget by remember { mutableStateOf<ReadingList?>(null) }
    var showAddBooks by remember { mutableStateOf(false) }

    val openList = uiState.openList

    FlowScaffold(
        topBar = {
            FlowTopBar(
                title = openList?.name ?: stringResource(R.string.reading_lists_title),
                subtitle = openList?.description?.takeIf { it.isNotBlank() },
                onNavigateUp = { if (openList != null) viewModel.closeList() else onBackClick() },
                actions = {
                    if (openList != null) {
                        IconButton(onClick = { renameTarget = openList }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.reading_lists_rename))
                        }
                        IconButton(onClick = { deleteTarget = openList }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.reading_lists_delete))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (openList != null) showAddBooks = true else showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        stringResource(
                            if (openList != null) R.string.reading_lists_add_books else R.string.reading_lists_create
                        )
                    )
                }
            )
        }
    ) {
        if (openList == null) {
            FlowStateHost(
                isLoading = uiState.isLoading,
                isEmpty = uiState.lists.isEmpty(),
                error = null,
                modifier = Modifier.fillMaxSize(),
                emptyTitle = stringResource(R.string.reading_lists_empty_title),
                emptyMessage = stringResource(R.string.reading_lists_empty_message),
                emptyIcon = Icons.AutoMirrored.Filled.LibraryBooks,
                emptyAction = {
                    TextButton(onClick = { showCreateDialog = true }) {
                        Text(stringResource(R.string.reading_lists_create))
                    }
                }
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(FlowSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(FlowSpacing.md)
                ) {
                    itemsIndexed(uiState.lists, key = { _, list -> list.id }) { _, list ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { viewModel.openList(list) }) {
                            ListItem(
                                headlineContent = { Text(list.name) },
                                supportingContent = {
                                    Text(stringResource(R.string.reading_lists_book_count, list.bookCount))
                                },
                                leadingContent = {
                                    Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null)
                                },
                                trailingContent = {
                                    IconButton(onClick = { deleteTarget = list }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.reading_lists_delete)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else {
            FlowStateHost(
                isLoading = false,
                isEmpty = uiState.openListBooks.isEmpty(),
                error = null,
                modifier = Modifier.fillMaxSize(),
                emptyTitle = stringResource(R.string.reading_lists_detail_empty),
                emptyMessage = stringResource(R.string.reading_lists_drag_handle),
                emptyIcon = Icons.AutoMirrored.Filled.LibraryBooks,
                emptyAction = {
                    TextButton(onClick = { showAddBooks = true }) {
                        Text(stringResource(R.string.reading_lists_add_books))
                    }
                }
            ) {
                ReorderableBookList(
                    books = uiState.openListBooks,
                    onMove = viewModel::moveBook,
                    onMoveCommitted = viewModel::commitOrder,
                    onRemove = viewModel::removeBook,
                    onOpen = onOpenBook
                )
            }
        }
    }

    if (showCreateDialog) {
        ReadingListEditDialog(
            initialName = "",
            initialDescription = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description ->
                viewModel.createList(name, description)
                showCreateDialog = false
            }
        )
    }

    renameTarget?.let { target ->
        ReadingListEditDialog(
            initialName = target.name,
            initialDescription = target.description,
            onDismiss = { renameTarget = null },
            onConfirm = { name, description ->
                viewModel.renameList(target.id, name, description)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.reading_lists_delete)) },
            text = { Text(stringResource(R.string.reading_lists_delete_message, target.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteList(target.id)
                        deleteTarget = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showAddBooks && openList != null) {
        val alreadyIn = uiState.openListBooks.map { it.book.id }.toSet()
        AddBooksDialog(
            candidates = uiState.shelf.filterNot { it.id in alreadyIn },
            onDismiss = { showAddBooks = false },
            onConfirm = { ids ->
                viewModel.addBooks(ids)
                showAddBooks = false
            }
        )
    }
}

/**
 * Long-press to lift, drag to reorder.
 *
 * The reorder is applied as soon as the dragged card's centre crosses a neighbour, and written to
 * Room once on release. Up/down buttons stay on every row because a drag gesture is unreachable
 * with TalkBack — reordering must not be mouse-only in the accessibility sense.
 */
@Composable
private fun ReorderableBookList(
    books: List<ReadingListBook>,
    onMove: (Int, Int) -> Unit,
    onMoveCommitted: () -> Unit,
    onRemove: (Long) -> Unit,
    onOpen: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(FlowSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FlowSpacing.md),
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(books.size) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        draggingIndex = listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
                            ?.index
                        dragOffset = 0f
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        val current = draggingIndex ?: return@detectDragGesturesAfterLongPress
                        dragOffset += amount.y
                        val currentInfo = listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == current } ?: return@detectDragGesturesAfterLongPress
                        val centre = currentInfo.offset + dragOffset + currentInfo.size / 2f
                        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                            item.index != current && centre.toInt() in item.offset..(item.offset + item.size)
                        }
                        if (target != null) {
                            onMove(current, target.index)
                            dragOffset -= (target.offset - currentInfo.offset)
                            draggingIndex = target.index
                        }
                    },
                    onDragEnd = {
                        draggingIndex = null
                        dragOffset = 0f
                        onMoveCommitted()
                    },
                    onDragCancel = {
                        draggingIndex = null
                        dragOffset = 0f
                    }
                )
            }
    ) {
        itemsIndexed(books, key = { _, item -> item.entryId }) { index, item ->
            val dragging = draggingIndex == index
            Card(
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (dragging) FlowElevation.overlay else FlowElevation.none
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
            ) {
                Row(
                    modifier = Modifier.padding(FlowSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = stringResource(R.string.reading_lists_drag_handle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(FlowSpacing.sm))
                    BookCover(
                        title = item.book.title,
                        author = item.book.author,
                        coverPath = item.book.coverPath,
                        modifier = Modifier.size(44.dp, 62.dp),
                        showTitleOnFallback = false
                    )
                    Spacer(modifier = Modifier.width(FlowSpacing.md))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpen(item.book.id) }
                    ) {
                        Text(
                            text = item.book.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.book.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            onMove(index, index - 1)
                            onMoveCommitted()
                        },
                        enabled = index > 0
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.reading_lists_move_up)
                        )
                    }
                    IconButton(
                        onClick = {
                            onMove(index, index + 1)
                            onMoveCommitted()
                        },
                        enabled = index < books.lastIndex
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.reading_lists_move_down)
                        )
                    }
                    IconButton(onClick = { onRemove(item.book.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.reading_lists_remove)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingListEditDialog(
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reading_lists_create)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FlowSpacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.reading_lists_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.reading_lists_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun AddBooksDialog(
    candidates: List<Book>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit
) {
    val selected = remember { mutableStateOf(setOf<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reading_lists_add_books)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                itemsIndexed(candidates, key = { _, book -> book.id }) { _, book ->
                    val checked = book.id in selected.value
                    ListItem(
                        headlineContent = { Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(book.author, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selected.value = if (checked) selected.value - book.id else selected.value + book.id
                                }
                            )
                        },
                        modifier = Modifier.clickable {
                            selected.value = if (checked) selected.value - book.id else selected.value + book.id
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected.value.toList()) },
                enabled = selected.value.isNotEmpty()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
