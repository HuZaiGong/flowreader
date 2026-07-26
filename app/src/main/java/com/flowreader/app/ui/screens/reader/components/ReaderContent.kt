package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.flowreader.app.core.designsystem.reader.ReaderMetrics
import com.flowreader.app.core.designsystem.reader.ReaderPalette
import com.flowreader.app.core.designsystem.reader.paragraphSpacing
import com.flowreader.app.core.designsystem.reader.readerBodyStyle
import com.flowreader.app.core.designsystem.reader.readerChapterTitleStyle
import com.flowreader.app.core.designsystem.reader.readerHeadingStyle
import com.flowreader.app.core.designsystem.reader.text
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.ReadingSettings
import java.io.File

/**
 * The reader body.
 *
 * Every style here comes from `:core`'s `ReaderTypography`, so the font family, the imported
 * custom font, the line height, the paragraph gap and the first-line indent all take effect —
 * before v52 the renderer hard-coded `bodyLarge` and dropped all four settings on the floor.
 */
@Composable
fun ReaderContent(
    chapter: Chapter,
    settings: ReadingSettings,
    palette: ReaderPalette,
    fontFamily: FontFamily,
    scrollState: ScrollState,
    onTap: (Offset, Size) -> Unit,
    onDoubleTap: () -> Unit,
    onHorizontalDrag: (Float) -> Unit,
    onParagraphLongPress: (String, Int, Int) -> Unit,
    onPositionChanged: (Int) -> Unit,
    annotations: List<Annotation> = emptyList(),
    modifier: Modifier = Modifier
) {
    val paragraphs = remember(chapter.content) { chapter.content.split("\n\n") }

    val bodyStyle = readerBodyStyle(settings, fontFamily, palette)
    val titleStyle = readerChapterTitleStyle(settings, fontFamily, palette)
    val headingStyle = readerHeadingStyle(settings, fontFamily, palette)
    val paragraphGap = paragraphSpacing(settings)
    val indentSp = ReaderMetrics.firstLineIndentSp(settings.fontSize, settings.firstLineIndent)

    LaunchedEffect(scrollState.value) {
        onPositionChanged(scrollState.value)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(settings.gestureSettings, settings.tapZoneRatio) {
                detectTapGestures(
                    onTap = { offset -> onTap(offset, Size(size.width.toFloat(), size.height.toFloat())) },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .pointerInput(settings.gestureSettings) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { onHorizontalDrag(total) },
                    onHorizontalDrag = { _, delta -> total += delta }
                )
            }
    ) {
        // Cap the measure so a tablet does not stretch a line to 100+ CJK glyphs.
        val contentWidth = ReaderMetrics.contentWidthDp(maxWidth.value, settings.fontSize).dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .width(contentWidth)
                    .padding(horizontal = FlowSpacing.lg, vertical = 80.dp)
            ) {
                Text(
                    text = chapter.title,
                    style = titleStyle,
                    modifier = Modifier.padding(bottom = FlowSpacing.xl)
                )

                var cumulativeOffset = 0
                paragraphs.forEach { paragraph ->
                    val paraTrimmed = paragraph.trim()
                    val paraStart = cumulativeOffset + paragraph.indexOf(paraTrimmed)
                    val paraEnd = paraStart + paraTrimmed.length

                    if (paraTrimmed.isNotBlank()) {
                        when {
                            paraTrimmed.startsWith("[IMG:") && paraTrimmed.endsWith("]") -> {
                                ReaderImage(
                                    path = paraTrimmed.removePrefix("[IMG:").removeSuffix("]"),
                                    tint = palette.text
                                )
                            }

                            paraTrimmed.startsWith("## ") -> {
                                Text(
                                    text = paraTrimmed.removePrefix("## "),
                                    style = headingStyle,
                                    modifier = Modifier.padding(top = FlowSpacing.sm, bottom = FlowSpacing.md)
                                )
                            }

                            else -> {
                                val paraAnnotations = annotations.filter {
                                    it.startPosition >= paraStart && it.endPosition <= paraEnd
                                }
                                val text = buildParagraph(paraTrimmed, paraStart, paraAnnotations, indentSp > 0f)

                                Text(
                                    text = text,
                                    style = bodyStyle,
                                    modifier = Modifier
                                        .padding(bottom = paragraphGap)
                                        .pointerInput(paraStart, paraEnd) {
                                            detectTapGestures(
                                                onLongPress = { onParagraphLongPress(paraTrimmed, paraStart, paraEnd) }
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
        }
    }
}

@Composable
private fun ReaderImage(path: String, tint: Color) {
    val file = remember(path) { File(path) }
    val exists = remember(path) { file.isFile }
    if (exists) {
        AsyncImage(
            model = file,
            contentDescription = "插图",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = FlowSpacing.sm)
                .clip(RoundedCornerShape(FlowSpacing.xs)),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(vertical = FlowSpacing.sm),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "插图缺失",
                tint = tint.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Applies stored highlights and inline markdown emphasis to one paragraph, prefixing the
 * two-character CJK indent when it is enabled.
 */
private fun buildParagraph(
    paragraph: String,
    paragraphStart: Int,
    annotations: List<Annotation>,
    indent: Boolean
): AnnotatedString {
    val prefix = if (indent) "　　" else ""
    if (annotations.isEmpty()) {
        return buildAnnotatedString {
            append(prefix)
            append(buildFormattedText(paragraph))
        }
    }
    return buildAnnotatedString {
        append(prefix)
        var lastEnd = 0
        annotations.sortedBy { it.startPosition }.forEach { annotation ->
            val relStart = (annotation.startPosition - paragraphStart).coerceIn(0, paragraph.length)
            val relEnd = (annotation.endPosition - paragraphStart).coerceIn(relStart, paragraph.length)
            if (relStart > lastEnd) {
                append(paragraph.substring(lastEnd, relStart))
            }
            withStyle(SpanStyle(background = Color(annotation.color.colorValue).copy(alpha = 0.4f))) {
                append(paragraph.substring(relStart, relEnd))
            }
            lastEnd = relEnd
        }
        if (lastEnd < paragraph.length) {
            append(paragraph.substring(lastEnd))
        }
    }
}

private fun buildFormattedText(text: String): AnnotatedString {
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
