package com.flowreader.app.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.flowreader.app.data.local.AppDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Read-only access to reading data for other apps (v56). Exposes:
 *
 * - `content://com.flowreader.app.provider/books` — book id/title/author/format/progress
 * - `content://com.flowreader.app.provider/books/<id>` — one book
 * - `content://com.flowreader.app.provider/progress/<bookId>` — reading position
 *
 * All reads are metadata-only (no file paths, no chapter content). Writes are rejected; the
 * authority is exported so third-party launchers/widgets can build on the data.
 *
 * Hilt does not support `@AndroidEntryPoint` on ContentProviders, so the database arrives via
 * an [EntryPoint] lookup instead of field injection.
 */
class FlowReaderContentProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProviderEntryPoint {
        fun database(): AppDatabase
    }

    private val database: AppDatabase by lazy {
        EntryPointAccessors.fromApplication(
            context!!.applicationContext,
            ProviderEntryPoint::class.java
        ).database()
    }

    private val bookDao get() = database.bookDao()

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return when (MATCHER.match(uri)) {
            MATCH_BOOKS -> {
                val books = runBlocking { bookDao.getAllBooks().first() }
                matrixCursor(
                    COLUMNS,
                    books.map { book ->
                        arrayOf(
                            book.id,
                            book.title,
                            book.author,
                            book.format,
                            book.totalChapters,
                            book.readingProgress,
                            book.lastReadTime,
                            book.tags
                        )
                    }
                )
            }
            MATCH_BOOK_BY_ID -> {
                val bookId = uri.lastPathSegment?.toLongOrNull() ?: return null
                val book = runBlocking { bookDao.getBookById(bookId) } ?: return null
                matrixCursor(
                    COLUMNS,
                    listOf(
                        arrayOf(
                            book.id,
                            book.title,
                            book.author,
                            book.format,
                            book.totalChapters,
                            book.readingProgress,
                            book.lastReadTime,
                            book.tags
                        )
                    )
                )
            }
            MATCH_PROGRESS_BY_BOOK -> {
                val bookId = uri.lastPathSegment?.toLongOrNull() ?: return null
                val book = runBlocking { bookDao.getBookById(bookId) } ?: return null
                matrixCursor(
                    PROGRESS_COLUMNS,
                    listOf(
                        arrayOf(
                            book.id,
                            book.currentChapter,
                            book.currentPosition,
                            book.readingProgress,
                            book.lastReadTime
                        )
                    )
                )
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? = when (MATCHER.match(uri)) {
        MATCH_BOOKS -> "vnd.android.cursor.dir/$AUTHORITY/books"
        MATCH_BOOK_BY_ID, MATCH_PROGRESS_BY_BOOK -> "vnd.android.cursor.item/$AUTHORITY/item"
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private fun matrixCursor(columns: Array<String>, rows: List<Array<Any?>>): Cursor {
        val cursor = MatrixCursor(columns)
        rows.forEach { row -> cursor.addRow(row) }
        return cursor
    }

    companion object {
        const val AUTHORITY = "com.flowreader.app.provider"
        const val PATH_BOOKS = "books"
        const val PATH_PROGRESS = "progress"

        private const val MATCH_BOOKS = 1
        private const val MATCH_BOOK_BY_ID = 2
        private const val MATCH_PROGRESS_BY_BOOK = 3

        val COLUMNS = arrayOf(
            "book_id",
            "title",
            "author",
            "format",
            "total_chapters",
            "progress",
            "last_read_time",
            "tags"
        )

        val PROGRESS_COLUMNS = arrayOf(
            "book_id",
            "current_chapter",
            "current_position",
            "progress",
            "last_read_time"
        )

        private val MATCHER by lazy {
            UriMatcher(UriMatcher.NO_MATCH).apply {
                addURI(AUTHORITY, PATH_BOOKS, MATCH_BOOKS)
                addURI(AUTHORITY, "$PATH_BOOKS/#", MATCH_BOOK_BY_ID)
                addURI(AUTHORITY, "$PATH_PROGRESS/#", MATCH_PROGRESS_BY_BOOK)
            }
        }

        /** Pure path routing, mirrors the UriMatcher table and is JVM-testable. */
        fun matchPath(path: String): Int = when {
            path == PATH_BOOKS -> MATCH_BOOKS
            path.matches(Regex("$PATH_BOOKS/\\d+")) -> MATCH_BOOK_BY_ID
            path.matches(Regex("$PATH_PROGRESS/\\d+")) -> MATCH_PROGRESS_BY_BOOK
            else -> UriMatcher.NO_MATCH
        }
    }
}
