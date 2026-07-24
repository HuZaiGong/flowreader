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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.AnnotationColor
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.ReadingSettings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

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
                        buildAnnotatedString { append(paraTrimmed) }
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
                                        selectionStart = 0
                                        selectionEnd = paraTrimmed.length
                                        showHighlightMenu = true
                                    }
                                )
                            }
                    )
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
                        onTextSelected(selectedText, selectionStart, selectionEnd)
                    }
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

@Composable
fun HighlightMenu(
    onDismiss: () -> Unit,
    onHighlight: (AnnotationColor) -> Unit,
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
