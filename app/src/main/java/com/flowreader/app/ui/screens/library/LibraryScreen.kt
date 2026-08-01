package com.flowreader.app.ui.screens.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.R
import com.flowreader.app.core.designsystem.component.BookCover
import com.flowreader.app.core.designsystem.component.BookShelfSkeleton
import com.flowreader.app.core.designsystem.component.FlowSelectionTopBar
import com.flowreader.app.core.designsystem.component.FlowStateHost
import com.flowreader.app.core.designsystem.token.FlowRadius
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.core.util.FlowFormatters
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.GlobalSearchResult
import com.flowreader.app.domain.model.LibraryViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    onContinueReading: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onWheelClick: () -> Unit,
    onNotesClick: () -> Unit,
    onReadingListsClick: () -> Unit,
    onOpdsClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }

    val bookPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri -> viewModel.importBook(uri) }
    }

    var showSearchBar by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var batchAction by remember { mutableStateOf<BatchAction?>(null) }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    // v52: import failures used to be swallowed by a LaunchedEffect that called clearError()
    // without ever showing anything. The reason now reaches the user.
    val error = uiState.error
    val dismissLabel = stringResource(R.string.action_dismiss)
    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = dismissLabel,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    val message = uiState.message
    val messageText = message?.let { libraryMessageText(it) }
    LaunchedEffect(message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (!showSearchBar && !uiState.selectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { bookPickerLauncher.launch(IMPORT_MIME_TYPES) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.library_import)) }
                )
            }
        },
        topBar = {
            when {
                uiState.selectionMode -> FlowSelectionTopBar(
                    selectedCount = uiState.selectedBookIds.size,
                    onClose = { viewModel.clearSelection() },
                    actions = {
                        IconButton(onClick = { viewModel.selectAllVisible() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = stringResource(R.string.batch_select_all))
                        }
                        IconButton(onClick = { batchAction = BatchAction.MOVE }) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = stringResource(R.string.batch_move))
                        }
                        IconButton(onClick = { batchAction = BatchAction.EDIT }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.batch_edit))
                        }
                        IconButton(onClick = { batchAction = BatchAction.ADD_TO_LIST }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = stringResource(R.string.batch_add_to_list))
                        }
                        IconButton(onClick = { batchAction = BatchAction.DELETE }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.batch_delete))
                        }
                    }
                )

                showSearchBar -> SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = { showSearchBar = false },
                    active = true,
                    onActiveChange = { showSearchBar = it },
                    placeholder = { Text(stringResource(R.string.library_search_placeholder)) },
                    leadingIcon = {
                        IconButton(onClick = { showSearchBar = false }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.library_search_exit)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isGlobalSearching) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (uiState.globalSearchResults.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.library_fulltext_hits, uiState.globalSearchResults.size),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = FlowSpacing.lg, vertical = FlowSpacing.sm)
                        )
                        LazyColumn {
                            items(
                                uiState.globalSearchResults,
                                key = { "${it.bookId}-${it.chapterIndex}-${it.matchedText.hashCode()}" }
                            ) { result ->
                                GlobalSearchResultItem(
                                    result = result,
                                    onClick = {
                                        showSearchBar = false
                                        onBookClick(result.bookId)
                                    }
                                )
                            }
                        }
                    }
                }

                else -> TopAppBar(
                    title = { Text(stringResource(R.string.library_title)) },
                    actions = {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search))
                        }
                        IconButton(onClick = { viewModel.setViewMode(uiState.viewMode.toggle()) }) {
                            if (uiState.viewMode == LibraryViewMode.GRID) {
                                Icon(
                                    Icons.Default.ViewList,
                                    contentDescription = stringResource(R.string.library_view_list)
                                )
                            } else {
                                Icon(
                                    Icons.Default.GridView,
                                    contentDescription = stringResource(R.string.library_view_grid)
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more))
                            }
                            LibraryOverflowMenu(
                                expanded = showOverflowMenu,
                                sortOrder = uiState.sortOrder,
                                onDismiss = { showOverflowMenu = false },
                                onSortOrder = { viewModel.updateSortOrder(it) },
                                onImportArchive = { bookPickerLauncher.launch(ARCHIVE_MIME_TYPES) },
                                onNotes = onNotesClick,
                                onReadingLists = onReadingListsClick,
                                onOpds = onOpdsClick,
                                onWheel = onWheelClick,
                                onSettings = onSettingsClick
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        FlowStateHost(
            isLoading = uiState.isLoading && uiState.books.isEmpty(),
            isEmpty = uiState.books.isEmpty(),
            error = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            emptyTitle = stringResource(R.string.library_empty_title),
            emptyMessage = stringResource(R.string.library_empty_message),
            emptyIcon = Icons.AutoMirrored.Filled.MenuBook,
            emptyAction = {
                TextButton(onClick = { bookPickerLauncher.launch(IMPORT_MIME_TYPES) }) {
                    Text(stringResource(R.string.library_empty_action))
                }
            },
            // v53: the shelf shape is known before the data lands, so the cold start shows the
            // layout it is about to fill instead of a centred spinner on an empty screen.
            loadingContent = { BookShelfSkeleton() }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.viewMode == LibraryViewMode.GRID) {
                    LibraryGrid(
                        uiState = uiState,
                        gridState = gridState,
                        isRefreshing = isRefreshing,
                        pullToRefreshState = pullToRefreshState,
                        onRefresh = { viewModel.refreshBooks() },
                        onContinueReading = onContinueReading,
                        onBookClick = onBookClick,
                        onToggleSelection = { viewModel.toggleSelection(it) },
                        onDeleteBook = { viewModel.deleteBook(it) },
                        onSelectCategory = { viewModel.selectCategory(it) }
                    )
                }
                if (uiState.viewMode == LibraryViewMode.LIST) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pullToRefresh(
                                isRefreshing = isRefreshing,
                                state = pullToRefreshState,
                                onRefresh = { viewModel.refreshBooks() }
                            ),
                        contentPadding = PaddingValues(FlowSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(FlowSpacing.lg)
                    ) {
                    if (uiState.recentlyRead.isNotEmpty() && !uiState.selectionMode) {
                        item(key = "recent_label") {
                            Text(
                                text = stringResource(R.string.library_section_continue),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = FlowSpacing.sm)
                            )
                        }

                        item(key = "recent_list") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.md)) {
                                items(uiState.recentlyRead, key = { it.id }) { book ->
                                    RecentBookCard(
                                        book = book,
                                        onClick = { onContinueReading(book.id) }
                                    )
                                }
                            }
                        }

                        item(key = "recent_divider") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = FlowSpacing.sm))
                        }
                    }

                    item(key = "all_label") {
                        Text(
                            text = stringResource(R.string.library_section_all),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = FlowSpacing.sm)
                        )
                    }

                    if (uiState.categories.isNotEmpty()) {
                        item(key = "category_filters") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
                                item(key = "all_categories") {
                                    FilterChip(
                                        selected = uiState.selectedCategoryId == null,
                                        onClick = { viewModel.selectCategory(null) },
                                        label = { Text(stringResource(R.string.library_filter_all)) }
                                    )
                                }
                                items(uiState.categories, key = { it.id }) { category ->
                                    FilterChip(
                                        selected = uiState.selectedCategoryId == category.id,
                                        onClick = { viewModel.selectCategory(category.id) },
                                        label = { Text(category.name) }
                                    )
                                }
                            }
                        }
                    }

                        items(uiState.books, key = { it.id }) { book ->
                            BookListItem(
                                book = book,
                                selectionMode = uiState.selectionMode,
                                selected = book.id in uiState.selectedBookIds,
                                onClick = {
                                    if (uiState.selectionMode) viewModel.toggleSelection(book.id) else onBookClick(book.id)
                                },
                                onLongClick = { viewModel.toggleSelection(book.id) },
                                onDelete = { viewModel.deleteBook(book.id) }
                            )
                        }
                    }
                }

                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }

    BatchActionDialogs(
        action = batchAction,
        uiState = uiState,
        onDismiss = { batchAction = null },
        viewModel = viewModel
    )
}

