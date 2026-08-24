package com.flowreader.app.domain.model

data class GlobalSearchResult(
    val bookId: Long,
    val bookTitle: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val matchedText: String,
    /**
     * Where the match starts inside [matchedText], or -1 when it could not be located.
     *
     * Lets the UI highlight the hit with an `AnnotatedString` instead of wrapping it in literal
     * `<<`/`>>` markers. It is -1 whenever the match only exists in the normalized index — a hit
     * spanning punctuation, for instance — and the UI then renders the snippet unhighlighted rather
     * than emphasizing the wrong characters.
     */
    val matchStart: Int = -1,
    /**
     * How many characters of [matchedText] to highlight, or 0 when [matchStart] is -1.
     *
     * Carried rather than derived from the query: the located run is the *normalized* query, which is
     * shorter than what the user typed whenever the query held punctuation or spaces.
     */
    val matchLength: Int = 0
)
