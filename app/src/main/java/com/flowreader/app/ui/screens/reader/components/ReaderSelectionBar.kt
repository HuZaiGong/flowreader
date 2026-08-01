package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.flowreader.app.core.designsystem.reader.ReaderPalette
import com.flowreader.app.core.designsystem.reader.background
import com.flowreader.app.core.designsystem.reader.text
import kotlin.math.roundToInt

/**
 * Floating action bar shown above a native text selection: highlight, copy and bookmark.
 * Rendered through a [Popup] in window coordinates so it floats over the reader regardless
 * of scroll state.
 */
@Composable
fun ReaderSelectionBar(
    windowTopLeft: Offset,
    windowBottomRight: Offset,
    palette: ReaderPalette,
    onHighlight: () -> Unit,
    onCopy: () -> Unit,
    onBookmark: () -> Unit,
    onDismiss: () -> Unit
) {
    val barWidthDp = 196.dp.value
    val x = (windowTopLeft.x + windowBottomRight.x) / 2f - barWidthDp / 2f
    val y = windowBottomRight.y + 8.dp.value

    Popup(
        offset = IntOffset(x.roundToInt(), y.roundToInt()),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.background,
            shadowElevation = 6.dp,
            modifier = Modifier.border(1.dp, palette.text.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SelectionAction(
                    label = "高亮",
                    icon = { Icon(Icons.Default.FormatColorFill, contentDescription = "高亮选中文本", tint = it) },
                    tint = palette.text,
                    onClick = onHighlight
                )
                SelectionAction(
                    label = "复制",
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = "复制选中文本", tint = it) },
                    tint = palette.text,
                    onClick = onCopy
                )
                SelectionAction(
                    label = "书签",
                    icon = { Icon(Icons.Default.BookmarkAdd, contentDescription = "为选中文本添加书签", tint = it) },
                    tint = palette.text,
                    onClick = onBookmark
                )
            }
        }
    }
}

@Composable
private fun SelectionAction(
    label: String,
    icon: @Composable (Color) -> Unit,
    tint: Color,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = 2.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon(tint)
            Text(text = label, color = tint, style = MaterialTheme.typography.labelSmall)
        }
    }
}
