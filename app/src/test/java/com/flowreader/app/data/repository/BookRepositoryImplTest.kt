package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.util.FullTextSearch
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BookRepositoryImplTest {

    private val dao = mockk<BookDao>(relaxed = true)
    private val fullTextSearch = mockk<FullTextSearch>(relaxed = true)
    private val repository = BookRepositoryImpl(dao, fullTextSearch)

    @Test
    fun batchDeleteDropsInvalidIdsAndDuplicates() = runTest {
        repository.deleteBooksByIds(listOf(1L, 0L, -5L, 1L, 2L))
        coVerify(exactly = 1) { dao.deleteBooksByIds(listOf(1L, 2L)) }
    }

    /**
     * Deleting a book has to take its text out of the index too, or global search keeps returning
     * hits that open a book that no longer exists.
     */
    @Test
    fun deletingBooksAlsoRemovesThemFromTheSearchIndex() = runTest {
        repository.deleteBooksByIds(listOf(1L, 2L))
        coVerify(exactly = 1) { fullTextSearch.deleteBooks(listOf(1L, 2L)) }

        repository.deleteBookById(7L)
        coVerify(exactly = 1) { fullTextSearch.deleteBooks(listOf(7L)) }
    }

    /** A failure cleaning the index must not block the delete the user asked for. */
    @Test
    fun aFailingIndexCleanupStillDeletesTheBook() = runTest {
        coEvery { fullTextSearch.deleteBooks(any()) } throws IllegalStateException("index closed")

        repository.deleteBookById(9L)

        coVerify(exactly = 1) { dao.deleteBookById(9L) }
    }

    /**
     * `%` and `_` are LIKE wildcards in the *bound argument* too, so searching 「%」 used to return
     * the whole library and 「a_c」 matched "abc". The DAO declares `ESCAPE '\'`; the escaping itself
     * has to happen here.
     */
    @Test
    fun likeWildcardsInAQueryAreEscaped() = runTest {
        every { dao.searchBooks(any()) } returns flowOf(emptyList())

        repository.searchBooks("50%").first()
        repository.searchBooks("a_c").first()
        repository.searchBooks("""C:\dir""").first()

        verify(exactly = 1) { dao.searchBooks("""50\%""") }
        verify(exactly = 1) { dao.searchBooks("""a\_c""") }
        verify(exactly = 1) { dao.searchBooks("""C:\\dir""") }
    }

    @Test
    fun anOrdinaryQueryPassesThroughUnchanged() = runTest {
        every { dao.searchBooks(any()) } returns flowOf(emptyList())

        repository.searchBooks("心流").first()

        verify(exactly = 1) { dao.searchBooks("心流") }
    }

    @Test
    fun tagLookupsAreEscapedToo() = runTest {
        every { dao.getBooksByTag(any()) } returns flowOf(emptyList())

        repository.getBooksByTag("100%").first()

        verify(exactly = 1) { dao.getBooksByTag("""100\%""") }
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
