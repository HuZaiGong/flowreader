package com.flowreader.app.util

import com.flowreader.app.core.util.CjkTokenizer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * End-to-end tests for the search index, executed against a **real** FTS5 build via sqlite-jdbc.
 *
 * Robolectric's bundled SQLite has no `fts5` module, which is why [FullTextSearchQueryTest] can only
 * assert the shape of the shipped SQL. Shape assertions cannot catch the bug class that actually
 * broke Chinese search: the schema was valid and the SQL was well-formed, but `unicode61` indexed
 * each run of Han characters as a single token, so 「心流」 could never match inside
 * 「关于心流阅读的故事」. That defect survived a functional bug sweep precisely because no test ever
 * ran a query.
 *
 * These tests build the production schema, index text through [CjkTokenizer], and run
 * [FullTextSearch.SEARCH_IN_BOOK_SQL] / [FullTextSearch.SEARCH_ALL_SQL] verbatim. The queries are
 * the same strings the app ships; only the driver differs. Argument binding mirrors
 * `FullTextSearch.search()`, including the `String`-only binding that makes `CAST(? AS INTEGER)`
 * necessary.
 */
class FullTextSearchEngineTest {

    private lateinit var connection: Connection

    private companion object {
        const val BOOK_A = 5L
        const val BOOK_B = 6L

        /**
         * Chapter 0 of book A. Deliberately shares only 「阅读」 with [MIXED_TEXT] so every other
         * assertion can expect an exact row count of 1 — an overlapping term turns a count
         * assertion into a silent tautology.
         */
        const val CHAPTER_TEXT = "第一章：这是一个关于心流阅读的故事，江南的春天格外温柔。"

        /** Chapter 1 of book A: exercises Han→Latin and Han→digit boundaries. */
        const val MIXED_TEXT = "潮流Flow Reader 上海1949年的阅读记录"

        /** The one term present in both chapters of book A. */
        const val SHARED_TERM = "阅读"
    }

