package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchIndexProgressTest {

    @Test
    fun theIdleValueReportsNoWork() {
        val idle = SearchIndexProgress.Idle

        assertFalse(idle.isIndexing)
        assertFalse(idle.isIncomplete)
        assertEquals(0, idle.booksIndexed)
        assertEquals(0, idle.booksTotal)
    }

    /** The search screen keys its 「索引尚未完成」 note off this, so it must not lag a book behind. */
    @Test
    fun aRunWithBooksLeftIsIncomplete() {
        val progress = SearchIndexProgress(isIndexing = true, booksIndexed = 2, booksTotal = 5)

        assertTrue(progress.isIncomplete)
    }

    @Test
    fun theLastBookOfARunIsNoLongerIncomplete() {
        val progress = SearchIndexProgress(isIndexing = true, booksIndexed = 5, booksTotal = 5)

        assertTrue(progress.isIndexing)
        assertFalse(progress.isIncomplete)
    }

    /**
     * A finished run resets to [SearchIndexProgress.Idle], so leftover counts must never look like
     * work in progress — the banner would sit on screen forever.
     */
    @Test
    fun countsWithoutAnActiveRunAreNotIncomplete() {
        val progress = SearchIndexProgress(isIndexing = false, booksIndexed = 1, booksTotal = 5)

        assertFalse(progress.isIncomplete)
    }

    @Test
    fun equalCountsCompareEqualSoStateFlowConflatesRepeats() {
        val first = SearchIndexProgress(isIndexing = true, booksIndexed = 1, booksTotal = 3)
        val second = SearchIndexProgress(isIndexing = true, booksIndexed = 1, booksTotal = 3)

        assertEquals(first, second)
        assertEquals(SearchIndexProgress.Idle, SearchIndexProgress())
    }
}
