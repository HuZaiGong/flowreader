package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
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
import kotlin.math.max
import kotlin.math.min

/**
 * The reader body.
 *
 * Every style here comes from `:core`'s `ReaderTypography`, so the font family, the imported
 * custom font, the line height, the paragraph gap and the first-line indent all take effect —
 * before v52 the renderer hard-coded `bodyLarge` and dropped all four settings on the floor.
 *
 * Since v54 long-pressing a paragraph opens an in-house text selection engine
 * ([ReaderParagraph]) with its own handles and the floating [ReaderSelectionBar]; the selected
 * range can be turned into a highlight, a bookmark or a copy. The platform `SelectionContainer`
 * was deliberately not used: its selection state API is `internal` in Compose 1.7.x.
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
    onHighlightSelection: (String, Int, Int) -> Unit,
    onBookmarkSelection: (String, Int, Int) -> Unit,
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
                                ReaderParagraph(
                                    paragraph = paraTrimmed,
                                    paragraphStart = paraStart,
                                    annotations = paraAnnotations,
                                    indent = indentSp > 0f,
                                    bodyStyle = bodyStyle,
                                    paragraphGap = paragraphGap,
                                    palette = palette,
                                    onHighlightSelection = onHighlightSelection,
                                    onBookmarkSelection = onBookmarkSelection
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

private enum class DragSide { START, END }

/**
 * One selectable paragraph: long-press selects a word, dragging extends the range, the two
 * handles adjust either bound, and [ReaderSelectionBar] offers highlight / copy / bookmark.
 *
 * All gesture math runs on the public [TextLayoutResult] API; the selection range is display
 * offsets, converted back to raw chapter offsets through [ParagraphContent.rawRange].
 */
