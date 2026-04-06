package com.flowreader.app.util

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FullTextSearch @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dbName = "flowreader_fts.db"
    private var database: SQLiteDatabase? = null

    suspend fun initialize() = withContext(Dispatchers.IO) {
        database = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        
        database?.execSQL("""
            CREATE TABLE IF NOT EXISTS book_content(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id INTEGER NOT NULL,
                chapter_index INTEGER NOT NULL,
                chapter_title TEXT,
                content TEXT,
                UNIQUE(book_id, chapter_index)
            )
        """.trimIndent())
        
        database?.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS book_content_fts USING fts5(
                book_id,
                chapter_index,
                chapter_title,
                content,
                content='book_content',
                content_rowid='id'
            )
        """.trimIndent())
        
        database?.execSQL("""
            CREATE TRIGGER IF NOT EXISTS book_content_ai AFTER INSERT ON book_content BEGIN
                INSERT INTO book_content_fts(rowid, book_id, chapter_index, chapter_title, content)
                VALUES (new.id, new.book_id, new.chapter_index, new.chapter_title, new.content);
            END
        """.trimIndent())
        
        database?.execSQL("""
            CREATE TRIGGER IF NOT EXISTS book_content_ad AFTER DELETE ON book_content BEGIN
                INSERT INTO book_content_fts(book_content_fts, rowid, book_id, chapter_index, chapter_title, content)
                VALUES ('delete', old.id, old.book_id, old.chapter_index, old.chapter_title, old.content);
            END
        """.trimIndent())
        
        database?.execSQL("""
            CREATE TRIGGER IF NOT EXISTS book_content_au AFTER UPDATE ON book_content BEGIN
                INSERT INTO book_content_fts(book_content_fts, rowid, book_id, chapter_index, chapter_title, content)
                VALUES ('delete', old.id, old.book_id, old.chapter_index, old.chapter_title, old.content);
                INSERT INTO book_content_fts(rowid, book_id, chapter_index, chapter_title, content)
                VALUES (new.id, new.book_id, new.chapter_index, new.chapter_title, new.content);
            END
        """.trimIndent())
    }

    suspend fun indexChapter(
        bookId: Long,
        chapterIndex: Int,
        chapterTitle: String,
        content: String
    ) = withContext(Dispatchers.IO) {
        database?.execSQL(
            "INSERT OR REPLACE INTO book_content (book_id, chapter_index, chapter_title, content) VALUES (?, ?, ?, ?)",
            arrayOf(bookId, chapterIndex, chapterTitle, content)
        )
    }

    suspend fun search(
        bookId: Long,
        query: String,
        maxResults: Int = 50
    ): List<FtsSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FtsSearchResult>()
        
        val queryArgs = arrayOf(bookId.toString(), "$query*", maxResults.toString())
        val cursor = database?.rawQuery(
            """
            SELECT chapter_index, chapter_title, snippet(book_content_fts, 3, '<<', '>>', '...', 30) as matched_text
            FROM book_content_fts
            WHERE book_id = ? AND book_content_fts MATCH ?
            ORDER BY rank
            LIMIT ?
            """.trimIndent(),
            queryArgs
        )
        
        cursor?.use {
            while (it.moveToNext()) {
                results.add(
                    FtsSearchResult(
                        chapterIndex = it.getInt(0),
                        chapterTitle = it.getString(1),
                        matchedText = it.getString(2)
                    )
                )
            }
        }
        
        results
    }

    suspend fun deleteBookContent(bookId: Long) = withContext(Dispatchers.IO) {
        database?.execSQL("DELETE FROM book_content WHERE book_id = ?", arrayOf(bookId.toString()))
    }

    fun close() {
        database?.close()
        database = null
    }
}

data class FtsSearchResult(
    val chapterIndex: Int,
    val chapterTitle: String,
    val matchedText: String
)
