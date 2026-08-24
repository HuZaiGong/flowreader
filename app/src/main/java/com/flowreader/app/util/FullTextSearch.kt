package com.flowreader.app.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.flowreader.app.core.util.CjkTokenizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FullTextSearch @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FullTextSearch"
        private const val DB_NAME = "flowreader_fts.db"

        /**
         * Schema version of `flowreader_fts.db`, tracked in `PRAGMA user_version`.
         *
         * The index is **purely derived data** — every row can be rebuilt from Room plus the book
         * files — so a version bump drops and recreates everything rather than migrating. Bump this
         * whenever the schema or the segmentation in [CjkTokenizer] changes, or existing installs
         * keep querying an index built by the old rules.
         *
         * 1 — original schema: every column indexed, `unicode61` tokenizer (no CJK support).
         * 2 — original text columns `UNINDEXED`; searchable text moved to bigram streams.
         */
        private const val SCHEMA_VERSION = 2

        /** Characters of original text kept on each side of a match in the returned snippet. */
        private const val SNIPPET_CONTEXT = 24

        /**
         * The window `instr` scans to locate the match for the snippet.
         *
         * A match can only exist inside the segmented prefix, so scanning past
         * [CjkTokenizer.MAX_INDEX_CHARS] is provably useless. The bound matters because a TXT file
         * with no chapter markers is parsed as one chapter of up to 128MB — an unbounded `instr`
         * would scan all of it, per result row, to place a 50-character snippet.
         */
        private const val SNIPPET_SCAN_WINDOW = CjkTokenizer.MAX_INDEX_CHARS

        /**
         * Slices the snippet out of the original `content` column.
         *
         * Not `snippet()`: the indexed columns hold space-separated bigrams, so `snippet()` would
         * return shredded text. Slicing happens inside SQLite so a 16MB chapter is never pulled
         * into memory just to show ~50 characters.
         *
         * Both sides go through `lower()` because SQLite's `instr` is a byte comparison — without
         * it, searching "Flow" would fail to locate "flow" in the text and fall back to the head of
         * the chapter. `lower()` only folds ASCII, so it is length-preserving for the scripts this
         * app sees and the offsets stay valid. `instr` is 1-based and returns 0 when the match has
         * no literal counterpart in the original text — the indexed stream drops punctuation, so a
         * hit spanning 「事，江」 has none — and `MAX(1, …)` turns that into a head-of-chapter
         * snippet rather than an error. Binds the needle twice.
         */
        private const val SNIPPET_SLICE =
            "substr(content, " +
                "MAX(1, instr(lower(substr(content, 1, $SNIPPET_SCAN_WINDOW)), ?) - $SNIPPET_CONTEXT), " +
                "length(?) + ${SNIPPET_CONTEXT * 2}) AS matched_text"

        /**
         * In-book search, scoped to one book.
         *
         * `book_id` **must** be compared through `CAST(? AS INTEGER)`. `book_content_fts` is an
         * FTS5 external-content table, so its columns have no declared type and therefore no
         * column affinity, while the values come back from `book_content.book_id` as INTEGER.
         * `SQLiteDatabase.rawQuery()` can only bind `String` arguments, and SQLite never treats
         * INTEGER 5 as equal to TEXT '5' when no affinity converts one side — so a bare
         * `book_id = ?` matched nothing and in-book search always returned zero results.
         * Declaring the column `UNINDEXED` does not change that: `UNINDEXED` only removes the
         * column from the full-text index, it does not give it an affinity.
         * Guarded by `FullTextSearchQueryTest` and `FullTextSearchEngineTest`.
         */
        internal const val SEARCH_IN_BOOK_SQL =
            "SELECT chapter_index, chapter_title, $SNIPPET_SLICE\n" +
                "FROM book_content_fts\n" +
                "WHERE book_id = CAST(? AS INTEGER) AND book_content_fts MATCH ?\n" +
                "ORDER BY rank\n" +
                "LIMIT ?"

        /** Global search across every indexed book; no book scoping, so no CAST needed. */
        internal const val SEARCH_ALL_SQL =
            "SELECT book_id, chapter_index, chapter_title, $SNIPPET_SLICE\n" +
                "FROM book_content_fts\n" +
                "WHERE book_content_fts MATCH ?\n" +
                "ORDER BY rank\n" +
                "LIMIT ? OFFSET ?"

        /**
         * Re-indexing a chapter is a DELETE followed by an INSERT, run in one transaction — **not**
         * `INSERT OR REPLACE`.
         *
         * SQLite fires DELETE triggers for rows removed by `REPLACE` conflict resolution
         * [only when `PRAGMA recursive_triggers` is on](https://sqlite.org/lang_conflict.html), and
         * it is off by default. So an upsert on `UNIQUE(book_id, chapter_index)` dropped the old row
         * without firing `book_content_ad`, leaving its terms in `book_content_fts` pointing at a
         * rowid that no longer existed — and since `AUTOINCREMENT` never reuses a rowid, the insert
         * could not repair it either. The next query against that book failed with
         * `SQLITE_CORRUPT_VTAB`, permanently, because an external-content table trusts the index to
         * agree with its content table. Every reader open re-indexes, so this was one book-reopen
         * away for anyone. `FullTextSearchEngineTest` covers it.
         */
        internal const val DELETE_CHAPTER_SQL =
            "DELETE FROM book_content WHERE book_id = ? AND chapter_index = ?"

        /** Writes one chapter. Binds 6 args; the last two are segmented streams. */
        internal const val INSERT_CHAPTER_SQL =
            "INSERT INTO book_content " +
                "(book_id, chapter_index, chapter_title, content, title_indexed, content_indexed) " +
                "VALUES (?, ?, ?, ?, ?, ?)"

        internal const val SELECT_INDEXED_BOOKS_SQL = "SELECT book_id FROM indexed_books"

        /**
         * `REPLACE` is safe here in a way it is not for `book_content`: `indexed_books` carries no
         * triggers, so nothing depends on a DELETE firing. See [DELETE_CHAPTER_SQL].
         */
        internal const val MARK_INDEXED_SQL =
            "INSERT OR REPLACE INTO indexed_books(book_id, chapter_count) VALUES (?, ?)"

        /**
         * The schema, as constants so `FullTextSearchEngineTest` can execute the *shipped* DDL
         * against a real FTS5 build. Hand-copying it into the test would let a schema change pass
         * while the test kept validating the old one — the failure mode this whole test exists to
         * prevent.
         */
        internal val CREATE_CONTENT_TABLE =
            """
            CREATE TABLE IF NOT EXISTS book_content(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id INTEGER NOT NULL,
                chapter_index INTEGER NOT NULL,
                chapter_title TEXT,
                content TEXT,
                title_indexed TEXT,
                content_indexed TEXT,
                UNIQUE(book_id, chapter_index)
            )
            """.trimIndent()

        /**
         * Which books are **completely** indexed, written only after every chapter of a book has
         * landed.
         *
         * `SELECT DISTINCT book_id FROM book_content` cannot answer this: a book interrupted halfway
         * through indexing has rows, so it would look done and its remaining chapters would never be
         * searchable. Keeping the marker in this database rather than in DataStore also means index
         * state cannot drift from the index — dropping the file drops both, and the old in-memory
         * flags no longer lied after process death.
         */
        internal val CREATE_INDEXED_BOOKS_TABLE =
            """
            CREATE TABLE IF NOT EXISTS indexed_books(
                book_id INTEGER PRIMARY KEY,
                chapter_count INTEGER NOT NULL
            )
            """.trimIndent()

        /**
         * Only the two `*_indexed` columns carry searchable text; they hold overlapping bigrams of
         * the folded original (see [CjkTokenizer]). Everything a result needs to *display* is
         * `UNINDEXED`: keeping `book_id` and `chapter_index` out of the index stops a query for "5"
         * from matching every chapter of book 5, and keeping `content` out avoids indexing each
         * chapter twice, once raw and once segmented.
         */
        internal val CREATE_FTS_TABLE =
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS book_content_fts USING fts5(
                book_id UNINDEXED,
                chapter_index UNINDEXED,
                chapter_title UNINDEXED,
                content UNINDEXED,
                title_indexed,
                content_indexed,
                content='book_content',
                content_rowid='id'
            )
            """.trimIndent()

        internal val CREATE_TRIGGER_INSERT =
            """
            CREATE TRIGGER IF NOT EXISTS book_content_ai AFTER INSERT ON book_content BEGIN
                INSERT INTO book_content_fts(
                    rowid, book_id, chapter_index, chapter_title, content, title_indexed, content_indexed)
                VALUES (
                    new.id, new.book_id, new.chapter_index, new.chapter_title, new.content,
                    new.title_indexed, new.content_indexed);
            END
            """.trimIndent()

        internal val CREATE_TRIGGER_DELETE =
            """
            CREATE TRIGGER IF NOT EXISTS book_content_ad AFTER DELETE ON book_content BEGIN
                INSERT INTO book_content_fts(
                    book_content_fts, rowid, book_id, chapter_index, chapter_title, content,
                    title_indexed, content_indexed)
                VALUES (
                    'delete', old.id, old.book_id, old.chapter_index, old.chapter_title, old.content,
                    old.title_indexed, old.content_indexed);
            END
            """.trimIndent()

        internal val CREATE_TRIGGER_UPDATE =
            """
            CREATE TRIGGER IF NOT EXISTS book_content_au AFTER UPDATE ON book_content BEGIN
                INSERT INTO book_content_fts(
                    book_content_fts, rowid, book_id, chapter_index, chapter_title, content,
                    title_indexed, content_indexed)
                VALUES (
                    'delete', old.id, old.book_id, old.chapter_index, old.chapter_title, old.content,
                    old.title_indexed, old.content_indexed);
                INSERT INTO book_content_fts(
                    rowid, book_id, chapter_index, chapter_title, content, title_indexed, content_indexed)
                VALUES (
                    new.id, new.book_id, new.chapter_index, new.chapter_title, new.content,
                    new.title_indexed, new.content_indexed);
            END
            """.trimIndent()

        /** Every DDL statement, in dependency order: tables before the triggers that reference them. */
        internal val SCHEMA_SQL = listOf(
            CREATE_CONTENT_TABLE,
            CREATE_INDEXED_BOOKS_TABLE,
            CREATE_FTS_TABLE,
            CREATE_TRIGGER_INSERT,
            CREATE_TRIGGER_DELETE,
            CREATE_TRIGGER_UPDATE
        )
    }

    @Volatile
    private var database: SQLiteDatabase? = null

    /**
     * Serializes every index mutation. `deleteAllContent()` + re-index (global rebuild) and
     * `deleteBookContent()` + re-index (opening a book in the reader) must not interleave, or a
     * book can end up deleted from the index after the rebuild already counted it as fresh.
     * The lock lives here rather than in one caller so no code path can skip it.
     */
    private val indexMutex = Mutex()

    /**
     * Serializes database initialization. Without this, concurrent callers can both pass
     * `if (database?.isOpen == true)` and both open a handle, leaking the first one.
     */
    private val initMutex = Mutex()

    /** Runs [block] with exclusive access to the index. Not re-entrant — never nest calls. */
    suspend fun <T> withIndexLock(block: suspend () -> T): T = indexMutex.withLock { block() }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (database?.isOpen == true) return@withContext

        initMutex.withLock {
            // Re-check inside the lock — another coroutine may have initialized while we waited.
            if (database?.isOpen == true) return@withContext

            try {
                val db = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null)
                database = db
                migrateIfNeeded(db)
                createTablesAndTriggers()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize FTS database", e)
                throw FtsException("Failed to initialize FTS database", e)
            }
        }
    }

    /**
     * Drops an index built by an older schema so [createTablesAndTriggers] can rebuild it.
     *
     * Without this, every statement in [createTablesAndTriggers] is a no-op on an existing install
     * (they are all `IF NOT EXISTS`), so a device that already searched once would keep the v1
     * tables — whose `unicode61` index cannot match Chinese at all — forever. Dropping
     * `indexed_books` with them is what makes the repopulation happen: maintenance indexes whatever
     * the marker table says is missing, so clearing the markers schedules the whole library.
     */
    private fun migrateIfNeeded(db: SQLiteDatabase) {
        val current = db.rawQuery("PRAGMA user_version", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        if (current == SCHEMA_VERSION) return

        Log.i(TAG, "Rebuilding FTS index: schema $current -> $SCHEMA_VERSION")
        // Triggers reference the tables, so they have to go first.
        db.execSQL("DROP TRIGGER IF EXISTS book_content_ai")
        db.execSQL("DROP TRIGGER IF EXISTS book_content_ad")
        db.execSQL("DROP TRIGGER IF EXISTS book_content_au")
        db.execSQL("DROP TABLE IF EXISTS book_content_fts")
        db.execSQL("DROP TABLE IF EXISTS book_content")
        db.execSQL("DROP TABLE IF EXISTS indexed_books")
        db.execSQL("PRAGMA user_version = $SCHEMA_VERSION")
    }

    private fun createTablesAndTriggers() {
        val db = database ?: return
        SCHEMA_SQL.forEach(db::execSQL)
    }

    /**
     * The books that are fully indexed, so a caller can index only what is missing.
     *
     * Returns an empty set on a fresh install and after a schema migration wiped the index, which is
     * what makes "index the difference" also handle "index everything" without a separate flag.
     */
    suspend fun indexedBookIds(): Set<Long> = withContext(Dispatchers.IO) {
        initialize()
        val ids = mutableSetOf<Long>()
        database?.rawQuery(SELECT_INDEXED_BOOKS_SQL, null)?.use {
            while (it.moveToNext()) ids.add(it.getLong(0))
        }
        ids
    }

    /**
     * Records that every chapter of [bookId] is in the index. Call this **after** the last
     * [indexChapter] for that book — a book without this marker is treated as not indexed and gets
     * re-indexed, which is the correct outcome for one interrupted partway through.
     */
    suspend fun markBookIndexed(bookId: Long, chapterCount: Int) = withContext(Dispatchers.IO) {
        initialize()
        try {
            database?.execSQL(
                MARK_INDEXED_SQL,
                arrayOf(bookId.toString(), chapterCount.toString())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark book indexed", e)
            throw FtsException("Failed to mark book indexed", e)
        }
    }

    private fun ensureInitialized() {
        val db = database
        if (db == null || !db.isOpen) {
            throw FtsNotInitializedException("FullTextSearch has not been initialized")
        }
    }

    suspend fun indexChapter(
        bookId: Long,
        chapterIndex: Int,
        chapterTitle: String,
        content: String
    ) = withContext(Dispatchers.IO) {
        ensureInitialized()
        val db = database ?: return@withContext
        try {
            // One transaction so the delete can never be visible without its replacement: a crash
            // in between would otherwise drop the chapter out of the index entirely.
            db.beginTransaction()
            try {
                db.execSQL(DELETE_CHAPTER_SQL, arrayOf(bookId.toString(), chapterIndex.toString()))
                db.execSQL(
                    INSERT_CHAPTER_SQL,
                    arrayOf(
                        bookId.toString(),
                        chapterIndex.toString(),
                        chapterTitle,
                        content,
                        CjkTokenizer.indexStream(chapterTitle),
                        CjkTokenizer.indexStream(content)
                    )
                )
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to index chapter", e)
            throw FtsException("Failed to index chapter", e)
        }
    }

    suspend fun search(
        bookId: Long,
        query: String,
        maxResults: Int = 50
    ): List<FtsSearchResult> = withContext(Dispatchers.IO) {
        // Callers reach in-book search straight from the reader's menu, which can win the race
        // against the fire-and-forget indexing launched when the book opened. Initializing here
        // rather than throwing FtsNotInitializedException is what keeps that from surfacing as
        // "no results".
        initialize()

        // A query of nothing but punctuation folds away to nothing; there is no term to match.
        val match = CjkTokenizer.matchExpression(query) ?: return@withContext emptyList()
        val needle = CjkTokenizer.fold(query)

        try {
            val results = mutableListOf<FtsSearchResult>()
            val cursor = database?.rawQuery(
                SEARCH_IN_BOOK_SQL,
                arrayOf(needle, needle, bookId.toString(), match, maxResults.toString())
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val snippet = it.getString(2).orEmpty()
                    val start = highlightStart(snippet, needle)
                    results.add(
                        FtsSearchResult(
                            chapterIndex = it.getInt(0),
                            chapterTitle = it.getString(1).orEmpty(),
                            matchedText = snippet,
                            bookId = bookId,
                            matchStart = start,
                            matchLength = highlightLength(start, needle)
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "FTS search failed", e)
            throw FtsException("FTS search failed for query: $query", e)
        }
    }

    suspend fun searchAll(
        query: String,
        maxResults: Int = 100,
        offset: Int = 0
    ): List<FtsSearchResult> = withContext(Dispatchers.IO) {
        initialize()
        val match = CjkTokenizer.matchExpression(query) ?: return@withContext emptyList()
        val needle = CjkTokenizer.fold(query)

        try {
            val results = mutableListOf<FtsSearchResult>()
            val cursor = database?.rawQuery(
                SEARCH_ALL_SQL,
                arrayOf(needle, needle, match, maxResults.toString(), offset.toString())
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val snippet = it.getString(3).orEmpty()
                    val start = highlightStart(snippet, needle)
                    results.add(
                        FtsSearchResult(
                            bookId = it.getLong(0),
                            chapterIndex = it.getInt(1),
                            chapterTitle = it.getString(2).orEmpty(),
                            matchedText = snippet,
                            matchStart = start,
                            matchLength = highlightLength(start, needle)
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Global FTS search failed", e)
            throw FtsException("Global FTS search failed for query: $query", e)
        }
    }

    /**
     * Where the match begins inside [snippet], or -1 when it cannot be located.
     *
     * The snippet is ~50 characters, so scanning it in Kotlin is cheaper than a second `instr` over
     * the chapter. [needle] is the folded query; it has no literal counterpart in the original text
     * whenever the hit spanned punctuation or a script boundary (a match on 「事，江」 folds to
     * `事江`), and the UI then renders the snippet without a highlight rather than highlighting the
     * wrong range.
     */
    private fun highlightStart(snippet: String, needle: String): Int {
        if (snippet.isEmpty() || needle.isEmpty()) return -1
        return snippet.indexOf(needle, ignoreCase = true)
    }

    /** Length of the highlight [highlightStart] located, or 0 when there is none. */
    private fun highlightLength(start: Int, needle: String): Int = if (start < 0) 0 else needle.length

    /**
     * Removes several books from the index, taking the index lock itself.
     *
     * Prefer this over [withIndexLock] + [deleteBookContent] at call sites that are not already
     * holding the lock: the lock is not re-entrant, so which variant a caller needs is easy to get
     * wrong, and getting it wrong deadlocks. Callers already inside [withIndexLock] must use
     * [deleteBookContent] directly.
     */
    suspend fun deleteBooks(bookIds: List<Long>) {
        if (bookIds.isEmpty()) return
        withIndexLock { bookIds.forEach { deleteBookContent(it) } }
    }

    suspend fun deleteBookContent(bookId: Long) = withContext(Dispatchers.IO) {
        initialize()
        try {
            // The marker goes with the content: leaving it behind would report a book with no rows
            // as indexed, so it would never be re-indexed.
            database?.execSQL("DELETE FROM book_content WHERE book_id = ?", arrayOf(bookId.toString()))
            database?.execSQL("DELETE FROM indexed_books WHERE book_id = ?", arrayOf(bookId.toString()))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete book content", e)
            throw FtsException("Failed to delete book content", e)
        }
    }

    suspend fun deleteAllContent() = withContext(Dispatchers.IO) {
        // initialize(), not ensureInitialized(): rebuildIndex() calls this as the first thing it does
        // inside the index lock, so on a fresh process nothing has opened the database yet.
        initialize()
        try {
            database?.execSQL("DELETE FROM book_content")
            database?.execSQL("DELETE FROM indexed_books")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all indexed content", e)
            throw FtsException("Failed to delete all indexed content", e)
        }
    }

    /**
     * Close the database when the singleton is being destroyed.
     * Hilt will call this when the application is terminated.
     */
    fun close() {
        try {
            database?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing FTS database", e)
        } finally {
            database = null
        }
    }
}

class FtsException(message: String, cause: Throwable? = null) : Exception(message, cause)
class FtsNotInitializedException(message: String) : Exception(message)

data class FtsSearchResult(
    val chapterIndex: Int,
    val chapterTitle: String,
    val matchedText: String,
    val bookId: Long = 0,
    val bookTitle: String = "",
    /** Offset of the match within [matchedText], or -1 when it could not be located. */
    val matchStart: Int = -1,
    /**
     * How many characters of [matchedText] the match covers, or 0 when [matchStart] is -1.
     *
     * Not derivable from the query: the highlight covers the *folded* query, which is shorter
     * whenever the query carried punctuation or spaces.
     */
    val matchLength: Int = 0
)
