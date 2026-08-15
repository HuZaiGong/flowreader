package com.flowreader.app.ui.screens.bookdetail

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.flowreader.app.R
import com.flowreader.app.core.designsystem.component.FlowStateHost
import com.flowreader.app.core.designsystem.token.FlowRadius
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.core.util.FlowFormatters
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.Bookmark
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.repository.AnnotationExportFormat
import com.flowreader.app.ui.components.durationText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Book detail.
 *
 * v52 changes: chapters are real `LazyColumn` items instead of a `Column { forEach }` crammed into
 * a single item (a 2000-chapter TXT used to compose all 2000 rows on first frame), and the
 * bookmarks tab finally renders `BookmarkListContent`, which existed but was never called.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookDetailScreen(
    bookId: Long,
    onBackClick: () -> Unit,
    onReadClick: (Long, Int) -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showTagsDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    // Export leaves through the system share sheet instead of a truncated AlertDialog preview.
    val exportText = uiState.annotationExportText
    LaunchedEffect(exportText) {
        if (exportText != null) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TITLE,
                    uiState.book?.title ?: context.getString(R.string.book_detail_export_title)
                )
                putExtra(Intent.EXTRA_TEXT, exportText)
            }
            context.startActivity(
                Intent.createChooser(sendIntent, context.getString(R.string.book_detail_export_annotations))
            )
            viewModel.clearAnnotationExport()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.book?.title ?: stringResource(R.string.book_detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        FlowStateHost(
            isLoading = uiState.isLoading,
            isEmpty = uiState.book == null,
            error = uiState.error,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            emptyTitle = stringResource(R.string.book_detail_not_found),
            onDismissError = onBackClick
        ) {
            val book = uiState.book ?: return@FlowStateHost
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(FlowSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(FlowSpacing.lg)
            ) {
                item(key = "header") {
                    BookInfoHeader(book = book, onReadClick = { onReadClick(bookId, -1) })
                }

                item(key = "stats") {
                    ReadingStatsCard(
                        totalReadTime = uiState.totalReadTime,
                        totalReadPages = uiState.totalReadPages
                    )
                }

                item(key = "tags") {
                    TagEditorCard(tags = book.tags, onEdit = { showTagsDialog = true })
                }

                stickyHeader(key = "tabs") {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Text(stringResource(R.string.book_detail_tab_chapters, uiState.chapters.size))
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Text(stringResource(R.string.book_detail_tab_annotations, uiState.annotations.size))
                                }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = {
                                    Text(stringResource(R.string.book_detail_tab_bookmarks, uiState.bookmarks.size))
                                }
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> if (uiState.chapters.isEmpty()) {
                        item(key = "chapters_empty") {
                            TabEmpty(stringResource(R.string.book_detail_chapters_empty))
                        }
                    } else {
                        items(uiState.chapters, key = { "chapter_${it.id}_${it.index}" }) { chapter ->
                            ChapterItem(chapter = chapter, onClick = { onReadClick(bookId, chapter.index) })
                        }
                    }

                    1 -> {
                        item(key = "annotation_export") {
                            Box {
                                OutlinedButton(onClick = { showExportMenu = true }) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(FlowSpacing.sm))
                                    Text(stringResource(R.string.book_detail_export_annotations))
                                }
                                DropdownMenu(
                                    expanded = showExportMenu,
                                    onDismissRequest = { showExportMenu = false }
                                ) {
                                    AnnotationExportFormat.entries.forEach { format ->
                                        DropdownMenuItem(
                                            text = { Text(format.name.lowercase()) },
                                            onClick = {
                                                viewModel.exportAnnotations(format)
                                                showExportMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (uiState.annotations.isEmpty()) {
                            item(key = "annotations_empty") {
                                TabEmpty(stringResource(R.string.book_detail_annotations_empty))
                            }
                        } else {
                            items(uiState.annotations, key = { "annotation_${it.id}" }) { annotation ->
                                AnnotationItem(
                                    annotation = annotation,
                                    onDelete = { viewModel.deleteAnnotation(annotation.id) }
                                )
                            }
                        }
                    }

                    else -> if (uiState.bookmarks.isEmpty()) {
                        item(key = "bookmarks_empty") {
                            TabEmpty(stringResource(R.string.book_detail_bookmarks_empty))
                        }
                    } else {
                        items(uiState.bookmarks, key = { "bookmark_${it.id}" }) { bookmark ->
                            BookmarkItem(
                                bookmark = bookmark,
                                onOpen = { onReadClick(bookId, bookmark.chapterIndex) },
                                onDelete = { viewModel.deleteBookmark(bookmark.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTagsDialog) {
        TagsDialog(
            tags = uiState.book?.tags.orEmpty(),
            onSave = { viewModel.updateTags(it) },
            onDismiss = { showTagsDialog = false }
        )
    }
}

@Composable
private fun TabEmpty(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FlowSpacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TagEditorCard(tags: List<String>, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(FlowSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FlowSpacing.sm)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.book_detail_tags_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
            }
            if (tags.isEmpty()) {
                Text(stringResource(R.string.book_detail_tags_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.sm)) {
                    tags.forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                }
            }
        }
    }
}

@Composable
private fun TagsDialog(tags: List<String>, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember(tags) { mutableStateOf(tags.joinToString(", ")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.book_detail_tags_edit_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.book_detail_tags_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(text)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun ReadingStatsCard(totalReadTime: Long, totalReadPages: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FlowRadius.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FlowSpacing.lg),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                icon = { Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                value = durationText(totalReadTime),
                label = stringResource(R.string.stats_read_time)
            )
            StatColumn(
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                value = "$totalReadPages",
                label = stringResource(R.string.stats_read_pages)
            )
        }
    }
}

@Composable
private fun StatColumn(icon: @Composable () -> Unit, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        Spacer(modifier = Modifier.height(FlowSpacing.xs))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BookInfoHeader(book: Book, onReadClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(FlowRadius.lg)) {
        Column(modifier = Modifier.padding(FlowSpacing.lg)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Box(
                    modifier = Modifier
                        .size(100.dp, 150.dp)
                        .clip(RoundedCornerShape(FlowRadius.sm))
                ) {
                    val coverPath = book.coverPath
                    if (coverPath.isNullOrBlank()) {
                        CoverFallback()
                    } else {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(coverPath))
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.book_detail_cover_of, book.title),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = { CoverFallback() },
                            error = { CoverFallback() }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(FlowSpacing.lg))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    Text(
                        text = stringResource(R.string.book_detail_chapter_count, book.totalChapters),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (book.readingProgress > 0) {
                        Spacer(modifier = Modifier.height(FlowSpacing.sm))
                        Text(
                            text = stringResource(
                                R.string.book_detail_read_percent,
                                FlowFormatters.percent(book.readingProgress)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (book.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(FlowSpacing.lg))
                Text(
                    text = stringResource(R.string.book_detail_description),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                Text(
                    text = book.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(FlowSpacing.lg))

            Button(onClick = onReadClick, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = if (book.readingProgress > 0) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(FlowSpacing.sm))
                Text(
                    stringResource(
                        if (book.readingProgress > 0) {
                            R.string.book_detail_continue_reading
                        } else {
                            R.string.book_detail_start_reading
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun CoverFallback() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AnnotationItem(annotation: Annotation, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(FlowRadius.sm)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FlowSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(annotation.color.colorValue))
            )
            Spacer(modifier = Modifier.width(FlowSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = annotation.selectedText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(FlowSpacing.xs))
                Text(
                    text = stringResource(
                        R.string.book_detail_entry_meta,
                        annotation.chapterIndex + 1,
                        dateFormat.format(annotation.createdTime)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (annotation.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.book_detail_annotation_note, annotation.note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.book_detail_delete_annotation),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ChapterItem(chapter: Chapter, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(FlowRadius.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FlowSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${chapter.index + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(32.dp)
            )
            Spacer(modifier = Modifier.width(FlowSpacing.md))
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
private fun BookmarkItem(bookmark: Bookmark, onOpen: () -> Unit, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    var isDeleting by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !isDeleting,
        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(),
        label = "BookmarkDeleteAnimation"
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
            shape = RoundedCornerShape(FlowRadius.sm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FlowSpacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(FlowSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bookmark.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(FlowSpacing.xs))
                    Text(
                        text = stringResource(
                            R.string.book_detail_entry_meta,
                            bookmark.chapterIndex + 1,
                            dateFormat.format(bookmark.createdTime)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { isDeleting = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.book_detail_delete_bookmark),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (isDeleting) {
        LaunchedEffect(isDeleting) { onDelete() }
    }
}