    @Before
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        createSchema()
        indexChapter(BOOK_A, 0, "第一章 风起", CHAPTER_TEXT)
        indexChapter(BOOK_A, 1, "第二章 混排", MIXED_TEXT)
        indexChapter(BOOK_B, 0, "另一本书", "完全不相关的另一本书的内容")
    }

    @After
    fun tearDown() {
        connection.close()
    }

    /** The bug that broke every Chinese search: a word in the middle of a clause. */
    @Test
    fun matchesChineseWordsInTheMiddleOfAClause() {
        // `unicode61` stored 「这是一个关于心流阅读的故事」 as one token, so each of these was 0 rows.
        for (query in listOf("心流", "故事", "春天", "格外温柔", "一个关于")) {
            assertEquals("query '$query' must match", 1, searchInBook(BOOK_A, query).size)
        }
    }

    @Test
    fun stillMatchesWordsAtTheStartOfAClause() {
        // These are the only queries the old tokenizer got right; they must not regress.
        for (query in listOf("江南", "这是", "第一章")) {
            assertTrue("query '$query' must match", searchInBook(BOOK_A, query).isNotEmpty())
        }
    }

    /** Folding before segmenting is what makes these reachable at all. */
    @Test
    fun matchesAcrossPunctuationAndScriptBoundaries() {
        assertEquals(1, searchInBook(BOOK_A, "事，江").size) // spans a comma
        assertEquals(1, searchInBook(BOOK_A, "事江").size) // same match, punctuation omitted
        assertEquals(1, searchInBook(BOOK_A, "流Flow").size) // Han -> Latin
        assertEquals(1, searchInBook(BOOK_A, "海1949").size) // Han -> digits
    }

    @Test
    fun latinSearchIsCaseInsensitiveAndMatchesMidWord() {
        assertEquals(1, searchInBook(BOOK_A, "flow").size)
        assertEquals(1, searchInBook(BOOK_A, "FLOW").size)
        assertEquals(1, searchInBook(BOOK_A, "eade").size) // inside "Reader"
    }

    /** A single code point has no bigram, so it must go through the prefix form. */
    @Test
    fun matchesASingleCharacterQuery() {
        assertEquals(1, searchInBook(BOOK_A, "心").size)
        assertEquals(1, searchInBook(BOOK_A, "春").size)
    }

    @Test
    fun findsNothingForTermsThatAreAbsent() {
        assertEquals(0, searchInBook(BOOK_A, "秋天").size)
        assertEquals(0, searchInBook(BOOK_A, "zzz").size)
    }

    /**
     * Bigrams are matched as an FTS5 *phrase*, so they must appear consecutively and in order.
     * Without that constraint, any permutation of a query's characters would match — the classic
     * bigram false-positive problem.
     */
    @Test
    fun doesNotMatchAPermutationOfTheQuery() {
        indexChapter(BOOK_B, 1, "重叠", "海上上海")
        assertEquals(1, searchInBook(BOOK_B, "海上上海").size)
        assertEquals(0, searchInBook(BOOK_B, "上海海上").size)
    }

    /** `book_id` and `chapter_index` were indexed as text, so a numeric query matched by id. */
    @Test
    fun numericQueriesDoNotMatchBookOrChapterIds() {
        // Book 5 chapter 0 exists, and 1949 is real text — only the latter may match.
        assertEquals(0, searchInBook(BOOK_A, "5").size)
        assertEquals(0, searchInBook(BOOK_A, "0").size)
        assertEquals(1, searchInBook(BOOK_A, "1949").size)
    }

    @Test
    fun inBookSearchIsScopedToTheRequestedBook() {
        assertEquals(1, searchInBook(BOOK_B, "另一本书").size)
        assertEquals(0, searchInBook(BOOK_A, "另一本书").size)
        // The cast is what makes this work at all: rawQuery binds Strings and the column has no
        // affinity, so a bare `book_id = ?` compares TEXT '5' to INTEGER 5 and matches nothing.
        assertTrue(FullTextSearch.SEARCH_IN_BOOK_SQL.contains("CAST(? AS INTEGER)"))
    }

    @Test
    fun globalSearchSpansEveryBook() {
        val results = searchAll(SHARED_TERM)
        assertEquals(2, results.size)
        assertEquals(setOf(BOOK_A), results.map { it.bookId }.toSet())
        assertEquals(setOf(0, 1), results.map { it.chapterIndex }.toSet())
    }

    @Test
    fun globalSearchPagesWithLimitAndOffset() {
        assertEquals(1, searchAll(SHARED_TERM, limit = 1, offset = 0).size)
        assertEquals(1, searchAll(SHARED_TERM, limit = 1, offset = 1).size)
        assertEquals(0, searchAll(SHARED_TERM, limit = 1, offset = 2).size)
    }

    @Test
    fun chapterTitlesAreSearchable() {
        assertEquals(1, searchInBook(BOOK_A, "风起").size)
    }

    @Test
    fun snippetShowsTheOriginalTextNotTheBigramStream() {
        val snippet = searchInBook(BOOK_A, "心流").single().matchedText
        assertTrue("snippet must contain the match: $snippet", snippet.contains("心流"))
        // The bigram stream would read "心流 流阅 阅读"; the original never repeats a character.
        assertTrue("snippet must not be segmented: $snippet", !snippet.contains("心流 流阅"))
    }

    /**
     * A chapter can be a whole 128MB TXT with no chapter markers, so the snippet is sliced inside
     * SQLite. Returning the column and trimming in Kotlin would pull all of it into memory per row.
     */
    @Test
    fun snippetIsWindowedOutOfTheMiddleOfALongChapter() {
        val filler = "无关的填充文字".repeat(60) // ~420 chars, far past the snippet window
        indexChapter(BOOK_B, 2, "长章节", filler + "独特标记" + filler)

        val snippet = searchInBook(BOOK_B, "独特标记").single().matchedText
        assertTrue("snippet must contain the match: $snippet", snippet.contains("独特标记"))
        assertTrue("snippet was not windowed (${snippet.length} chars)", snippet.length < 100)
        // Context on both sides, not a match sitting at the head of the slice.
        assertTrue("match should have leading context: $snippet", snippet.indexOf("独特标记") > 0)
    }

    @Test
    fun snippetLocatesTheHighlightOffset() {
        val result = searchInBook(BOOK_A, "心流").single()
        assertTrue("expected a highlight offset, got ${result.matchStart}", result.matchStart >= 0)
        assertEquals(2, result.matchLength)
        val end = result.matchStart + result.matchLength
        assertEquals("心流", result.matchedText.substring(result.matchStart, end))
    }

    /**
     * The highlight span covers the **folded** query, not what the user typed.
     *
     * A query carrying punctuation or spaces is longer than the run that exists in the text, so
     * highlighting `query.length` characters would spill past the match — which is why the length
     * travels with the result instead of being re-derived in the UI.
     */
    @Test
    fun theHighlightSpanCoversTheFoldedQueryNotTheTypedOne() {
        val result = searchInBook(BOOK_A, " 心 流 ").single()
        assertTrue("expected a highlight offset, got ${result.matchStart}", result.matchStart >= 0)
        assertEquals(2, result.matchLength)
        val end = result.matchStart + result.matchLength
        assertEquals("心流", result.matchedText.substring(result.matchStart, end))
    }

    /** A match that only exists in the folded stream has no literal offset — highlight is skipped. */
    @Test
    fun snippetHasNoHighlightWhenTheMatchSpannedPunctuation() {
        val result = searchInBook(BOOK_A, "事，江").single()
        assertEquals(-1, result.matchStart)
        assertEquals(0, result.matchLength)
        assertTrue("snippet must still be returned", result.matchedText.isNotEmpty())
    }

    /** Every hit's span must be a valid slice of its snippet, or the UI would throw on substring. */
    @Test
    fun everyReportedSpanIsInsideItsSnippet() {
        for (query in listOf("心流", "阅读", "1949", "flow", "事，江")) {
            for (row in searchAll(query)) {
                if (row.matchStart < 0) {
                    assertEquals("$query: no offset means no length", 0, row.matchLength)
                    continue
                }
                assertTrue(
                    "$query: span ${row.matchStart}+${row.matchLength} escapes a " +
                        "${row.matchedText.length}-char snippet",
                    row.matchStart + row.matchLength <= row.matchedText.length
                )
            }
        }
    }

    /**
     * Re-indexing must not corrupt the index. `INSERT OR REPLACE` did: SQLite skips DELETE triggers
     * for rows removed by REPLACE unless `recursive_triggers` is on, so the FTS index kept terms for
     * a rowid that no longer existed and every later query on that book raised
     * `SQLITE_CORRUPT_VTAB`. The reader re-indexes on open, so this was reachable by reopening a book.
     */
    @Test
    fun reindexingAChapterReplacesItsOldTermsWithoutCorruptingTheIndex() {
        indexChapter(BOOK_A, 0, "第一章 风起", "彻底换掉的新内容")
        assertIndexIntegrity()
        assertEquals(0, searchInBook(BOOK_A, "心流").size)
        assertEquals(1, searchInBook(BOOK_A, "新内容").size)
        // The other chapters must survive untouched.
        assertEquals(1, searchInBook(BOOK_A, "1949").size)
        assertEquals(1, searchInBook(BOOK_B, "另一本书").size)
    }

    /** Re-indexing repeatedly is what the reader actually does; rowids must not drift out of sync. */
    @Test
    fun repeatedReindexingKeepsTheIndexConsistent() {
        repeat(5) { round ->
            indexChapter(BOOK_A, 0, "第一章 风起", "第 $round 轮的春天内容")
            assertIndexIntegrity()
            assertEquals("round $round", 1, searchInBook(BOOK_A, "春天").size)
        }
    }

    @Test
    fun deletingABookRemovesItFromTheIndex() {
        execute("DELETE FROM book_content WHERE book_id = $BOOK_A")
        assertIndexIntegrity()
        assertEquals(0, searchInBook(BOOK_A, "心流").size)
        assertEquals(1, searchInBook(BOOK_B, "另一本书").size)
    }

    /** `deleteAllContent()`'s bare DELETE — the AFTER DELETE trigger is what keeps this in sync. */
    @Test
    fun clearingEveryChapterEmptiesTheIndex() {
        execute("DELETE FROM book_content")
        assertIndexIntegrity()
        assertEquals(0, searchAll("阅读").size)
        assertEquals(0, searchAll("另一本书").size)
    }

    @Test
    fun queriesThatFoldAwayAreTreatedAsNoResults() {
        // matchExpression returns null, so the app never reaches SQL — matching all rows on an
        // empty query would otherwise dump the whole library.
        for (query in listOf("", "   ", "\"*^-+~(){}[]:")) {
            assertEquals(null, CjkTokenizer.matchExpression(query))
        }
    }

    // ---- completion markers --------------------------------------------------------------------

    /**
     * The marker table is what lets maintenance index only the difference. A book with rows but no
     * marker must read as *not* indexed: that is how an interrupted run resumes instead of being
     * mistaken for a finished one.
     */
    @Test
    fun aPartiallyIndexedBookIsNotReportedAsIndexed() {
        // setUp() indexed content for both books but marked neither.
        assertEquals(emptySet<Long>(), indexedBookIds())
        assertTrue(searchInBook(BOOK_A, "心流").isNotEmpty())
    }

    @Test
    fun markingABookRecordsItExactlyOnce() {
        markBookIndexed(BOOK_A, 2)
        markBookIndexed(BOOK_B, 1)
        assertEquals(setOf(BOOK_A, BOOK_B), indexedBookIds())

        // Re-indexing marks again; PRIMARY KEY + REPLACE must update, not duplicate or fail.
        markBookIndexed(BOOK_A, 3)
        assertEquals(setOf(BOOK_A, BOOK_B), indexedBookIds())
        assertEquals(3, chapterCountFor(BOOK_A))
    }

    /** Dropping a book's content must drop its marker, or it would never be re-indexed. */
    @Test
    fun deletingABookAlsoClearsItsMarker() {
        markBookIndexed(BOOK_A, 2)
        markBookIndexed(BOOK_B, 1)

        execute("DELETE FROM book_content WHERE book_id = $BOOK_A")
        execute("DELETE FROM indexed_books WHERE book_id = $BOOK_A")

        assertEquals(setOf(BOOK_B), indexedBookIds())
    }

    @Test
    fun clearingEverythingClearsEveryMarker() {
        markBookIndexed(BOOK_A, 2)
        execute("DELETE FROM book_content")
        execute("DELETE FROM indexed_books")
        assertEquals(emptySet<Long>(), indexedBookIds())
    }

    // ---- harness -------------------------------------------------------------------------------
    // Everything below executes the constants FullTextSearch ships — same DDL, same SQL, same
    // segmentation, same String-only binding. Nothing about the schema is restated here.

    private data class Row(
        val bookId: Long,
        val chapterIndex: Int,
        val chapterTitle: String,
        val matchedText: String,
        val matchStart: Int,
        /** Mirrors `FtsSearchResult.matchLength`: the folded needle's length, or 0 with no hit. */
        val matchLength: Int
    )

    private fun createSchema() {
        FullTextSearch.SCHEMA_SQL.forEach(::execute)
    }

    private fun indexChapter(bookId: Long, chapterIndex: Int, title: String, content: String) {
        connection.prepareStatement(FullTextSearch.DELETE_CHAPTER_SQL).use {
            it.setString(1, bookId.toString())
            it.setString(2, chapterIndex.toString())
            it.executeUpdate()
        }
        connection.prepareStatement(FullTextSearch.INSERT_CHAPTER_SQL).use {
            it.setString(1, bookId.toString())
            it.setString(2, chapterIndex.toString())
            it.setString(3, title)
            it.setString(4, content)
            it.setString(5, CjkTokenizer.indexStream(title))
            it.setString(6, CjkTokenizer.indexStream(content))
            it.executeUpdate()
        }
    }

    /**
     * Asks FTS5 to verify that the index agrees with its content table. An external-content table
     * cannot detect the mismatch on its own — it simply returns wrong results or raises
     * `SQLITE_CORRUPT_VTAB` on a later, unrelated query — so mutation tests assert this directly.
     */
    private fun assertIndexIntegrity() {
        execute("INSERT INTO book_content_fts(book_content_fts) VALUES('integrity-check')")
    }

    /** Mirrors `FullTextSearch.markBookIndexed()`, including its String-only binding. */
    private fun markBookIndexed(bookId: Long, chapterCount: Int) {
        connection.prepareStatement(FullTextSearch.MARK_INDEXED_SQL).use {
            it.setString(1, bookId.toString())
            it.setString(2, chapterCount.toString())
            it.executeUpdate()
        }
    }

    /** Mirrors `FullTextSearch.indexedBookIds()`. */
    private fun indexedBookIds(): Set<Long> =
        connection.prepareStatement(FullTextSearch.SELECT_INDEXED_BOOKS_SQL).use { st ->
            st.executeQuery().use { rs ->
                buildSet {
                    while (rs.next()) add(rs.getLong(1))
                }
            }
        }

    private fun chapterCountFor(bookId: Long): Int =
        connection.prepareStatement("SELECT chapter_count FROM indexed_books WHERE book_id = ?").use { st ->
            st.setString(1, bookId.toString())
            st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else -1 }
        }

    private fun searchInBook(bookId: Long, query: String, limit: Int = 50): List<Row> {
        val match = CjkTokenizer.matchExpression(query) ?: return emptyList()
        val needle = CjkTokenizer.fold(query)
        return connection.prepareStatement(FullTextSearch.SEARCH_IN_BOOK_SQL).use { st ->
            st.setString(1, needle)
            st.setString(2, needle)
            st.setString(3, bookId.toString())
            st.setString(4, match)
            st.setString(5, limit.toString())
            st.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        val snippet = rs.getString(3).orEmpty()
                        val start = snippet.indexOf(needle, ignoreCase = true)
                        add(
                            Row(
                                bookId = bookId,
                                chapterIndex = rs.getInt(1),
                                chapterTitle = rs.getString(2).orEmpty(),
                                matchedText = snippet,
                                matchStart = start,
                                matchLength = if (start < 0) 0 else needle.length
                            )
                        )
                    }
                }
            }
        }
    }

    private fun searchAll(query: String, limit: Int = 100, offset: Int = 0): List<Row> {
        val match = CjkTokenizer.matchExpression(query) ?: return emptyList()
        val needle = CjkTokenizer.fold(query)
        return connection.prepareStatement(FullTextSearch.SEARCH_ALL_SQL).use { st ->
            st.setString(1, needle)
            st.setString(2, needle)
            st.setString(3, match)
            st.setString(4, limit.toString())
            st.setString(5, offset.toString())
            st.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        val snippet = rs.getString(4).orEmpty()
                        val start = snippet.indexOf(needle, ignoreCase = true)
                        add(
                            Row(
                                bookId = rs.getLong(1),
                                chapterIndex = rs.getInt(2),
                                chapterTitle = rs.getString(3).orEmpty(),
                                matchedText = snippet,
                                matchStart = start,
                                matchLength = if (start < 0) 0 else needle.length
                            )
                        )
                    }
                }
            }
        }
    }

    private fun execute(sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }

    /** Guards the premise of this whole file: the driver really does have fts5 compiled in. */
    @Test
    fun sqliteDriverProvidesFts5() {
        connection.createStatement().use { st ->
            st.executeQuery("SELECT sqlite_version()").use {
                it.next()
                assertNotNull(it.getString(1))
            }
        }
        execute("CREATE VIRTUAL TABLE fts5_probe USING fts5(x)")
    }
}
