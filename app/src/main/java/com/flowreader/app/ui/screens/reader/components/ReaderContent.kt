package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.AnnotationColor
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.ReadingSettings
import java.io.File

@Composable
fun ReaderContent(
    chapter: Chapter,
    settings: ReadingSettings,
    textColor: androidx.compose.ui.graphics.Color,
    backgroundColor: androidx.compose.ui.graphics.Color,
    scrollState: androidx.compose.foundation.ScrollState,
    annotations: List<Annotation> = emptyList(),
    onTap: (Offset, Size) -> Unit,
    onPositionChanged: (Int) -> Unit,
    onTextSelected: (String, Int, Int, AnnotationColor) -> Unit = { _, _, _, _ -> },
    onBookmarkRequested: (String) -> Unit = {},
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
                    onLongPress = { offset ->
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

            var cumulativeOffset = 0
            paragraphs.forEachIndexed { index, paragraph ->
                val paraTrimmed = paragraph.trim()
                val paraStart = cumulativeOffset + paragraph.indexOf(paraTrimmed)
                val paraEnd = paraStart + paraTrimmed.length

                if (paraTrimmed.isNotBlank()) {
                    when {
                        paraTrimmed.startsWith("[IMG:") && paraTrimmed.endsWith("]") -> {
                            val imgPath = paraTrimmed.removePrefix("[IMG:").removeSuffix("]")
                            val imgFile = File(imgPath)
                            if (imgFile.exists()) {
                                AsyncImage(
                                    model = imgFile,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                        paraTrimmed.startsWith("## ") -> {
                            val headingText = paraTrimmed.removePrefix("## ")
                            Text(
                                text = headingText,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = (fontSizeValue + 6).sp,
                                    lineHeight = (fontSizeValue * lineSpacingValue + 12).sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = textColor,
                                modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                            )
                        }
                        else -> {
                            val paraAnnotations = annotations.filter {
                                it.startPosition >= paraStart && it.endPosition <= paraEnd
                            }

                            val baseStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = fontSizeValue.sp,
                                lineHeight = (fontSizeValue * lineSpacingValue).sp,
                                textAlign = TextAlign.Justify
                            )

                            val text = if (paraAnnotations.isNotEmpty()) {
                                buildAnnotatedString {
                                    var lastEnd = paraStart
                                    paraAnnotations.sortedBy { it.startPosition }.forEach { ann ->
                                        val relStart = ann.startPosition - paraStart
                                        val relEnd = ann.endPosition - paraStart
                                        if (relStart > lastEnd - paraStart) {
                                            append(paraTrimmed.substring(lastEnd - paraStart, relStart))
                                        }
                                        withStyle(SpanStyle(background = Color(ann.color.colorValue).copy(alpha = 0.4f))) {
                                            append(paraTrimmed.substring(relStart, relEnd))
                                        }
                                        lastEnd = ann.endPosition
                                    }
                                    if (lastEnd - paraStart < paraTrimmed.length) {
                                        append(paraTrimmed.substring(lastEnd - paraStart))
                                    }
                                }
                            } else {
                                buildFormattedText(paraTrimmed)
                            }

                            Text(
                                text = text,
                                style = baseStyle,
                                color = textColor,
                                modifier = Modifier
                                    .padding(bottom = settings.paragraphSpacing.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                selectedText = paraTrimmed
                                                selectionStart = paraStart
                                                selectionEnd = paraEnd
                                                showHighlightMenu = true
                                            }
                                        )
                                    }
                            )
                        }
                    }
                }
                cumulativeOffset += paragraph.length + 2
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        if (showHighlightMenu) {
            HighlightMenu(
                onDismiss = { 
                    showHighlightMenu = false
                    selectedText = ""
                },
                onHighlight = { color ->
                    if (selectedText.isNotEmpty()) {
                        onTextSelected(selectedText, selectionStart, selectionEnd, color)
                    }
                    showHighlightMenu = false
                    selectedText = ""
                },
                onBookmark = { note ->
                    onBookmarkRequested(note)
                    showHighlightMenu = false
                    selectedText = ""
                },
                textColor = textColor,
                backgroundColor = backgroundColor,
                selectedText = selectedText
            )
        }
    }
}

private fun buildFormattedText(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val boldStart = text.indexOf("**", i)
            val italicStart = text.indexOf("*", i)

            val nextMarker = when {
                boldStart >= 0 && italicStart >= 0 -> minOf(boldStart, italicStart)
                boldStart >= 0 -> boldStart
                italicStart >= 0 -> italicStart
                else -> -1
            }

            if (nextMarker < 0) {
                append(text.substring(i))
                break
            }

            if (nextMarker > i) {
                append(text.substring(i, nextMarker))
            }

            if (boldStart >= 0 && boldStart == nextMarker) {
                val boldEnd = text.indexOf("**", boldStart + 2)
                if (boldEnd >= 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(boldStart + 2, boldEnd))
                    }
                    i = boldEnd + 2
                } else {
                    append("**")
                    i = boldStart + 2
                }
            } else if (italicStart >= 0 && italicStart == nextMarker) {
                val italicEnd = text.indexOf("*", italicStart + 1)
                if (italicEnd >= 0 && (italicEnd > italicStart + 1) && !text.startsWith("*", italicEnd + 1)) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(italicStart + 1, italicEnd))
                    }
                    i = italicEnd + 1
                } else {
                    append("*")
                    i = italicStart + 1
                }
            } else {
                i = nextMarker + 1
            }
        }
    }
}

@Composable
fun HighlightMenu(
    onDismiss: () -> Unit,
    onHighlight: (AnnotationColor) -> Unit,
    onBookmark: (String) -> Unit = {},
    textColor: androidx.compose.ui.graphics.Color,
    backgroundColor: androidx.compose.ui.graphics.Color,
    selectedText: String = ""
) {
    var inputText by remember { mutableStateOf(selectedText) }
    
    LaunchedEffect(selectedText) {
        inputText = selectedText
    }
    
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
                
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("输入要高亮的文本") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnnotationColor.entries.forEach { color ->
                        IconButton(
                            onClick = { 
                                if (inputText.isNotEmpty()) {
                                    onHighlight(color)
                                }
                            },
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
                OutlinedButton(
                    onClick = { onBookmark(inputText.ifBlank { selectedText.ifBlank { "书签" } }) },
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text("添加书签备注", color = textColor)
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
