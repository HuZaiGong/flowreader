package com.flowreader.app.ui.screens.reader.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.flowreader.app.domain.model.Annotation

/**
 * Rendered paragraph text plus the display-index -> raw-chapter-offset map.
 *
 * The reader body strips markdown markers (`**` / `*`), prepends the CJK indent and inserts
 * highlight spans, so a selection made against the rendered text cannot be persisted directly.
 * [rawOffsets][ParagraphContent.rawOffsets] maps every rendered character back to its offset in
 * the raw chapter content, or -1 for characters the renderer invented (the indent prefix).
 */
data class ParagraphContent(
    val annotatedString: AnnotatedString,
    val rawOffsets: IntArray
) {
    init {
        require(rawOffsets.size == annotatedString.length) {
            "rawOffsets must be parallel to the display text"
        }
    }

    /**
     * Converts a rendered selection range (inclusive start, exclusive end) into raw chapter
     * offsets. Returns null when either bound lands on an invented character.
     */
    fun rawRange(displayStart: Int, displayEnd: Int): IntRange? {
        if (displayStart < 0 || displayEnd > rawOffsets.size || displayStart >= displayEnd) return null
        val rawStart = rawOffsets[displayStart]
        val rawEnd = rawOffsets[displayEnd - 1]
        if (rawStart < 0 || rawEnd < 0) return null
        return rawStart..rawEnd
    }
}

/**
 * Builds the display text for one paragraph with markdown emphasis and stored highlight spans,
 * tracking every character back to the raw chapter content.
 *
 * @param paragraphStart offset of [paragraph] inside the raw chapter content.
 */
fun buildParagraphContent(
    paragraph: String,
    paragraphStart: Int,
    annotations: List<Annotation>,
    indent: Boolean
): ParagraphContent {
    val prefix = if (indent) "　　" else ""
    val rawOffsets = mutableListOf<Int>()
    val builder = AnnotatedString.Builder()

    fun appendRange(text: String, rawStart: Int) {
        for (i in text.indices) rawOffsets.add(rawStart + i)
    }

    fun appendRaw(text: String, rawStart: Int) {
        appendRange(text, rawStart)
        builder.append(text)
    }

    /** Appends a raw chunk applying `**bold**` / `*italic*` markdown while keeping offsets true. */
    fun appendFormatted(text: String, rawStart: Int) {
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
                appendRaw(text.substring(i), rawStart + i)
                break
            }

            if (nextMarker > i) {
                appendRaw(text.substring(i, nextMarker), rawStart + i)
            }

            if (boldStart >= 0 && boldStart == nextMarker) {
                val boldEnd = text.indexOf("**", boldStart + 2)
                if (boldEnd >= 0) {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendRange(text.substring(boldStart + 2, boldEnd), rawStart + boldStart + 2)
                        append(text.substring(boldStart + 2, boldEnd))
                    }
                    i = boldEnd + 2
                } else {
                    appendRaw("**", rawStart + boldStart)
                    i = boldStart + 2
                }
            } else if (italicStart >= 0 && italicStart == nextMarker) {
                val italicEnd = text.indexOf("*", italicStart + 1)
                if (italicEnd >= 0 && (italicEnd > italicStart + 1) && !text.startsWith("*", italicEnd + 1)) {
                    builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendRange(text.substring(italicStart + 1, italicEnd), rawStart + italicStart + 1)
                        append(text.substring(italicStart + 1, italicEnd))
                    }
                    i = italicEnd + 1
                } else {
                    appendRaw("*", rawStart + italicStart)
                    i = italicStart + 1
                }
            } else {
                i = nextMarker + 1
            }
        }
    }

    if (prefix.isNotEmpty()) {
        repeat(prefix.length) { rawOffsets.add(-1) }
        builder.append(prefix)
    }

    var lastEnd = 0
    annotations.sortedBy { it.startPosition }.forEach { annotation ->
        val relStart = (annotation.startPosition - paragraphStart).coerceIn(0, paragraph.length)
        val relEnd = (annotation.endPosition - paragraphStart).coerceIn(relStart, paragraph.length)
        if (relStart > lastEnd) {
            appendFormatted(paragraph.substring(lastEnd, relStart), paragraphStart + lastEnd)
        }
        builder.withStyle(SpanStyle(background = Color(annotation.color.colorValue).copy(alpha = 0.4f))) {
            appendRange(paragraph.substring(relStart, relEnd), paragraphStart + relStart)
            append(paragraph.substring(relStart, relEnd))
        }
        lastEnd = relEnd
    }
    if (lastEnd < paragraph.length) {
        appendFormatted(paragraph.substring(lastEnd), paragraphStart + lastEnd)
    }

    return ParagraphContent(builder.toAnnotatedString(), rawOffsets.toIntArray())
}