private enum class BatchAction { DELETE, MOVE, EDIT, ADD_TO_LIST }

private val IMPORT_MIME_TYPES = arrayOf(
    "application/epub+zip",
    "text/plain",
    "text/markdown",
    "application/pdf",
    "image/jpeg",
    "image/png",
    "image/webp",
    "application/vnd.comicbook+zip",
    "application/x-fictionbook+xml",
    "application/x-mobipocket-ebook",
    "application/zip",
    "application/octet-stream"
)

private val ARCHIVE_MIME_TYPES = arrayOf("application/zip", "application/vnd.comicbook+zip")

@Composable
private fun libraryMessageText(message: LibraryMessage): String = when (message) {
    is LibraryMessage.Deleted -> stringResource(R.string.batch_result_deleted, message.count)
    is LibraryMessage.Moved -> stringResource(R.string.batch_result_moved, message.count)
    is LibraryMessage.Updated -> stringResource(R.string.batch_result_updated, message.count)
    is LibraryMessage.AddedToList -> stringResource(R.string.batch_result_added_to_list, message.listName)
    is LibraryMessage.ArchiveImported -> stringResource(R.string.library_import_archive_result, message.count)
}

@Composable
private fun sortOrderName(order: SortOrder): String = when (order) {
    SortOrder.ADDED_TIME -> stringResource(R.string.library_sort_added)
    SortOrder.LAST_READ -> stringResource(R.string.library_sort_last_read)
    SortOrder.TITLE -> stringResource(R.string.library_sort_title)
    SortOrder.AUTHOR -> stringResource(R.string.library_sort_author)
}

