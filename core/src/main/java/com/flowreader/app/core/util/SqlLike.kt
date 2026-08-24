package com.flowreader.app.core.util

/**
 * Escaping for Room `LIKE` predicates.
 *
 * `LIKE '%' || :query || '%'` treats `%` and `_` in the *bound argument* as wildcards, so a user
 * typing a single `%` into the search box matched every row and returned the entire library — which
 * reads as "search is broken" rather than as a wildcard feature. Every `LIKE` query that
 * concatenates a user-supplied argument must pass it through [escape] and declare `ESCAPE '\'`.
 *
 * SQLite's `ESCAPE` clause accepts exactly one character, hence the single backslash; the backslash
 * itself has to be escaped first or `\%` typed by the user would consume the following character.
 */
object SqlLike {

    /** The escape character declared by the `ESCAPE` clause of every query using [escape]. */
    const val ESCAPE_CHAR: String = "\\"

    /** Neutralizes `\`, `%` and `_` so [query] is matched literally. */
    fun escape(query: String): String {
        if (query.isEmpty()) return query
        val out = StringBuilder(query.length)
        for (c in query) {
            if (c == '\\' || c == '%' || c == '_') out.append('\\')
            out.append(c)
        }
        return out.toString()
    }
}
