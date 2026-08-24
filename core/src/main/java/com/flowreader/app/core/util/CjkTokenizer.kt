package com.flowreader.app.core.util

/**
 * Segmentation for the full-text-search index.
 *
 * FTS5's default `unicode61` tokenizer splits on non-alphanumeric characters only. It has no notion
 * of word boundaries inside CJK, so an entire run of Han characters becomes **one** token: indexing
 * 「这是一个关于心流阅读的故事」 stores that whole clause as a single term. Searching 「心流」 then
 * matches nothing, because a prefix query can only reach the *start* of a token. That is why Chinese
 * full-text search returned zero results for every query that was not the first word of a sentence.
 *
 * The fix is to segment in the application layer, before the text ever reaches SQLite:
 *
 *  1. [fold] the text — drop everything that is not a letter or digit, and lowercase what remains.
 *  2. Emit **overlapping bigrams** over the folded code points ([indexStream]).
 *
 * Queries go through the same two steps ([matchExpression]) and are issued as an FTS5 *phrase*, so
 * the bigrams must appear consecutively and in order. That positional constraint is what keeps the
 * scheme precise: 「海上上海」 does not match a query for 「海上上海」's reverse 「上海海上」, because
 * `上海 海海 海上` is not a consecutive run in `海上 上上 上海`.
 *
 * Folding *before* segmenting is what makes matches survive punctuation and script changes, which a
 * CJK-only bigram scheme silently misses: 「事，江」, 「流Flow」 and 「海1949」 all match, and English
 * gains mid-word matching (`eading` finds "reading") for free. `unicode61` keeps a mixed-script
 * bigram such as `流f` or `海1` as one token, since Han, Latin and digits are all alphanumeric.
 *
 * Cost: the index stream is ~3x the folded CJK length and ~2.4x for Latin. Only the bigram columns
 * are indexed — every column carrying original text is declared `UNINDEXED` — which keeps the
 * multiplier off the human-readable payload.
 *
 * Two known limits, both covered by tests:
 *  - A **single** code point has no bigram of its own, so [matchExpression] emits the prefix form
 *    `"x"*` to match any bigram starting with it. A code point that occurs *only* as the final
 *    character of a chapter's folded stream is therefore unreachable.
 *  - Text of exactly one code point is indexed as that code point alone, reachable by the same
 *    prefix form.
 *
 * Everything here is pure and Compose-free so it stays JVM-testable — see `CjkTokenizerTest` and
 * the end-to-end `FullTextSearchEngineTest`, which runs these expressions against a real FTS5 table.
 */
object CjkTokenizer {

    private const val TOKEN_SEPARATOR = ' '

    /**
     * Upper bound on how much of one chapter is segmented. A TXT file with no chapter markers is
     * parsed as a single chapter of up to 128MB; at ~3x expansion that would build a 384MB string.
     * Content past this point is not searchable, which is the lesser evil versus an OOM on import.
     */
    const val MAX_INDEX_CHARS: Int = 500_000

    /**
     * Reduces [text] to the stream that matching happens on: letters and digits only, lowercased.
     *
     * Because the result can only contain letters and digits, **no FTS5 operator can survive it** —
     * quotes, `*`, `^`, `-`, `NEAR` punctuation and parentheses are all dropped. Query escaping is
     * therefore structural rather than a blocklist.
     */
    fun fold(text: String): String {
        if (text.isEmpty()) return ""
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            if (Character.isLetterOrDigit(codePoint)) {
                out.appendCodePoint(Character.toLowerCase(codePoint))
            }
            i += Character.charCount(codePoint)
        }
        return out.toString()
    }

    /**
     * The value stored in an indexed FTS5 column: space-separated overlapping bigrams of
     * [fold]ed [text], truncated at [limit] input characters.
     */
    fun indexStream(text: String, limit: Int = MAX_INDEX_CHARS): String {
        val source = if (text.length > limit) text.substring(0, limit) else text
        return bigrams(fold(source))
    }

    /**
     * Whether [query] is worth sending to the index.
     *
     * A UI throttle, not a correctness rule: [matchExpression] handles any non-empty query. The
     * point is that a one-code-point *Latin* query (`a`) becomes the prefix form `"a"*` and matches
     * a large share of every book, whereas a one-character **CJK** query is an ordinary word — 「爱」,
     * 「山」 — and users do search for those. A flat "at least two characters" guard is what made
     * single-character Chinese search do nothing at all.
     */
    fun isSearchableQuery(query: String): Boolean {
        val folded = fold(query)
        if (folded.isEmpty()) return false
        if (folded.codePointCount(0, folded.length) > 1) return true
        val only = folded.codePointAt(0)
        // Basic-Latin letters and digits are the only single characters cheap enough to reject.
        return only > 0x7F
    }

    /**
     * The FTS5 `MATCH` expression for [query], or `null` when the query folds away to nothing and
     * no search is possible. Callers must treat `null` as "no results" rather than "match all".
     */
    fun matchExpression(query: String): String? {
        val folded = fold(query)
        if (folded.isEmpty()) return null
        // A lone code point produces no bigram, so match any bigram opening with it instead.
        if (folded.codePointCount(0, folded.length) == 1) return "\"$folded\"*"
        return "\"${bigrams(folded)}\""
    }

    private fun bigrams(folded: String): String {
        if (folded.isEmpty()) return ""
        val out = StringBuilder(folded.length * 3)
        var previousStart = -1
        var i = 0
        while (i < folded.length) {
            val width = Character.charCount(folded.codePointAt(i))
            if (previousStart >= 0) {
                if (out.isNotEmpty()) out.append(TOKEN_SEPARATOR)
                out.append(folded, previousStart, i + width)
            }
            previousStart = i
            i += width
        }
        // Single code point: index it on its own so the prefix form in matchExpression can find it.
        if (out.isEmpty()) out.append(folded)
        return out.toString()
    }
}
