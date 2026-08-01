package com.flowreader.app.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.R
import com.flowreader.app.core.designsystem.component.BookCover
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.core.util.FlowFormatters
import com.flowreader.app.domain.model.GlobalSearchResult

/**
 * The independent search destination (v55): search history on idle, then two-section results —
 * books (title/author) and chapter hits from the FTS index — with paged loading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String,
    onBackClick: () -> Unit,
    onBookClick: (Long) -> Unit,
    onChapterClick: (Long, Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            viewModel.updateQuery(initialQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.material3.OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.updateQuery(it) },
                        placeholder = { Text(stringResource(R.string.library_search_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                uiState.query.trim().length < 2 -> {
                    HistorySection(
                        history = uiState.history,
                        onUseHistory = { viewModel.useHistory(it) },
                        onClearHistory = { viewModel.clearHistory() }
                    )
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(FlowSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(FlowSpacing.md)
                ) {
                    item(key = "book_header") {
                        Text(
                            text = stringResource(R.string.search_section_books, uiState.bookResults.size),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (uiState.bookResults.isEmpty()) {
                        item(key = "book_empty") {
                            Text(
                                text = stringResource(R.string.search_no_books),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(uiState.bookResults, key = { "book-${it.id}" }) { book ->
                        BookSearchRow(book = book, onClick = { onBookClick(book.id) })
                    }

                    item(key = "chapter_header") {
                        Text(
                            text = stringResource(R.string.search_section_chapters, uiState.chapterResults.size),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = FlowSpacing.md)
                        )
                    }

                    items(uiState.chapterResults, key = { "chapter-${it.bookId}-${it.chapterIndex}" }) { result ->
                        ChapterSearchRow(result = result, onClick = { onChapterClick(result.bookId, result.chapterIndex) })
                    }

                    if (uiState.hasMoreChapters) {
                        item(key = "load_more") {
                            OutlinedButton(
                                onClick = { viewModel.loadMoreChapters() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = FlowSpacing.sm)
                            ) {
                                Text(stringResource(R.string.search_load_more))
                            }
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = FlowSpacing.lg)
                )
            }
        }
    }
}

@Composable
private fun HistorySection(history: List<String>, onUseHistory: (String) -> Unit, onClearHistory: () -> Unit) {
    Column(modifier = Modifier.padding(FlowSpacing.lg)) {
        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = FlowSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(FlowSpacing.md))
                Text(
                    text = stringResource(R.string.search_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(FlowSpacing.sm))
                Text(
                    text = stringResource(R.string.search_history_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClearHistory) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(FlowSpacing.xs))
                    Text(stringResource(R.string.search_clear_history))
                }
            }
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            history.forEach { query ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUseHistory(query) }
                        .padding(vertical = FlowSpacing.sm)
                ) {
                    Text(
                        text = query,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BookSearchRow(book: com.flowreader.app.domain.model.Book, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(text = book.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = book.author + if (book.readingProgress > 0f) " · " + FlowFormatters.percent(book.readingProgress) else "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            BookCover(
                title = book.title,
                author = book.author,
                coverPath = book.coverPath,
                modifier = Modifier.size(44.dp, 62.dp),
                showTitleOnFallback = false
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun ChapterSearchRow(result: GlobalSearchResult, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = result.bookTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = result.chapterTitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.matchedText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingContent = {
            Icon(
                Icons.Default.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
