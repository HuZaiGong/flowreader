package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.ReadingListDao
import com.flowreader.app.data.local.entity.BookEntity
import com.flowreader.app.data.local.entity.ReadingListBookRow
import com.flowreader.app.data.local.entity.ReadingListEntity
import com.flowreader.app.data.local.entity.ReadingListWithCount
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingListRepositoryImplTest {

    private val dao = mockk<ReadingListDao>(relaxed = true)
    private val repository = ReadingListRepositoryImpl(dao)

    @Test
    fun listsCarryTheirBookCount() = runTest {
        every { dao.getAllLists() } returns flowOf(
            listOf(ReadingListWithCount(ReadingListEntity(id = 1, name = "2026 必读"), bookCount = 3))
        )

        val lists = repository.getAllLists().first()

        assertEquals(1, lists.size)
        assertEquals("2026 必读", lists[0].name)
        assertEquals(3, lists[0].bookCount)
    }

    @Test
    fun booksInAListKeepTheirStoredPosition() = runTest {
        every { dao.getBooksInList(1L) } returns flowOf(
            listOf(
                ReadingListBookRow(bookEntity(10, "A"), entryId = 100, entryPosition = 0),
                ReadingListBookRow(bookEntity(20, "B"), entryId = 101, entryPosition = 1)
            )
        )

        val books = repository.getBooksInList(1L).first()

        assertEquals(listOf(0, 1), books.map { it.position })
        assertEquals(listOf("A", "B"), books.map { it.book.title })
        assertEquals(listOf(100L, 101L), books.map { it.entryId })
    }

    @Test
    fun createTrimsTheNameAndRejectsBlankOnes() = runTest {
        val captured = slot<ReadingListEntity>()
        coEvery { dao.insertList(capture(captured)) } returns 5L

        assertEquals(5L, repository.createList("  2026 必读  ", "  按季度  "))
        assertEquals("2026 必读", captured.captured.name)
        assertEquals("按季度", captured.captured.description)

        val blank = runCatching { repository.createList("   ") }
        assertTrue(blank.isFailure)
    }

    @Test
    fun invalidIdsAreRefusedBeforeTheyReachTheDao() = runTest {
        assertNull(repository.getListById(0L))
        repository.deleteList(0L)
        repository.renameList(0L, "x", "")
        repository.renameList(3L, "  ", "")
        repository.addBooks(0L, listOf(1L))
        repository.removeBook(1L, 0L)
        repository.reorder(1L, emptyList())

        coVerify(exactly = 0) { dao.getListById(any()) }
        coVerify(exactly = 0) { dao.deleteList(any()) }
        coVerify(exactly = 0) { dao.renameList(any(), any(), any(), any()) }
        coVerify(exactly = 0) { dao.appendBooks(any(), any(), any()) }
        coVerify(exactly = 0) { dao.removeBook(any(), any(), any()) }
        coVerify(exactly = 0) { dao.applyOrder(any(), any(), any()) }
    }

    @Test
    fun addingFiltersOutInvalidBookIds() = runTest {
        repository.addBooks(1L, listOf(10L, 0L, -3L, 20L))
        coVerify { dao.appendBooks(1L, listOf(10L, 20L), any()) }
    }

    @Test
    fun reorderDedupesBeforeWriting() = runTest {
        repository.reorder(1L, listOf(10L, 20L, 10L))
        coVerify { dao.applyOrder(1L, listOf(10L, 20L), any()) }
    }

    private fun bookEntity(id: Long, title: String) = BookEntity(
        id = id,
        title = title,
        author = "作者",
        filePath = "/books/$id"
    )
}