@Composable
internal fun ReaderParagraph(
    paragraph: String,
    paragraphStart: Int,
    annotations: List<Annotation>,
    indent: Boolean,
    bodyStyle: TextStyle,
    paragraphGap: Dp,
    palette: ReaderPalette,
    onHighlightSelection: (String, Int, Int) -> Unit,
    onBookmarkSelection: (String, Int, Int) -> Unit
) {
    val content = remember(paragraph, paragraphStart, annotations, indent) {
        buildParagraphContent(paragraph, paragraphStart, annotations, indent)
    }
    val selectionState = remember(paragraphStart) { mutableStateOf<TextRange?>(null) }
    val layoutState = remember(paragraphStart) { mutableStateOf<TextLayoutResult?>(null) }
    val coordinatesState = remember(paragraphStart) { mutableStateOf<LayoutCoordinates?>(null) }
    val clipboard = LocalClipboardManager.current
    val viewConfiguration = LocalViewConfiguration.current

    val selectionColor = palette.text.copy(alpha = 0.28f)
    val handleColor = palette.text.copy(alpha = 0.85f)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val handleRadius = with(density) { 6.dp.toPx() }
    val handleTouchRadius = with(density) { 28.dp.toPx() }

    fun extendFromLongPress(position: Offset) {
        val layout = layoutState.value ?: return
        val offset = layout.getOffsetForPosition(position).coerceIn(0, layout.layoutInput.text.length)
        if (selectionState.value == null) {
            selectionState.value = layout.getWordBoundary(offset)
        } else {
            val current = selectionState.value!!
            selectionState.value = if (offset < (current.min + current.max) / 2) {
                TextRange(offset, current.end)
            } else {
                TextRange(current.start, offset)
            }
        }
    }

    fun dragHandle(side: DragSide, position: Offset) {
        val layout = layoutState.value ?: return
        val current = selectionState.value ?: return
        val offset = layout.getOffsetForPosition(position).coerceIn(0, layout.layoutInput.text.length)
        selectionState.value = when (side) {
            DragSide.START -> TextRange(min(offset, current.end), current.end)
            DragSide.END -> TextRange(current.start, max(offset, current.start))
        }
    }

    fun handlePositions(layout: TextLayoutResult, range: TextRange): Pair<Offset, Offset>? {
        if (range.collapsed) return null
        val startBox = layout.getBoundingBox(range.start)
        val endBox = layout.getBoundingBox(range.end - 1)
        return Offset(startBox.left, startBox.top) to Offset(endBox.right, endBox.bottom)
    }

    fun selectionRects(layout: TextLayoutResult, range: TextRange): List<Rect> {
        if (range.collapsed) return emptyList()
        val firstLine = layout.getLineForOffset(range.start)
        val lastLine = layout.getLineForOffset((range.end - 1).coerceAtLeast(range.start))
        return buildList {
            for (line in firstLine..lastLine) {
                val lineStart = layout.getLineStart(line)
                val lineEnd = layout.getLineEnd(line)
                val segStart = range.start.coerceAtLeast(lineStart)
                val segEnd = range.end.coerceAtMost(lineEnd)
                if (segEnd <= segStart) continue
                val left = layout.getBoundingBox(segStart).left
                val right = layout.getBoundingBox(segEnd - 1).right
                add(Rect(left, layout.getLineTop(line), right, layout.getLineBottom(line)))
            }
        }
    }

    Text(
        text = content.annotatedString,
        style = bodyStyle,
        onTextLayout = { layoutState.value = it },
        modifier = Modifier
            .padding(bottom = paragraphGap)
            .onGloballyPositioned { coordinatesState.value = it }
            .drawWithContent {
                drawContent()
                val layout = layoutState.value
                val range = selectionState.value
                if (layout != null && range != null && !range.collapsed) {
                    selectionRects(layout, range).forEach { rect ->
                        drawRect(selectionColor, topLeft = rect.topLeft, size = rect.size)
                    }
                    handlePositions(layout, range)?.let { (start, end) ->
                        drawHandle(start, handleColor, handleRadius)
                        drawHandle(end, handleColor, handleRadius)
                    }
                }
            }
            .pointerInput(layoutState.value, paragraphStart) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val layout = layoutState.value
                    val selection = selectionState.value
                    val handles = if (layout != null && selection != null) handlePositions(layout, selection) else null

                    val nearStart = handles != null && (down.position - handles.first).getDistance() <= handleTouchRadius
                    val nearEnd = handles != null && (down.position - handles.second).getDistance() <= handleTouchRadius

                    if (nearStart || nearEnd) {
                        down.consume()
                        val side = if (nearStart) DragSide.START else DragSide.END
                        drag(down.id) { change ->
                            change.consume()
                            dragHandle(side, change.position)
                        }
                        return@awaitEachGesture
                    }

                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress != null) {
                        longPress.consume()
                        extendFromLongPress(longPress.position)
                        drag(down.id) { change ->
                            change.consume()
                            extendFromLongPress(change.position)
                        }
                    }
                }
            }
            .pointerInput(paragraphStart) {
                detectTapGestures(
                    onTap = { selectionState.value = null },
                    onDoubleTap = { position ->
                        val layout = layoutState.value ?: return@detectTapGestures
                        val offset = layout.getOffsetForPosition(position)
                        selectionState.value = layout.getWordBoundary(offset)
                    }
                )
            }
    )

    val selection = selectionState.value
    val layout = layoutState.value
    val coordinates = coordinatesState.value
    if (selection != null && layout != null && coordinates != null && !selection.collapsed) {
        val displayText = layout.layoutInput.text
        val startBox = layout.getBoundingBox(selection.start)
        val endBox = layout.getBoundingBox(selection.end - 1)

        ReaderSelectionBar(
            windowTopLeft = coordinates.localToWindow(startBox.topLeft),
            windowBottomRight = coordinates.localToWindow(endBox.bottomRight),
            palette = palette,
            onHighlight = {
                val range = content.rawRange(selection.start, selection.end)
                if (range != null && !range.isEmpty()) {
                    val text = paragraph.substring(
                        (range.first - paragraphStart).coerceIn(0, paragraph.length),
                        (range.last - paragraphStart).coerceIn(0, paragraph.length)
                    )
                    onHighlightSelection(text, range.first, range.last)
                }
                selectionState.value = null
            },
            onCopy = {
                clipboard.setText(AnnotatedString(displayText.substring(selection.start, selection.end)))
                selectionState.value = null
            },
            onBookmark = {
                val range = content.rawRange(selection.start, selection.end)
                if (range != null && !range.isEmpty()) {
                    val text = paragraph.substring(
                        (range.first - paragraphStart).coerceIn(0, paragraph.length),
                        (range.last - paragraphStart).coerceIn(0, paragraph.length)
                    )
                    onBookmarkSelection(text, range.first, range.last)
                }
                selectionState.value = null
            },
            onDismiss = { selectionState.value = null }
        )
    }
}

private fun DrawScope.drawHandle(center: Offset, color: Color, radius: Float) {
    drawCircle(color = color, radius = radius, center = center)
    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = radius * 0.45f, center = center)
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
