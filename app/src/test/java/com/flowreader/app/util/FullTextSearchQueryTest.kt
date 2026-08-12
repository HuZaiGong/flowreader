package com.flowreader.app.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression gate for the in-book search bug: `FullTextSearch.search()` bound `book_id` as TEXT
 * against an FTS5 external-content column, which holds INTEGER, so it always matched nothing.
 *
 * Robolectric's bundled SQLite has no `fts5` module, so the production query cannot be executed
 * here. These tests cover the two halves of the bug instead:
 *  - [inBookSearchScopesBookIdThroughAnIntegerCast] pins the shape of the shipped SQL.
 *  - [textArgumentNeverMatchesAnIntegerOnAnAffinitylessColumn] proves the SQLite semantics that
 *    make the cast necessary, on a plain table declared without types — the same "no column
 *    affinity" situation an FTS5 table's columns are in.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FullTextSearchQueryTest {

    private lateinit var db: android.database.sqlite.SQLiteDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = context.openOrCreateDatabase("affinity_probe.db", Context.MODE_PRIVATE, null)
        db.execSQL("DROP TABLE IF EXISTS shadow")
        // No declared types: exactly like an FTS5 table's columns, which have no affinity.
        db.execSQL("CREATE TABLE shadow(book_id, chapter_index, content)")
        db.execSQL("INSERT INTO shadow VALUES (7, 0, 'alpha')")
        db.execSQL("INSERT INTO shadow VALUES (8, 0, 'beta')")
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun inBookSearchScopesBookIdThroughAnIntegerCast() {
        val sql = FullTextSearch.SEARCH_IN_BOOK_SQL
        assertTrue(
            "in-book search must cast the bound book id: $sql",
            sql.contains("book_id = CAST(? AS INTEGER)")
        )
        assertFalse(
            "a bare `book_id = ?` compares INTEGER to TEXT and matches nothing: $sql",
            sql.contains(Regex("""book_id\s*=\s*\?"""))
        )
        assertTrue(sql.contains("book_content_fts MATCH ?"))
    }

    @Test
    fun globalSearchIsNotBookScoped() {
        val sql = FullTextSearch.SEARCH_ALL_SQL
        assertFalse("global search must not filter by book: $sql", sql.contains("book_id ="))
        assertTrue(sql.contains("book_content_fts MATCH ?"))
    }

    @Test
    fun textArgumentNeverMatchesAnIntegerOnAnAffinitylessColumn() {
        assertEquals("integer", typeOfStoredBookId())
        assertEquals(0, countWhere("book_id = ?", "7"))
    }

    @Test
    fun castedTextArgumentMatchesTheStoredInteger() {
        assertEquals(1, countWhere("book_id = CAST(? AS INTEGER)", "7"))
    }

    private fun typeOfStoredBookId(): String =
        db.rawQuery("SELECT typeof(book_id) FROM shadow LIMIT 1", null).use {
            it.moveToFirst()
            it.getString(0)
        }

    private fun countWhere(predicate: String, arg: String): Int =
        db.rawQuery("SELECT COUNT(*) FROM shadow WHERE $predicate", arrayOf(arg)).use {
            it.moveToFirst()
            it.getInt(0)
        }
}
