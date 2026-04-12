package com.flowreader.app.ui.screens.bookdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flowreader.app.domain.model.Bookmark
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.ui.theme.FlowReaderTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: Long,
    onBackClick: () -> Unit,
    onReadClick: (Long) -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    FlowReaderTheme(theme = uiState.appTheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(uiState.book?.title ?: "书籍详情") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                uiState.book?.let { book ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            BookInfoHeader(
                                book = book,
                                onReadClick = { onReadClick(bookId) }
                            )
                        }

                        item {
                            TabRow(selectedTabIndex = selectedTab) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("目录 (${uiState.chapters.size})") }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = { Text("书签 (${uiState.bookmarks.size})") }
                                )
                            }
                        }

                        when (selectedTab) {
                            0 -> {
                                if (uiState.chapters.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "暂无目录",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                } else {
                                    items(uiState.chapters) { chapter ->
                                        ChapterItem(chapter = chapter)
                                    }
                                }
                            }
                            1 -> {
                                if (uiState.bookmarks.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "暂无书签",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                } else {
                                    items(uiState.bookmarks) { bookmark ->
                                        BookmarkItem(
                                            bookmark = bookmark,
                                            onDelete = { viewModel.deleteBookmark(bookmark.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookInfoHeader(
    book: com.flowreader.app.domain.model.Book,
    onReadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp, 150.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (book.coverPath != null && File(book.coverPath).exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(book.coverPath))
                                .crossfade(true)
                                .build(),
                            contentDescription = book.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${book.totalChapters} 章",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    if (book.readingProgress > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(
                                progress = { book.readingProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${(book.readingProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (book.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "简介",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onReadClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (book.readingProgress > 0) Icons.Default.PlayArrow else Icons.Default.MenuBook,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (book.readingProgress > 0) "继续阅读" else "开始阅读")
            }
        }
    }
}

@Composable
private fun ChapterItem(chapter: Chapter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${chapter.index + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BookmarkItem(
    bookmark: Bookmark,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "第 ${bookmark.chapterIndex + 1} 章 · ${dateFormat.format(bookmark.createdTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
