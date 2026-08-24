package com.flowreader.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Emphasizes the matched run inside a search snippet.
 *
 * Both search surfaces — the global search screen and the reader's in-book dialog — used to render
 * the raw snippet, which came back wrapped in literal `<<`/`>>` markers. The index now reports where
 * the hit is instead, so the markers are gone and the emphasis is real styling.
 *
 * `start` is -1, and `length` 0, whenever the hit exists only in the normalized index — a match
 * spanning punctuation, for instance. Those render unhighlighted rather than emphasizing a guessed
 * range: pure text is a smaller lie than the wrong characters in bold.
 */
fun highlightedSnippet(text: String, start: Int, length: Int, style: SpanStyle): AnnotatedString {
    if (start < 0 || start >= text.length || length <= 0) return AnnotatedString(text)
    val end = (start + length).coerceAtMost(text.length)
    return buildAnnotatedString {
        append(text, 0, start)
        withStyle(style) { append(text, start, end) }
        append(text, end, text.length)
    }
}

/** [highlightedSnippet] with the app's standard match emphasis. */
@Composable
fun snippetWithMatchEmphasis(text: String, start: Int, length: Int): AnnotatedString =
    highlightedSnippet(
        text = text,
        start = start,
        length = length,
        style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    )