@Composable
private fun LibraryOverflowMenu(
    expanded: Boolean,
    sortOrder: SortOrder,
    onDismiss: () -> Unit,
    onSortOrder: (SortOrder) -> Unit,
    onImportArchive: () -> Unit,
    onNotes: () -> Unit,
    onReadingLists: () -> Unit,
    onOpds: () -> Unit,
    onWheel: () -> Unit,
    onSettings: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortOrder.entries.forEach { order ->
            DropdownMenuItem(
                text = { Text(sortOrderName(order)) },
                onClick = {
                    onSortOrder(order)
                    onDismiss()
                },
                leadingIcon = {
                    if (sortOrder == order) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                    }
                }
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_import_archive)) },
            leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null) },
            onClick = {
                onDismiss()
                onImportArchive()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_opds)) },
            leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) },
            onClick = {
                onDismiss()
                onOpds()
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_reading_lists)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null) },
            onClick = {
                onDismiss()
                onReadingLists()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_notes)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
            onClick = {
                onDismiss()
                onNotes()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_wheel)) },
            leadingIcon = { Icon(Icons.Default.Casino, contentDescription = null) },
            onClick = {
                onDismiss()
                onWheel()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_settings)) },
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
            onClick = {
                onDismiss()
                onSettings()
            }
        )
    }
}

