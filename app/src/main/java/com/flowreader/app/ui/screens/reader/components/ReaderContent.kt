package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowreader.app.domain.model.AnnotationColor
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.ReadingSettings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check

@Composable
fun ReaderContent(
    chapter: Chapter,
    settings: ReadingSettings,
    textColor: androidx.compose.ui.graphics.Color,
    backgroundColor: androidx.compose.ui.graphics.Color,
    scrollState: androidx.compose.foundation.ScrollState,
    onTap: (Offset, Size) -> Unit,
    onPositionChanged: (Int) -> Unit,
    onTextSelected: (String, Int, Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var showHighlightMenu by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }
    var selectionStart by remember { mutableIntStateOf(0) }
    var selectionEnd by remember { mutableIntStateOf(0) }

    val paragraphs = remember(chapter.content) {
        chapter.content.split("\n\n")
    }

    val fontSizeValue = settings.fontSize
    val lineSpacingValue = settings.lineSpacing

    LaunchedEffect(scrollState.value) {
        onPositionChanged(scrollState.value)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        onTap(
                            offset,
                            Size(
                                size.width.toFloat(),
                                size.height.toFloat()
                            )
                        )
                    },
                    onLongPress = {
                        showHighlightMenu = true
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = 20.dp,
                    vertical = 80.dp)
        ) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = (fontSizeValue + 4).sp,
                    lineHeight = (fontSizeValue * lineSpacingValue + 8).sp
                ),
                color = textColor,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            paragraphs.forEach { paragraph ->
                if (paragraph.isNotBlank()) {
                    Text(
                        text = paragraph.trim(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSizeValue.sp,
                            lineHeight = (fontSizeValue * lineSpacingValue).sp,
                            textAlign = TextAlign.Justify
                        ),
                        color = textColor,
                        modifier = Modifier
                            .padding(bottom = settings.paragraphSpacing.toInt().dp)
                            .clickable {
                                selectedText = paragraph.trim().take(100)
                                selectionStart = 0
                                selectionEnd = selectedText.length
                                showHighlightMenu = true
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        if (showHighlightMenu) {
            HighlightMenu(
                onDismiss = { showHighlightMenu = false },
                onHighlight = { color ->
                    onTextSelected(selectedText, selectionStart, selectionEnd)
                    showHighlightMenu = false
                },
                textColor = textColor,
                backgroundColor = backgroundColor
            )
        }
    }
}

@Composable
fun HighlightMenu(
    onDismiss: () -> Unit,
    onHighlight: (AnnotationColor) -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    backgroundColor: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = backgroundColor.copy(alpha = 0.95f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "添加高亮",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnnotationColor.entries.forEach { color ->
                        IconButton(
                            onClick = { onHighlight(color) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    androidx.compose.ui.graphics.Color(color.colorValue),
                                    androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = color.name,
                                tint = androidx.compose.ui.graphics.Color.Black
                            )
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("取消", color = textColor)
                }
            }
        }
    }
}
