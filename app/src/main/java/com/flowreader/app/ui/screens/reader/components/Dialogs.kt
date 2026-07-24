package com.flowreader.app.ui.screens.reader.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
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
        title = { Text("高亮与笔记") },
        text = {
            if (annotations.isEmpty()) {
                Text("暂无高亮或笔记\n\n选中文字后点击高亮按钮添加")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(annotations.size) { index ->
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
                                        text = "笔记: ${annotation.note}",
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
                                        Icon(Icons.Default.Edit, contentDescription = "编辑笔记")
                                    }
                                    IconButton(onClick = { onAnnotationDelete(annotation) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除")
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
                Text("关闭")
            }
        }
    )

    if (editingNoteId != null) {
        AlertDialog(
            onDismissRequest = { editingNoteId = null },
            title = { Text("编辑笔记") },
            text = {
                OutlinedTextField(
                    value = editingNoteText,
                    onValueChange = { editingNoteText = it },
                    label = { Text("笔记内容") },
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
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNoteId = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ShareProgressDialog(
    shareText: String,
    onDismiss: () -> Unit,
    onShare: (Intent) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享阅读进度") },
        text = {
            Column {
                Text(
                    text = shareText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "分享到")
                onShare(shareIntent)
            }) {
                Text("分享")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun SearchDialog(
    query: String,
    results: List<FtsSearchResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onResultClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("全文搜索") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入搜索关键词") },
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
                        Text("搜索")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (results.isNotEmpty()) {
                    Text(
                        "搜索结果 (${results.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(results.size) { index ->
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
                } else if (!isSearching && query.isNotEmpty()) {
                    Text(
                        "未找到匹配结果",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