@Composable
private fun BatchActionDialogs(
    action: BatchAction?,
    uiState: LibraryUiState,
    onDismiss: () -> Unit,
    viewModel: LibraryViewModel
) {
    when (action) {
        null -> Unit

        BatchAction.DELETE -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.batch_delete_title, uiState.selectedBookIds.size)) },
            text = { Text(stringResource(R.string.batch_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelected()
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        )

        BatchAction.MOVE -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.batch_move_title)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.batch_move_none)) },
                        modifier = Modifier.clickable {
                            viewModel.moveSelectedToCategory(null)
                            onDismiss()
                        }
                    )
                    uiState.categories.forEach { category ->
                        ListItem(
                            headlineContent = { Text(category.name) },
                            modifier = Modifier.clickable {
                                viewModel.moveSelectedToCategory(category.id)
                                onDismiss()
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        )

        BatchAction.EDIT -> BatchMetadataDialog(
            onDismiss = onDismiss,
            onConfirm = { author, tags ->
                viewModel.updateSelectedMetadata(author, tags)
                onDismiss()
            }
        )

        BatchAction.ADD_TO_LIST -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.batch_add_to_list)) },
            text = {
                if (uiState.readingLists.isEmpty()) {
                    Text(stringResource(R.string.reading_lists_no_list))
                } else {
                    Column {
                        uiState.readingLists.forEach { list ->
                            ListItem(
                                headlineContent = { Text(list.name) },
                                supportingContent = {
                                    Text(stringResource(R.string.reading_lists_book_count, list.bookCount))
                                },
                                modifier = Modifier.clickable {
                                    viewModel.addSelectedToList(list)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun BatchMetadataDialog(onDismiss: () -> Unit, onConfirm: (String?, String?) -> Unit) {
    var author by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.batch_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FlowSpacing.md)) {
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(R.string.batch_edit_author)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(stringResource(R.string.batch_edit_tags)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(author.ifBlank { null }, tags.ifBlank { null }) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun GlobalSearchResultItem(result: GlobalSearchResult, onClick: () -> Unit) {
    val unknown = stringResource(R.string.library_unknown_book)
    ListItem(
        headlineContent = {
            Text(result.bookTitle.ifBlank { unknown }, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = stringResource(
                    R.string.library_search_context,
                    result.chapterIndex + 1,
                    result.chapterTitle
                ) + "\n" + result.matchedText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun RecentBookCard(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(FlowRadius.sm)
    ) {
        Column {
            BookCover(
                title = book.title,
                author = book.author,
                coverPath = book.coverPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(topStart = FlowRadius.sm, topEnd = FlowRadius.sm)
            )
            Column(modifier = Modifier.padding(FlowSpacing.sm)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.readingProgress > 0) {
                    Spacer(modifier = Modifier.height(FlowSpacing.xs))
                    LinearProgressIndicator(
                        progress = { book.readingProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookListItem(
    book: Book,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(FlowRadius.md),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FlowSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
                Spacer(modifier = Modifier.width(FlowSpacing.sm))
            }

            BookCover(
                title = book.title,
                author = book.author,
                coverPath = book.coverPath,
                modifier = Modifier.size(70.dp, 100.dp),
                showTitleOnFallback = false
            )

            Spacer(modifier = Modifier.width(FlowSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                Text(
                    text = stringResource(R.string.library_chapter_count, book.totalChapters),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (book.readingProgress > 0) {
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { book.readingProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                        )
                        Spacer(modifier = Modifier.width(FlowSpacing.sm))
                        Text(
                            text = FlowFormatters.percent(book.readingProgress),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!selectionMode) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.library_book_actions, book.title)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.library_delete_title)) },
            text = { Text(stringResource(R.string.library_delete_message, book.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryGrid(
    uiState: com.flowreader.app.ui.screens.library.LibraryUiState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    isRefreshing: Boolean,
    pullToRefreshState: androidx.compose.material3.pulltorefresh.PullToRefreshState,
    onRefresh: () -> Unit,
    onContinueReading: (Long) -> Unit,
    onBookClick: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onDeleteBook: (Long) -> Unit,
    onSelectCategory: (Long?) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                isRefreshing = isRefreshing,
                state = pullToRefreshState,
                onRefresh = onRefresh
            ),
        contentPadding = PaddingValues(FlowSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(FlowSpacing.md),
        verticalArrangement = Arrangement.spacedBy(FlowSpacing.md)
    ) {
        if (!uiState.selectionMode) {
            uiState.recentlyRead.firstOrNull()?.let { recent ->
                item(key = "continue_big", span = { GridItemSpan(maxLineSpan) }) {
                    ContinueReadingBigCard(
                        book = recent,
                        onClick = { onContinueReading(recent.id) }
                    )
                }
            }
        }

        if (uiState.categories.isNotEmpty()) {
            item(key = "grid_category_filters", span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
                    item(key = "all_categories_grid") {
                        FilterChip(
                            selected = uiState.selectedCategoryId == null,
                            onClick = { onSelectCategory(null) },
                            label = { Text(stringResource(R.string.library_filter_all)) }
                        )
                    }
                    items(uiState.categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = uiState.selectedCategoryId == category.id,
                            onClick = { onSelectCategory(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }
        }

        if (uiState.books.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.library_empty_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = FlowSpacing.xl),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        gridItems(uiState.books, key = { it.id }) { book ->
            BookGridCard(
                book = book,
                selectionMode = uiState.selectionMode,
                selected = book.id in uiState.selectedBookIds,
                onClick = {
                    if (uiState.selectionMode) onToggleSelection(book.id) else onBookClick(book.id)
                },
                onLongClick = { onToggleSelection(book.id) },
                onDelete = { onDeleteBook(book.id) }
            )
        }
    }
}

@Composable
private fun ContinueReadingBigCard(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(FlowRadius.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FlowSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(
                title = book.title,
                author = book.author,
                coverPath = book.coverPath,
                modifier = Modifier.size(84.dp, 120.dp)
            )
            Spacer(modifier = Modifier.width(FlowSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.library_section_continue),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                if (book.readingProgress > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { book.readingProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                        )
                        Spacer(modifier = Modifier.width(FlowSpacing.sm))
                        Text(
                            text = FlowFormatters.percent(book.readingProgress),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.library_continue_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookGridCard(
    book: Book,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(FlowRadius.sm),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Box {
            BookCover(
                title = book.title,
                author = book.author,
                coverPath = book.coverPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                shape = RoundedCornerShape(topStart = FlowRadius.sm, topEnd = FlowRadius.sm)
            )
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(FlowSpacing.xs)
                )
            }
        }
        Column(modifier = Modifier.padding(FlowSpacing.sm)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(FlowSpacing.xs))
            if (book.readingProgress > 0) {
                LinearProgressIndicator(
                    progress = { book.readingProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                Text(
                    text = FlowFormatters.percent(book.readingProgress),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (!selectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.library_book_actions, book.title)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.library_delete_title)) },
            text = { Text(stringResource(R.string.library_delete_message, book.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private fun LibraryViewMode.toggle(): LibraryViewMode =
    if (this == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
