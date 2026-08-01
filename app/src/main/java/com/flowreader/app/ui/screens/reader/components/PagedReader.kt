package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.flowreader.app.core.designsystem.reader.ReaderMetrics
import com.flowreader.app.core.designsystem.reader.ReaderPalette
import com.flowreader.app.core.designsystem.reader.background
import com.flowreader.app.core.designsystem.reader.paragraphSpacing
import com.flowreader.app.core.designsystem.reader.readerBodyStyle
import com.flowreader.app.core.designsystem.reader.readerChapterTitleStyle
import com.flowreader.app.core.designsystem.reader.readerHeadingStyle
import com.flowreader.app.core.designsystem.reader.text
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.core.util.ReaderBehavior
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.GestureAction
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.PageMode
import com.flowreader.app.domain.model.ReadingSettings
import com.flowreader.feature.reader.Block
import com.flowreader.feature.reader.ChapterPaginator
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The `PAGED` page-turn mode: the chapter is measured with the live reader styles and split
 * into horizontal pages, then swiped left/right. Tap zones flip a single page instead of the
 * whole chapter; swipes still change chapters; progress folds the page fraction in.
 *
 * Pages are rebuilt when the chapter, the styles or the available size change; the pager
 * state is scoped per chapter (`key(chapterIndex)`) so chapter jumps land on the remembered
 * page.
 */
@Composable
fun PagedReader(
    chapter: Chapter,
    chapterIndex: Int,
    settings: ReadingSettings,
    palette: ReaderPalette,
    fontFamily: FontFamily,
    annotations: List<Annotation>,
    currentPosition: Int,
    onPageChanged: (Int, Float) -> Unit,
    onMiddleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onHorizontalDrag: (Float) -> Unit,
    onHighlightSelection: (String, Int, Int) -> Unit,
    onBookmarkSelection: (String, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val bodyStyle = readerBodyStyle(settings, fontFamily, palette)
    val titleStyle = readerChapterTitleStyle(settings, fontFamily, palette)
    val headingStyle = readerHeadingStyle(settings, fontFamily, palette)
    val gap = paragraphSpacing(settings)
    val textMeasurer = rememberTextMeasurer()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        val contentWidth = ReaderMetrics.contentWidthDp(maxWidth.value, settings.fontSize).dp
        val contentWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { contentWidth.toPx() }
        val pageHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) {
            (maxHeight - 80.dp).coerceAtLeast(1.dp).toPx().toInt()
        }
        val indentEnabled = ReaderMetrics.firstLineIndentSp(settings.fontSize, settings.firstLineIndent) > 0f
        val gapPx = with(androidx.compose.ui.platform.LocalDensity.current) { gap.toPx() }.roundToInt()

        fun measureBlock(text: String, indent: Boolean, isTitle: Boolean): Int {
            val display = buildParagraphContent(text, 0, emptyList(), indent && !isTitle).annotatedString
            val style = when {
                isTitle -> titleStyle
                text.startsWith("## ") -> headingStyle
                else -> bodyStyle
            }
            val result = textMeasurer.measure(
                text = display,
                style = style,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = contentWidthPx.toInt())
            )
            return result.size.height.toInt()
        }

        val paginator = remember(chapter, settings, maxWidth, maxHeight, textMeasurer) {
            ChapterPaginator(::measureBlock)
        }

        val blocks = remember(chapter.content) {
            buildList {
                add(Block(chapter.title, 0, isTitle = true))
                var cumulative = 0
                chapter.content.split("\n\n").forEach { paragraph ->
                    val trimmed = paragraph.trim()
                    val start = cumulative + paragraph.indexOf(trimmed)
                    if (trimmed.isNotBlank()) {
                        when {
                            trimmed.startsWith("[IMG:") && trimmed.endsWith("]") -> add(Block(trimmed, start, paragraph = false))
                            trimmed.startsWith("## ") -> add(Block(trimmed, start, isHeading = true, indent = false))
                            else -> add(Block(trimmed, start, indent = indentEnabled))
                        }
                    }
                    cumulative += paragraph.length + 2
                }
            }
        }

        val pages = remember(blocks, paginator, pageHeightPx, gapPx) {
            paginator.paginate(blocks, pageHeightPx, gapPx)
        }

        if (pages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("本章暂无内容", color = palette.text.copy(alpha = 0.5f))
            }
            return@BoxWithConstraints
        }

        key(chapterIndex) {
            val initialPage = currentPosition.coerceIn(0, pages.size - 1)
            val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pages.size })

            LaunchedEffect(currentPosition, pages.size) {
                val target = currentPosition.coerceIn(0, pages.size - 1)
                if (pagerState.currentPage != target) {
                    if (settings.pageMode == PageMode.SLIDE) {
                        pagerState.animateScrollToPage(target)
                    } else {
                        pagerState.scrollToPage(target)
                    }
                }
            }

            LaunchedEffect(pagerState, pages.size) {
                snapshotFlow { pagerState.currentPage }
                    .distinctUntilChanged()
                    .collect { page ->
                        val fraction = if (pages.size > 1) page.toFloat() / (pages.size - 1) else 0f
                        onPageChanged(page, fraction)
                    }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(settings.gestureSettings, settings.tapZoneRatio, pages.size) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = { onHorizontalDrag(total) },
                            onHorizontalDrag = { _, delta -> total += delta }
                        )
                    }
                    .pointerInput(settings.gestureSettings, settings.tapZoneRatio, pages.size) {
                        detectTapGestures(
                            onTap = { offset ->
                                val action = ReaderBehavior.tapAction(offset.x, size.width.toFloat(), settings)
                                when (action) {
                                    GestureAction.PREVIOUS_PAGE -> {
                                        if (pagerState.currentPage > 0) {
                                            scope.launch { pagerState.scrollToPage(pagerState.currentPage - 1) }
                                        }
                                    }
                                    GestureAction.NEXT_PAGE -> {
                                        if (pagerState.currentPage < pages.size - 1) {
                                            scope.launch { pagerState.scrollToPage(pagerState.currentPage + 1) }
                                        }
                                    }
                                    else -> onMiddleTap()
                                }
                            },
                            onDoubleTap = { onDoubleTap() }
                        )
                    }
            ) { pageIndex ->
                PageContent(
                    fragments = pages[pageIndex],
                    bodyStyle = bodyStyle,
                    titleStyle = titleStyle,
                    headingStyle = headingStyle,
                    contentWidth = contentWidth,
                    gap = gap,
                    palette = palette,
                    annotations = annotations,
                    onHighlightSelection = onHighlightSelection,
                    onBookmarkSelection = onBookmarkSelection
                )
            }
        }
    }
}

