package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.BookmarkDao
import com.flowreader.app.data.local.entity.BookmarkEntity
import com.flowreader.app.domain.model.Bookmark
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BookmarkRepositoryImplTest {
    private val dao = mockk<BookmarkDao>(relaxed = true)
    private val repository = BookmarkRepositoryImpl(dao)

    @Test
    fun addBookmark_normalizesBlankTextAndNegativePosition() = runTest {
        coEvery { dao.insertBookmark(any()) } returns 7L

        val saved = repository.addBookmark(Bookmark(bookId = 1L, chapterIndex = -1, position = -20, text = "   "))

        assertEquals(7L, saved.id)
        assertEquals(0, saved.chapterIndex)
        assertEquals(0, saved.position)
        assertEquals("书签", saved.text)
        coVerify { dao.insertBookmark(any<BookmarkEntity>()) }
    }

    @Test
    fun getBookmarks_rejectsInvalidBookId() {
        assertThrows(IllegalArgumentException::class.java) {
            repository.getBookmarksByBookId(0L)
        }
    }
}
