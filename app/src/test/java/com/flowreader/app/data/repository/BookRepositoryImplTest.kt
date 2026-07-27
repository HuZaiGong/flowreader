package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.BookDao
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BookRepositoryImplTest {

    private val dao = mockk<BookDao>(relaxed = true)
    private val repository = BookRepositoryImpl(dao)

    @Test
    fun batchDeleteDropsInvalidIdsAndDuplicates() = runTest {
        repository.deleteBooksByIds(listOf(1L, 0L, -5L, 1L, 2L))
        coVerify(exactly = 1) { dao.deleteBooksByIds(listOf(1L, 2L)) }
    }

    @Test
    fun anEmptySelectionNeverReachesTheDao() = runTest {
        repository.deleteBooksByIds(emptyList())
        repository.moveBooksToCategory(listOf(0L), 3L)
        repository.updateBooksMetadata(emptyList(), "作者", listOf("技术"))

        coVerify(exactly = 0) { dao.deleteBooksByIds(any()) }
        coVerify(exactly = 0) { dao.updateCategoryForBooks(any(), any()) }
        coVerify(exactly = 0) { dao.updateAuthorForBooks(any(), any()) }
        coVerify(exactly = 0) { dao.updateTagsForBooks(any(), any()) }
    }

    @Test
    fun movingToNoCategoryIsAllowed() = runTest {
        repository.moveBooksToCategory(listOf(1L, 2L), null)
        coVerify { dao.updateCategoryForBooks(listOf(1L, 2L), null) }
    }

    @Test
    fun aNullFieldLeavesThatColumnAlone() = runTest {
        repository.updateBooksMetadata(listOf(1L), author = "米哈里", tags = null)

        coVerify(exactly = 1) { dao.updateAuthorForBooks(listOf(1L), "米哈里") }
        coVerify(exactly = 0) { dao.updateTagsForBooks(any(), any()) }
    }

    @Test
    fun aBlankAuthorDoesNotWipeExistingAuthors() = runTest {
        repository.updateBooksMetadata(listOf(1L), author = "   ", tags = listOf("技术"))

        coVerify(exactly = 0) { dao.updateAuthorForBooks(any(), any()) }
        coVerify(exactly = 1) { dao.updateTagsForBooks(listOf(1L), "技术") }
    }

    @Test
    fun tagsAreNormalisedIntoTheStoredCommaFormat() = runTest {
        repository.updateBooksMetadata(listOf(1L), author = null, tags = listOf("  技术 ", "", "在读"))
        coVerify { dao.updateTagsForBooks(listOf(1L), "技术,在读") }
    }

    @Test
    fun anEmptyTagListClearsTheColumn() = runTest {
        // Distinct from `null`: the user explicitly emptied the field.
        repository.updateBooksMetadata(listOf(1L), author = null, tags = emptyList())
        coVerify { dao.updateTagsForBooks(listOf(1L), "") }
    }
}
