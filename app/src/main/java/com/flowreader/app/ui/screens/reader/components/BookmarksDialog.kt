package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flowreader.app.R
import com.flowreader.app.domain.model.Bookmark

@Composable
fun BookmarksDialog(
    bookmarks: List<Bookmark>,
    currentChapterIndex: Int,
    onBookmarkSelect: (Bookmark) -> Unit,
    onBookmarkDelete: (Bookmark) -> Unit,
    onDismiss: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    backgroundColor: androidx.compose.ui.graphics.Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_bookmarks_title)) },
        text = {
            if (bookmarks.isEmpty()) {
                Text(stringResource(R.string.reader_bookmarks_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        ListItem(
                            headlineContent = { Text(bookmark.text) },
                            supportingContent = {
                                val currentMarker = if (bookmark.chapterIndex == currentChapterIndex) {
                                    stringResource(R.string.reader_bookmark_current)
                                } else {
                                    ""
                                }
                                Text(
                                    stringResource(
                                        R.string.reader_bookmark_entry,
                                        bookmark.chapterIndex + 1,
                                        bookmark.position
                                    ) + currentMarker
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { onBookmarkDelete(bookmark) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.action_delete)
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onBookmarkSelect(bookmark) }
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
}
