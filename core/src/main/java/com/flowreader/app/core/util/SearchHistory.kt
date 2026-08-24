package com.flowreader.app.core.util

/**
 * Encoding and update rules for the recent-search list.
 *
 * The list lives in one DataStore string, and it used to be a bare `"|"`-join. Nothing escaped the
 * delimiter, so a single query containing `|` split into two bogus history entries on the next read —
 * and since each fragment was then re-encoded, the damage was permanent. [encode] escapes instead.
 *
 * Pure and dependency-free so it is JVM-testable: `:core` has no `org.json` on its unit-test
 * classpath, and a real JSON array would need one for the sake of a flat list of strings.
 */
object SearchHistory {

    /** How many recent queries are kept. Older entries fall off the end. */
    const val MAX_ENTRIES: Int = 10

    private const val SEPARATOR = '|'
    private const val ESCAPE = '\\'

    /** Serializes [entries] into a single string that [decode] round-trips exactly. */
    fun encode(entries: List<String>): String = entries.joinToString(SEPARATOR.toString()) { entry ->
        buildString(entry.length) {
            for (c in entry) {
                if (c == ESCAPE || c == SEPARATOR) append(ESCAPE)
                append(c)
            }
        }
    }

    /**
     * Parses what [encode] wrote.
     *
     * Also reads the unescaped legacy format: a stored value with no backslashes decodes identically
     * either way, so no migration step is needed. Blank entries are dropped, which is what clears the
     * empty string [encode] produces for an empty list.
     */
    fun decode(stored: String?): List<String> {
        if (stored.isNullOrEmpty()) return emptyList()
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (c in stored) {
            when {
                escaped -> {
                    current.append(c)
                    escaped = false
                }
                c == ESCAPE -> escaped = true
                c == SEPARATOR -> {
                    out.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
        }
        // A trailing lone backslash cannot happen through encode(); treat it as a literal rather
        // than dropping the character it was meant to escape.
        if (escaped) current.append(ESCAPE)
        out.add(current.toString())
        return out.filter { it.isNotBlank() }
    }

    /**
     * [history] with [query] promoted to the front, capped at [MAX_ENTRIES].
     *
     * Re-running an old search **moves** it to the front. The previous rule skipped the whole update
     * when the query was already present, so the list froze in whatever order it first happened to
     * acquire and the user's most recent search could sit at position 10. Matching is
     * case-insensitive, so `Flow` does not become a second entry beside `flow`.
     */
    fun withQuery(history: List<String>, query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return history
        val withoutDuplicate = history.filterNot { it.equals(trimmed, ignoreCase = true) }
        return (listOf(trimmed) + withoutDuplicate).take(MAX_ENTRIES)
    }
}
