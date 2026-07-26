package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flowreader.app.core.designsystem.token.FlowRadius
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.domain.model.AnnotationColor

/**
 * Long-press actions for one paragraph.
 *
 * Replaces the v51 `HighlightMenu`, which asked the user to *type the text they wanted to
 * highlight* into a free-text field while separately storing the long-pressed paragraph's
 * character range — so the saved text and the saved range could disagree. The highlight now
 * always covers exactly the paragraph that was pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParagraphActionSheet(
    paragraph: String,
    onHighlight: (AnnotationColor) -> Unit,
    onBookmark: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = FlowSpacing.lg)
                .padding(bottom = FlowSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FlowSpacing.md)
        ) {
            Text("选中段落", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            SelectionContainer {
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text("高亮颜色", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.md)) {
                AnnotationColor.entries.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(FlowRadius.sm))
                            .background(Color(color.colorValue))
                            .clickable { onHighlight(color) }
                            .semantics { contentDescription = "使用${color.name}高亮该段落" }
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("书签备注（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FlowSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { onBookmark(note.ifBlank { paragraph.take(60) }) }) {
                    Icon(Icons.Default.Bookmark, contentDescription = null)
                    Text("添加书签", modifier = Modifier.padding(start = FlowSpacing.sm))
                }
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(paragraph))
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Text("复制", modifier = Modifier.padding(start = FlowSpacing.sm))
                }
            }
        }
    }
}
