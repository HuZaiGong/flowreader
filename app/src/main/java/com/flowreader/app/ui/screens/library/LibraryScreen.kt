package com.flowreader.app.ui.screens.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.flowreader.app.core.designsystem.component.FlowStateHost
import com.flowreader.app.core.designsystem.token.FlowRadius
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.core.util.FlowFormatters
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.GlobalSearchResult
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    onContinueReading: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onWheelClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val multipleBookPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri -> viewModel.importBook(uri) }
    }

    var showSearchBar by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    // v52: import failures used to be swallowed by a LaunchedEffect that called clearError()
    // without ever showing anything. The reason now reaches the user.
    val error = uiState.error
    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "知道了",
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (!showSearchBar) {
                ExtendedFloatingActionButton(
                    onClick = { multipleBookPickerLauncher.launch(IMPORT_MIME_TYPES) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("导入书籍") }
                )
            }
        },
        topBar = {
            if (showSearchBar) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = { showSearchBar = false },
                    active = true,
                    onActiveChange = { showSearchBar = it },
                    placeholder = { Text("搜索书名、作者或全文") },
                    leadingIcon = {
                        IconButton(onClick = { showSearchBar = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "退出搜索")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isGlobalSearching) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (uiState.globalSearchResults.isNotEmpty()) {
                        Text(
                            text = "全文命中 (${uiState.globalSearchResults.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = FlowSpacing.lg, vertical = FlowSpacing.sm)
                        )
                        LazyColumn {
                            items(uiState.globalSearchResults, key = { "${it.bookId}-${it.chapterIndex}-${it.matchedText.hashCode()}" }) { result ->
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
            } else {
                TopAppBar(
                    title = { Text("心流阅读") },
                    actions = {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(sortOrderName(order)) },
                                        onClick = {
                                            viewModel.updateSortOrder(order)
                                            showOverflowMenu = false
                                        },
                                        leadingIcon = {
                                            if (uiState.sortOrder == order) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            } else {
                                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("决策转盘") },
                                    leadingIcon = { Icon(Icons.Default.Casino, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onWheelClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("设置") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onSettingsClick()
                                    }
                                )
                            }
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
            emptyTitle = "书架空空如也",
            emptyMessage = "用下方的「导入书籍」添加 EPUB / TXT / PDF / Markdown",
            emptyIcon = Icons.AutoMirrored.Filled.MenuBook,
            emptyAction = {
                TextButton(onClick = { multipleBookPickerLauncher.launch(IMPORT_MIME_TYPES) }) {
                    Text("现在导入")
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                    if (uiState.recentlyRead.isNotEmpty()) {
                        item(key = "recent_label") {
                            Text(
                                text = "继续阅读",
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
                            text = "全部书籍",
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
                                        label = { Text("全部") }
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
                            onClick = { onBookClick(book.id) },
                            onDelete = { viewModel.deleteBook(book.id) }
                        )
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
}

private val IMPORT_MIME_TYPES = arrayOf(
    "application/epub+zip",
    "text/plain",
    "text/markdown",
    "application/pdf"
)

private fun sortOrderName(order: SortOrder): String = when (order) {
    SortOrder.ADDED_TIME -> "按添加时间"
    SortOrder.LAST_READ -> "按阅读时间"
    SortOrder.TITLE -> "按书名"
    SortOrder.AUTHOR -> "按作者"
}

@Composable
private fun GlobalSearchResultItem(result: GlobalSearchResult, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(result.bookTitle.ifBlank { "未知书籍" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = "第 ${result.chapterIndex + 1} 章 · ${result.chapterTitle}\n${result.matchedText}",
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * Cover art. Coil resolves the file off the main thread and falls back to the placeholder, so the
 * composition no longer performs a `File(...).exists()` disk hit while laying out the shelf.
 */
@Composable
private fun BookCover(book: Book, modifier: Modifier = Modifier, iconSize: Int = 32) {
    val coverPath = book.coverPath
    if (coverPath.isNullOrBlank()) {
        CoverPlaceholder(modifier = modifier, iconSize = iconSize)
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(File(coverPath))
            .crossfade(true)
            .memoryCacheKey(coverPath)
            .diskCacheKey(coverPath)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = "《${book.title}》封面",
        contentScale = ContentScale.Crop,
        loading = { CoverPlaceholder(iconSize = iconSize) },
        error = { CoverPlaceholder(iconSize = iconSize) },
        modifier = modifier
    )
}

@Composable
private fun CoverPlaceholder(modifier: Modifier = Modifier, iconSize: Int = 32) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = null,
            modifier = Modifier.size(iconSize.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = FlowRadius.sm, topEnd = FlowRadius.sm))
            ) {
                BookCover(book = book, modifier = Modifier.fillMaxSize(), iconSize = 40)
            }
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

@Composable
private fun BookListItem(book: Book, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

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
            Box(
                modifier = Modifier
                    .size(70.dp, 100.dp)
                    .clip(RoundedCornerShape(FlowRadius.sm))
            ) {
                BookCover(book = book, modifier = Modifier.fillMaxSize())
            }

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
                    text = "${book.totalChapters} 章",
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
                                .clip(RoundedCornerShape(2.dp))
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

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "删除《${book.title}》")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除书籍") },
            text = { Text("确定要删除《${book.title}》吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