@Composable
private fun PageContent(
    fragments: List<ChapterPaginator.Fragment>,
    bodyStyle: TextStyle,
    titleStyle: TextStyle,
    headingStyle: TextStyle,
    contentWidth: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
    palette: ReaderPalette,
    annotations: List<Annotation>,
    onHighlightSelection: (String, Int, Int) -> Unit,
    onBookmarkSelection: (String, Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = FlowSpacing.lg, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.width(contentWidth)) {
            fragments.forEach { fragment ->
                if (fragment.isTitle) {
                    Text(
                        text = fragment.text,
                        style = titleStyle,
                        modifier = Modifier.padding(bottom = FlowSpacing.xl)
                    )
                } else if (fragment.isHeading) {
                    Text(
                        text = fragment.text.removePrefix("## "),
                        style = headingStyle,
                        modifier = Modifier.padding(top = FlowSpacing.sm, bottom = FlowSpacing.md)
                    )
                } else {
                    val fragmentEnd = fragment.paragraphStart + fragment.text.length
                    val fragmentAnnotations = annotations.filter {
                        it.startPosition >= fragment.paragraphStart && it.endPosition <= fragmentEnd
                    }
                    ReaderParagraph(
                        paragraph = fragment.text,
                        paragraphStart = fragment.paragraphStart,
                        annotations = fragmentAnnotations,
                        indent = fragment.indent,
                        bodyStyle = bodyStyle,
                        paragraphGap = gap,
                        palette = palette,
                        onHighlightSelection = onHighlightSelection,
                        onBookmarkSelection = onBookmarkSelection
                    )
                }
            }
        }
    }
}
