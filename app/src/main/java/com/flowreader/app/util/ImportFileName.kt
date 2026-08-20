package com.flowreader.app.util

import java.io.File

/**
 * The one place that turns an untrusted display name into a filename this app is willing to write.
 *
 * Everything about an imported file's name comes from outside: `ContentResolver` hands back whatever
 * `DISPLAY_NAME` the source provider chose, and a `file://` URI's last path segment is just as
 * attacker-controlled when the URI arrives through `ACTION_VIEW`. Before v56.6.2 that string went
 * straight into `File(booksDir, fileName)`, so a provider returning `../../databases/flowreader_db`
 * wrote outside `filesDir/books` — over the Room database, or over DataStore's
 * `datastore/settings.preferences_pb`.
 *
 * Two independent guards, because either one alone is a single point of failure:
 *
 * 1. [sanitize] strips path structure out of the name. Separators, control characters and the
 *    `..` segment cannot survive it.
 * 2. [resolveWithin] re-checks the *resolved* path with `canonicalFile`, which is what actually
 *    proves containment — it collapses `..` and follows symlinks, so it catches a traversal that
 *    slipped through sanitizing as well as a `books/` entry that is itself a symlink out of
 *    `filesDir`.
 *
 * Unicode is deliberately preserved: book titles here are overwhelmingly Chinese, and the existing
 * ASCII-only `sanitizeFileName` (used for comic *directory* names) would reduce 三体.epub to
 * `_.epub`, collapsing distinct books onto one filename. Letters and digits in any script are kept;
 * everything that could carry path meaning is not.
 */
object ImportFileName {

    /** Long enough for real titles, short enough to stay clear of filesystem name limits. */
    private const val MAX_NAME_LENGTH = 120

    private const val FALLBACK = "未知书籍"

    /** Anything that is not a letter, digit, dot, underscore, hyphen or space collapses to `_`. */
    private val DISALLOWED = Regex("[^\\p{L}\\p{N}._\\- ]+")

    /**
     * Reduces [raw] to a bare filename with no path structure. Never returns a blank string, never
     * returns `.` or `..`, and never returns a value containing a path separator.
     */
    fun sanitize(raw: String?): String {
        val candidate = raw?.trim().orEmpty()
        if (candidate.isEmpty()) return FALLBACK

        // Take the last segment first: a name like "a/b/../../evil.epub" loses its structure here
        // rather than being partially rewritten into something still meaningful to the filesystem.
        val lastSegment = candidate
            .replace('\\', '/')
            .substringAfterLast('/')

        val collapsed = DISALLOWED.replace(lastSegment, "_").trim().trim('_')

        // A name of only dots ("..", ".", "...") carries directory meaning; reject it outright
        // rather than trying to salvage characters from it.
        if (collapsed.isEmpty() || collapsed.all { it == '.' }) return FALLBACK

        // Leading dots would hide the file and, more importantly, are how "..foo" style names try
        // to look harmless; the extension separator is still preserved inside the name.
        val visible = collapsed.trimStart('.').ifBlank { FALLBACK }

        return if (visible.length <= MAX_NAME_LENGTH) {
            visible
        } else {
            // Truncate the stem, keep the extension so format detection still works.
            val extension = visible.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it.length <= 8 }
            val stemBudget = if (extension == null) MAX_NAME_LENGTH else MAX_NAME_LENGTH - extension.length - 1
            val stem = visible.substringBeforeLast('.').take(stemBudget).trim().ifBlank { FALLBACK }
            if (extension == null) stem else "$stem.$extension"
        }
    }

    /**
     * Resolves [fileName] inside [directory] and returns the file only if it genuinely lands there.
     *
     * Returns null when the resolved canonical path escapes [directory] — the caller must treat that
     * as a failed import rather than falling back to a guessed name, since by then the name is known
     * to be hostile.
     */
    fun resolveWithin(directory: File, fileName: String): File? {
        val safeName = sanitize(fileName)
        val target = File(directory, safeName)

        val canonicalDirectory = runCatching { directory.canonicalFile }.getOrNull() ?: return null
        val canonicalTarget = runCatching { target.canonicalFile }.getOrNull() ?: return null

        // Compare against the directory path plus a separator so `/books` cannot be satisfied by a
        // sibling named `/booksomething`.
        val prefix = canonicalDirectory.path + File.separator
        return if (canonicalTarget.path.startsWith(prefix)) target else null
    }
}
